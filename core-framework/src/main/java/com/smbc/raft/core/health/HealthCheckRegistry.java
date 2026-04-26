package com.smbc.raft.core.health;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.extern.log4j.Log4j2;

/**
 * Central registry for named {@link HealthCheck} instances.
 *
 * <p>Supports two usage modes:
 *
 * <ol>
 *   <li><b>On-demand</b> — call {@link #runAll()} or {@link #run(String)} from a TestNG test
 *       method. Results are returned synchronously.
 *   <li><b>Periodic</b> — call {@link #startScheduler(long, TimeUnit)} to run all registered checks
 *       on a background thread. The latest results are always available via {@link
 *       #latestResults()}. Call {@link #stopScheduler()} when done (e.g. {@code @AfterSuite}).
 * </ol>
 *
 * <p>The registry is intentionally framework-agnostic. Your future Spring Boot app can consume it
 * as a plain singleton bean — no Spring annotations are needed here.
 *
 * <p>Thread safety: registration and result access are both safe for concurrent callers. Checks
 * themselves run on a single background thread to avoid hammering services under test.
 *
 * <p>Usage:
 *
 * <pre>
 *   HealthCheckRegistry registry = new HealthCheckRegistry("QA Environment");
 *   registry.register("appServer",  new WindowsServiceCheck("appServer", "MyApp", "app-qa"));
 *   registry.register("tradeFiles", FilePresenceCheck.builder()
 *       .name("tradeFiles").path("C:/tmp/trades")
 *       .expectedFormats(Set.of("csv")).maxAgeMinutes(60).minFileCount(1).build());
 *
 *   // On-demand (in a @Test)
 *   List&lt;HealthCheckResult&gt; results = registry.runAll();
 *
 *   // Periodic (start before suite, stop after suite)
 *   registry.startScheduler(5, TimeUnit.MINUTES);
 *   ...
 *   List&lt;HealthCheckResult&gt; latest = registry.latestResults();
 *   registry.stopScheduler();
 * </pre>
 */
@Log4j2
public class HealthCheckRegistry {

  private final String registryName;

  // LinkedHashMap preserves registration order in reports
  private final Map<String, HealthCheck> checks = new LinkedHashMap<>();

  // Latest results — replaced atomically after every poll cycle
  private volatile List<HealthCheckResult> latest = Collections.emptyList();

  private ScheduledExecutorService scheduler;

  public HealthCheckRegistry(String registryName) {
    this.registryName = registryName;
  }

  // ── Registration ──────────────────────────────────────────────────────

  /**
   * Register a named health check. Replaces any existing check with the same name. Fluent — returns
   * {@code this} for chaining.
   */
  public synchronized HealthCheckRegistry register(String name, HealthCheck check) {
    Objects.requireNonNull(name, "name");
    Objects.requireNonNull(check, "check");
    checks.put(name, check);
    log.debug("[{}] registered check '{}'", registryName, name);
    return this;
  }

  /** Remove a named check. No-op if the name is not registered. */
  public synchronized HealthCheckRegistry deregister(String name) {
    checks.remove(name);
    return this;
  }

  public synchronized Set<String> registeredNames() {
    return Collections.unmodifiableSet(new LinkedHashSet<>(checks.keySet()));
  }

  // ── On-demand execution ───────────────────────────────────────────────

  /**
   * Run all registered checks synchronously and return results. Updates {@link #latestResults()} as
   * a side effect.
   */
  public List<HealthCheckResult> runAll() {
    log.info("[{}] running {} health check(s)", registryName, checks.size());
    List<HealthCheckResult> results;
    synchronized (this) {
      results = checks.values().stream().map(this::safeRun).collect(Collectors.toList());
    }
    latest = Collections.unmodifiableList(results);
    logSummary(results);
    return latest;
  }

  /**
   * Run a single named check synchronously.
   *
   * @throws NoSuchElementException if the name is not registered
   */
  public HealthCheckResult run(String name) {
    HealthCheck check;
    synchronized (this) {
      check = checks.get(name);
    }
    if (check == null) {
      throw new NoSuchElementException("No health check registered with name: " + name);
    }
    HealthCheckResult result = safeRun(check);
    log.info("[{}] {}", registryName, result.summary());
    return result;
  }

  // ── Periodic scheduling ───────────────────────────────────────────────

  /**
   * Start a background thread that runs all checks every {@code period}. Results are stored and
   * accessible via {@link #latestResults()}. Calling this when a scheduler is already running is a
   * no-op.
   */
  public synchronized void startScheduler(long period, TimeUnit unit) {
    if (scheduler != null && !scheduler.isShutdown()) {
      log.warn("[{}] scheduler already running", registryName);
      return;
    }
    scheduler =
        Executors.newSingleThreadScheduledExecutor(
            r -> {
              Thread t = new Thread(r, "health-registry-" + registryName);
              t.setDaemon(true);
              return t;
            });
    scheduler.scheduleAtFixedRate(
        () -> {
          try {
            runAll();
          } catch (Exception e) {
            // Exceptions must not kill the scheduler thread
            log.error("[{}] unexpected error in scheduled check cycle", registryName, e);
          }
        },
        0,
        period,
        unit);
    log.info("[{}] health check scheduler started — interval: {} {}", registryName, period, unit);
  }

  /**
   * Stop the background scheduler. Waits up to 5 seconds for the current cycle to finish. Safe to
   * call even if the scheduler was never started.
   */
  public synchronized void stopScheduler() {
    if (scheduler == null) return;
    scheduler.shutdown();
    try {
      if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
        scheduler.shutdownNow();
      }
    } catch (InterruptedException e) {
      scheduler.shutdownNow();
      Thread.currentThread().interrupt();
    }
    log.info("[{}] health check scheduler stopped", registryName);
    scheduler = null;
  }

  // ── Result access ─────────────────────────────────────────────────────

  /**
   * Returns the results from the most recent {@link #runAll()} call, whether triggered on-demand or
   * by the scheduler. Returns an empty list before the first run.
   */
  public List<HealthCheckResult> latestResults() {
    return latest;
  }

  /**
   * Returns true if the most recent results show all checks UP or SKIPPED. Returns false if no
   * checks have been run yet.
   */
  public boolean isHealthy() {
    List<HealthCheckResult> current = latest;
    if (current.isEmpty()) return false;
    return current.stream().allMatch(HealthCheckResult::isHealthy);
  }

  /** Returns only the results with status DOWN or DEGRADED. */
  public List<HealthCheckResult> failures() {
    return latest.stream().filter(r -> !r.isHealthy()).collect(Collectors.toList());
  }

  // ── Internal ──────────────────────────────────────────────────────────

  private HealthCheckResult safeRun(HealthCheck check) {
    try {
      return check.run();
    } catch (Exception e) {
      // Should never happen if implementations follow the contract,
      // but guard here so one bad check can't break the whole cycle.
      log.error("[{}] check threw unexpectedly: {}", registryName, e.getMessage(), e);
      return HealthCheckResult.builder()
          .name("unknown")
          .category("Error")
          .status(HealthCheckResult.Status.DOWN)
          .detail("Uncaught exception: " + e.getMessage())
          .durationMillis(0)
          .checkedAt(java.time.Instant.now())
          .build();
    }
  }

  private void logSummary(List<HealthCheckResult> results) {
    long up = results.stream().filter(r -> r.getStatus() == HealthCheckResult.Status.UP).count();
    long down =
        results.stream().filter(r -> r.getStatus() == HealthCheckResult.Status.DOWN).count();
    long deg =
        results.stream().filter(r -> r.getStatus() == HealthCheckResult.Status.DEGRADED).count();
    log.info(
        "[{}] summary — UP:{} DOWN:{} DEGRADED:{} SKIPPED:{}",
        registryName,
        up,
        down,
        deg,
        results.size() - up - down - deg);
    results.stream()
        .filter(r -> !r.isHealthy())
        .forEach(r -> log.warn("[{}] {}", registryName, r.summary()));
  }
}
