package com.automation.core.retry;

import lombok.Getter;
import lombok.Setter;

/**
 * Configuration for a {@link ValidationLoop}.
 *
 * <p>Expresses the number of iterations, the interval between them, and
 * whether a {@code ValidationLoop} should throw an exception when all
 * iterations exhaust without the condition being met. Static factories cover
 * the most common polling profiles:
 * <ul>
 *   <li>{@link #once()} &mdash; single evaluation, no retry.</li>
 *   <li>{@link #shortLoop()} &mdash; 15 iterations at 2&nbsp;s (~30&nbsp;s).</li>
 *   <li>{@link #longLoop()} &mdash; 60 iterations at 5&nbsp;s (~5&nbsp;min).</li>
 *   <li>{@link #custom(int, long)} &mdash; user-specified iterations/interval.</li>
 * </ul>
 */
@Getter
@Setter
public class LoopConfig {

    private int iterations;
    private long intervalMillis;
    private boolean throwOnFailure;

    public LoopConfig(int iterations, long intervalMillis) {
        this(iterations, intervalMillis, false);
    }

    public LoopConfig(int iterations, long intervalMillis, boolean throwOnFailure) {
        if (iterations <= 0) {
            throw new IllegalArgumentException("iterations must be > 0");
        }
        if (intervalMillis < 0) {
            throw new IllegalArgumentException("intervalMillis must be >= 0");
        }
        this.iterations = iterations;
        this.intervalMillis = intervalMillis;
        this.throwOnFailure = throwOnFailure;
    }

    public static LoopConfig once() {
        return new LoopConfig(1, 0);
    }

    public static LoopConfig shortLoop() {
        return new LoopConfig(
            LoopType.SHORT.getIterations(),
            LoopType.SHORT.getIntervalMillis()
        );
    }

    public static LoopConfig longLoop() {
        return new LoopConfig(
            LoopType.LONG.getIterations(),
            LoopType.LONG.getIntervalMillis()
        );
    }

    public static LoopConfig custom(int iterations, long intervalMillis) {
        return new LoopConfig(iterations, intervalMillis);
    }

    /**
     * Exponential backoff config. Each interval doubles from initialIntervalMillis
     * up to maxIntervalMillis.
     */
    public static LoopConfig exponentialBackoff(int iterations,
                                                 long initialIntervalMillis,
                                                 long maxIntervalMillis) {
        return new ExponentialBackoffLoopConfig(iterations, initialIntervalMillis, maxIntervalMillis);
    }
}
