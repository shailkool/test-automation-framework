package com.automation.core.retry;

/**
 * Defines loop/retry strategies for validation
 */
public enum LoopType {

    ONCE(1, 0),
    SHORT(15, 2000),
    LONG(60, 5000),
    CUSTOM(0, 0);

    private int iterations;
    private long intervalMillis;

    LoopType(int iterations, long intervalMillis) {
        this.iterations = iterations;
        this.intervalMillis = intervalMillis;
    }

    public int getIterations() {
        return iterations;
    }

    public long getIntervalMillis() {
        return intervalMillis;
    }

    public long getTotalTimeoutMillis() {
        return iterations * intervalMillis;
    }

    /** Configure the CUSTOM loop type */
    public static LoopType custom(int iterations, long intervalMillis) {
        CUSTOM.iterations = iterations;
        CUSTOM.intervalMillis = intervalMillis;
        return CUSTOM;
    }
}