package com.smbc.raft.core.retry;

/**
 * LoopConfig variant that doubles the sleep interval on each failed attempt,
 * capped at maxIntervalMillis.
 */
public class ExponentialBackoffLoopConfig extends LoopConfig {

    private final long initialIntervalMillis;
    private final long maxIntervalMillis;
    private int currentIteration = 0;

    public ExponentialBackoffLoopConfig(int iterations,
                                        long initialIntervalMillis,
                                        long maxIntervalMillis) {
        super(iterations, initialIntervalMillis);
        this.initialIntervalMillis = initialIntervalMillis;
        this.maxIntervalMillis = maxIntervalMillis;
    }

    /** Returns the interval for the current attempt, then advances the counter. */
    public long nextIntervalMillis() {
        long interval = Math.min(
            initialIntervalMillis * (1L << currentIteration),
            maxIntervalMillis
        );
        currentIteration++;
        return interval;
    }
}
