package com.smbc.raft.core.retry;

import com.smbc.raft.core.reporting.ExtentReportManager;
import java.util.Objects;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.function.Supplier;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;

/**
 * High-level polling/validation primitive built on top of {@link LoopConfig}.
 *
 * <p>Re-invokes an action until it satisfies a predicate, up to a configured number of iterations.
 * Returns a {@link ValidationResult} summarising the outcome. Designed for UI, API, and database
 * scenarios where the expected state takes time to propagate.
 */
@Log4j2
@Getter
public class ValidationLoop {

  private final LoopConfig config;

  public ValidationLoop(LoopConfig config) {
    this.config = Objects.requireNonNull(config, "config");
  }

  public static ValidationLoop once() {
    return new ValidationLoop(LoopConfig.once());
  }

  public static ValidationLoop shortLoop() {
    return new ValidationLoop(LoopConfig.shortLoop());
  }

  public static ValidationLoop longLoop() {
    return new ValidationLoop(LoopConfig.longLoop());
  }

  public static ValidationLoop custom(int iterations, long intervalMillis) {
    return new ValidationLoop(LoopConfig.custom(iterations, intervalMillis));
  }

  /**
   * Poll {@code actualSupplier} until the {@code matcher} returns true when applied to the supplied
   * value and {@code expected}.
   */
  public <A, E> ValidationResult<A> validate(
      Supplier<A> actualSupplier, BiPredicate<A, E> matcher, E expected) {
    Objects.requireNonNull(actualSupplier, "actualSupplier");
    Objects.requireNonNull(matcher, "matcher");

    long start = System.currentTimeMillis();
    A actual = null;
    boolean success = false;
    int iteration = 0;

    for (int i = 0; i < config.getIterations(); i++) {
      iteration = i + 1;
      try {
        actual = actualSupplier.get();
        success = matcher.test(actual, expected);
        if (success) {
          break;
        }
      } catch (Exception e) {
        log.warn("Iteration {}/{} threw: {}", iteration, config.getIterations(), e.getMessage());
      }

      if (i < config.getIterations() - 1) {
        sleepInterval();
      }
    }

    long elapsed = System.currentTimeMillis() - start;
    ValidationResult<A> result =
        new ValidationResult<>(
            null, actual, success, iteration, config.getIterations(), elapsed, "ValidationLoop");

    if (!success) {
      String message =
          String.format(
              "ValidationLoop failed after %d/%d iterations (%dms)",
              iteration, config.getIterations(), elapsed);
      log.warn(message);
      ExtentReportManager.logWarning(message);
      if (config.isThrowOnFailure()) {
        throw new AssertionError(message);
      }
    }
    return result;
  }

  /** Poll until {@code supplier} returns {@code true}. */
  public ValidationResult<Boolean> validateTrue(Supplier<Boolean> supplier) {
    return validate(supplier, (actual, expected) -> Boolean.TRUE.equals(actual), Boolean.TRUE);
  }

  /** Poll until {@code supplier} returns a value equal to {@code expected}. */
  public <T> ValidationResult<T> validateEquals(Supplier<T> supplier, T expected) {
    return validate(supplier, Objects::equals, expected);
  }

  /** Poll until {@code supplier}'s result contains {@code expected}. */
  public ValidationResult<String> validateContains(Supplier<String> supplier, String expected) {
    return validate(
        supplier, (actual, exp) -> actual != null && exp != null && actual.contains(exp), expected);
  }

  /** Poll until {@code predicate} evaluates to true against {@code supplier}'s result. */
  public <T> ValidationResult<T> validateCondition(Supplier<T> supplier, Predicate<T> predicate) {
    Objects.requireNonNull(predicate, "predicate");
    return validate(supplier, (actual, expected) -> predicate.test(actual), null);
  }

  private void sleepInterval() {
    try {
      long ms =
          (config instanceof ExponentialBackoffLoopConfig)
              ? ((ExponentialBackoffLoopConfig) config).nextIntervalMillis()
              : config.getIntervalMillis();

      if (ms > 0) {
        Thread.sleep(ms);
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }
}
