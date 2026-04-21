package com.automation.core.retry;

import lombok.Getter;

/**
 * Holds the result of a validation with retry
 */
@Getter
public class ValidationResult<T> {
    
    private final T expected;
    private final T actual;
    private final boolean matched;
    private final int attemptNumber;
    private final int totalAttempts;
    private final long elapsedTimeMillis;
    private final String description;
    
    public ValidationResult(T expected, T actual, boolean matched, 
                          int attemptNumber, int totalAttempts,
                          long elapsedTimeMillis, String description) {
        this.expected = expected;
        this.actual = actual;
        this.matched = matched;
        this.attemptNumber = attemptNumber;
        this.totalAttempts = totalAttempts;
        this.elapsedTimeMillis = elapsedTimeMillis;
        this.description = description;
    }
    
    /**
     * Get success status
     */
    public boolean isSuccess() {
        return matched;
    }
    
    /**
     * Get failure status
     */
    public boolean isFailed() {
        return !matched;
    }
    
    /**
     * Get elapsed time in seconds
     */
    public double getElapsedTimeSeconds() {
        return elapsedTimeMillis / 1000.0;
    }

    /** Alias for {@link #getAttemptNumber()} used by ValidationLoop callers. */
    public int getIterations() {
        return attemptNumber;
    }

    /** Alias for {@link #getElapsedTimeMillis()}. */
    public long getDurationMillis() {
        return elapsedTimeMillis;
    }

    /** Alias for {@link #getElapsedTimeSeconds()}. */
    public double getDurationSeconds() {
        return getElapsedTimeSeconds();
    }
    
    /**
     * Check if validation succeeded on first attempt
     */
    public boolean isFirstAttemptSuccess() {
        return matched && attemptNumber == 1;
    }
    
    /**
     * Get formatted summary
     */
    public String getSummary() {
        return String.format(
            "%s: %s (Attempt %d/%d, %.2fs) - Expected: %s, Actual: %s",
            matched ? "PASS" : "FAIL",
            description,
            attemptNumber,
            totalAttempts,
            getElapsedTimeSeconds(),
            expected,
            actual
        );
    }
    
    @Override
    public String toString() {
        return getSummary();
    }
}
