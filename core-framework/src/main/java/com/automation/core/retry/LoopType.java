package com.automation.core.retry;

/**
 * Defines loop/retry strategies for validation
 */
public enum LoopType {
    /**
     * Check only once, no retry
     */
    ONCE(1, 0),
    
    /**
     * Short loop: 15 iterations, 2 seconds between each
     */
    SHORT(15, 2000),
    
    /**
     * Long loop: 60 iterations, 5 seconds between each
     */
    LONG(60, 5000),
    
    /**
     * Custom loop: configurable iterations and interval
     */
    CUSTOM(0, 0);
    
    private final int iterations;
    private final long intervalMillis;
    
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
    
    /**
     * Get total timeout in milliseconds
     */
    public long getTotalTimeoutMillis() {
        return iterations * intervalMillis;
    }
    
    /**
     * Create custom loop type
     */
    public static LoopType custom(int iterations, long intervalMillis) {
        LoopType custom = CUSTOM;
        return new LoopType(iterations, intervalMillis) {
            @Override
            public String toString() {
                return String.format("CUSTOM(%d iterations, %dms interval)", 
                    iterations, intervalMillis);
            }
        };
    }
}
