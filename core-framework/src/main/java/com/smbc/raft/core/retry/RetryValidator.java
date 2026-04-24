package com.smbc.raft.core.retry;

import com.smbc.raft.core.reporting.ExtentReportManager;
import lombok.extern.log4j.Log4j2;

import java.util.function.Supplier;

/**
 * Provides retry/loop mechanism for validating expected vs actual results
 * Supports validation from UI, API, and Database sources
 */
@Log4j2
public class RetryValidator {
    
    /**
     * Validate with retry mechanism
     * 
     * @param expected Expected value
     * @param actualSupplier Supplier that fetches actual value
     * @param loopType Loop strategy (ONCE, SHORT, LONG, CUSTOM)
     * @param description Description of what is being validated
     * @return ValidationResult containing match status and details
     */
    public static <T> ValidationResult<T> validateWithRetry(
            T expected, 
            Supplier<T> actualSupplier, 
            LoopType loopType,
            String description) {
        
        log.info("Starting validation with {} loop: {}", loopType, description);
        ExtentReportManager.logInfo("Validating: " + description + " with " + loopType);
        
        int iterations = loopType.getIterations();
        long interval = loopType.getIntervalMillis();
        
        T actual = null;
        boolean matched = false;
        int attemptNumber = 0;
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < iterations; i++) {
            attemptNumber = i + 1;
            
            try {
                // Fetch actual value
                actual = actualSupplier.get();
                
                log.debug("Attempt {}/{}: Expected={}, Actual={}", 
                    attemptNumber, iterations, expected, actual);
                
                // Check if values match
                matched = valuesMatch(expected, actual);
                
                if (matched) {
                    long elapsedTime = System.currentTimeMillis() - startTime;
                    log.info("Match found on attempt {}/{} after {}ms", 
                        attemptNumber, iterations, elapsedTime);
                    ExtentReportManager.logPass(
                        String.format("Validation passed on attempt %d/%d (elapsed: %dms)", 
                            attemptNumber, iterations, elapsedTime));
                    break;
                }
                
                // If not the last iteration, wait before next attempt
                if (i < iterations - 1) {
                    Thread.sleep(interval);
                }
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Validation interrupted", e);
                break;
            } catch (Exception e) {
                log.error("Error during validation attempt {}/{}", attemptNumber, iterations, e);
                ExtentReportManager.logWarning(
                    String.format("Error on attempt %d: %s", attemptNumber, e.getMessage()));
            }
        }
        
        long totalElapsedTime = System.currentTimeMillis() - startTime;
        
        ValidationResult<T> result = new ValidationResult<>(
            expected, 
            actual, 
            matched, 
            attemptNumber, 
            iterations,
            totalElapsedTime,
            description
        );
        
        if (!matched) {
            log.warn("Validation failed after {} attempts ({}ms): Expected={}, Actual={}", 
                iterations, totalElapsedTime, expected, actual);
            ExtentReportManager.logFail(
                String.format("Validation failed after %d attempts (%dms). Expected: %s, Actual: %s", 
                    iterations, totalElapsedTime, expected, actual));
        }
        
        return result;
    }
    
    /**
     * Validate boolean condition with retry
     */
    public static ValidationResult<Boolean> validateConditionWithRetry(
            Supplier<Boolean> conditionSupplier,
            LoopType loopType,
            String description) {
        
        return validateWithRetry(true, conditionSupplier, loopType, description);
    }
    
    /**
     * Check if two values match
     */
    private static <T> boolean valuesMatch(T expected, T actual) {
        if (expected == null && actual == null) {
            return true;
        }
        if (expected == null || actual == null) {
            return false;
        }
        return expected.equals(actual);
    }
    
    /**
     * Validate with custom loop parameters
     */
    public static <T> ValidationResult<T> validateWithCustomLoop(
            T expected,
            Supplier<T> actualSupplier,
            int iterations,
            long intervalMillis,
            String description) {
        
        LoopType customLoop = LoopType.custom(iterations, intervalMillis);
        return validateWithRetry(expected, actualSupplier, customLoop, description);
    }
    
    /**
     * Validate string contains with retry
     */
    public static ValidationResult<String> validateStringContainsWithRetry(
            String expectedSubstring,
            Supplier<String> actualSupplier,
            LoopType loopType,
            String description) {
        
        log.info("Starting string contains validation with {} loop: {}", loopType, description);
        
        int iterations = loopType.getIterations();
        long interval = loopType.getIntervalMillis();
        
        String actual = null;
        boolean matched = false;
        int attemptNumber = 0;
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < iterations; i++) {
            attemptNumber = i + 1;
            
            try {
                actual = actualSupplier.get();
                matched = actual != null && actual.contains(expectedSubstring);
                
                if (matched) {
                    long elapsedTime = System.currentTimeMillis() - startTime;
                    log.info("String contains match found on attempt {}/{} after {}ms", 
                        attemptNumber, iterations, elapsedTime);
                    ExtentReportManager.logPass(
                        String.format("String contains validation passed on attempt %d/%d", 
                            attemptNumber, iterations));
                    break;
                }
                
                if (i < iterations - 1) {
                    Thread.sleep(interval);
                }
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("Error during string validation attempt {}", attemptNumber, e);
            }
        }
        
        long totalElapsedTime = System.currentTimeMillis() - startTime;
        
        return new ValidationResult<>(
            expectedSubstring,
            actual,
            matched,
            attemptNumber,
            iterations,
            totalElapsedTime,
            description
        );
    }
    
    /**
     * Wait until condition is true
     */
    public static boolean waitUntil(
            Supplier<Boolean> conditionSupplier,
            LoopType loopType,
            String description) {
        
        ValidationResult<Boolean> result = validateConditionWithRetry(
            conditionSupplier, loopType, description);
        return result.isMatched();
    }
    
    /**
     * Wait until element is visible (for UI)
     */
    public static boolean waitUntilVisible(
            Supplier<Boolean> visibilitySupplier,
            LoopType loopType) {
        
        return waitUntil(visibilitySupplier, loopType, "Element visibility");
    }
    
    /**
     * Wait until API returns expected status
     */
    public static boolean waitUntilApiStatus(
            int expectedStatus,
            Supplier<Integer> statusSupplier,
            LoopType loopType) {
        
        ValidationResult<Integer> result = validateWithRetry(
            expectedStatus, statusSupplier, loopType, "API status code");
        return result.isMatched();
    }
    
    /**
     * Wait until database record exists
     */
    public static boolean waitUntilRecordExists(
            Supplier<Boolean> existsSupplier,
            LoopType loopType,
            String tableName) {
        
        return waitUntil(existsSupplier, loopType, 
            "Record exists in " + tableName);
    }
}
