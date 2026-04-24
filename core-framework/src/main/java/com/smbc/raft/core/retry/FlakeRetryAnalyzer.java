package com.smbc.raft.core.retry;

import lombok.extern.log4j.Log4j2;
import org.testng.IRetryAnalyzer;
import org.testng.ITestResult;

import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.sql.SQLTransientConnectionException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * TestNG IRetryAnalyzer that retries test methods failing due to known
 * transient infrastructure exceptions — not AssertionErrors.
 *
 * <p>Only the exception types listed in RETRYABLE_CAUSES are eligible.
 * AssertionError, NullPointerException, and all other deterministic failures
 * pass straight through with no retry so genuine bugs are never masked.
 *
 * <p>The retry count is configurable via the system property
 * {@code retry.max.attempts} (default 1). Set to 0 to disable entirely.
 *
 * Usage — applied at class level so all methods in the class inherit it:
 * <pre>
 *   {@literal @}Test(retryAnalyzer = FlakeRetryAnalyzer.class)
 *   public void testSomething() { ... }
 * </pre>
 * Or register globally in BaseTest so every test inherits it automatically.
 */
@Log4j2
public class FlakeRetryAnalyzer implements IRetryAnalyzer {

    private static final int MAX_ATTEMPTS = Integer.parseInt(
            System.getProperty("retry.max.attempts", "1"));

    /**
     * Exception types whose presence in the cause chain justifies a retry.
     * Add to this set when new infrastructure failure modes are identified —
     * never add application exception types here.
     */
    private static final Set<Class<? extends Throwable>> RETRYABLE_CAUSES = Set.of(
            SocketTimeoutException.class,
            SocketException.class,
            UnknownHostException.class,
            SQLTransientConnectionException.class,
            java.util.concurrent.TimeoutException.class,
            com.microsoft.playwright.PlaywrightException.class
    );

    /**
     * Per-test attempt counter. Keyed by "ClassName#methodName" so parallel
     * threads each maintain their own count without contention.
     */
    private static final ConcurrentMap<String, Integer> ATTEMPT_COUNTS =
            new ConcurrentHashMap<>();

    @Override
    public boolean retry(ITestResult result) {
        if (MAX_ATTEMPTS == 0) {
            return false;
        }

        Throwable cause = result.getThrowable();

        // Never retry genuine assertion failures — the app is broken
        if (cause instanceof AssertionError) {
            return false;
        }

        // Only retry if the failure is caused by a known transient exception
        if (!isRetryable(cause)) {
            log.debug("Not retrying {} — exception type {} is not in RETRYABLE_CAUSES",
                    key(result), cause == null ? "null" : cause.getClass().getSimpleName());
            return false;
        }

        String key = key(result);
        Integer attemptsObj = ATTEMPT_COUNTS.merge(key, 1, (oldV, newV) -> oldV + newV);
        int attempts = attemptsObj != null ? attemptsObj : 1;

        if (attempts <= MAX_ATTEMPTS) {
            log.warn("Retrying [{}/{}] {} — transient failure: {}",
                    attempts, MAX_ATTEMPTS, key, cause.getMessage());
            return true;
        }

        // Exhausted retries — clean up counter and let the failure propagate
        ATTEMPT_COUNTS.remove(key);
        log.warn("Giving up on {} after {} attempt(s). Final exception: {}",
                key, attempts, cause.getMessage());
        return false;
    }

    /**
     * Walk the full cause chain to detect retryable exceptions, including
     * when they are wrapped in a RuntimeException or framework exception.
     */
    private static boolean isRetryable(Throwable t) {
        Throwable current = t;
        while (current != null) {
            for (Class<? extends Throwable> retryable : RETRYABLE_CAUSES) {
                if (retryable.isInstance(current)) {
                    return true;
                }
            }
            current = current.getCause();
        }
        return false;
    }

    private static String key(ITestResult result) {
        return result.getTestClass().getName() + "#" + result.getMethod().getMethodName();
    }
}
