package com.smbc.raft.core.health;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import lombok.Builder;
import lombok.Getter;

/**
 * Immutable result of a single health check execution. Status is deliberately a simple three-state
 * enum so the Spring Boot layer you add later can map it to HTTP 200/207/503 without knowing about
 * this library's internals.
 */
@Getter
@Builder
public class HealthCheckResult {

  public enum Status {
    /** Check passed — component is operating normally. */
    UP,
    /** Check passed with warnings — operating but degraded. */
    DEGRADED,
    /** Check failed — component is unavailable or in error. */
    DOWN,
    /** Check was not applicable on this platform and was skipped. */
    SKIPPED
  }

  private final String name;
  private final String category;
  private final Status status;
  private final String detail;
  private final long durationMillis;
  private final Instant checkedAt;

  private static final DateTimeFormatter FMT =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

  public boolean isHealthy() {
    return status == Status.UP || status == Status.SKIPPED;
  }

  public String timestamp() {
    return FMT.format(checkedAt);
  }

  public String summary() {
    return String.format("[%s] %s — %s (%dms): %s", category, name, status, durationMillis, detail);
  }

  @Override
  public String toString() {
    return summary();
  }
}
