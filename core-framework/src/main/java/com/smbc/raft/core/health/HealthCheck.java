package com.smbc.raft.core.health;

/**
 * A single verifiable health condition.
 *
 * <p>Implementations must be stateless and thread-safe — the registry
 * may call {@link #run()} from multiple threads simultaneously when
 * running checks in parallel or on a schedule.
 *
 * <p>Checked exceptions are deliberately not thrown — wrap them in the
 * result detail. This keeps scheduler error handling simple.
 */
@FunctionalInterface
public interface HealthCheck {

    /**
     * Execute the check and return a result.
     * Must never throw — catch all exceptions internally and return
     * a {@link HealthCheckResult.Status#DOWN} result with the error
     * message in the detail field.
     */
    HealthCheckResult run();
}
