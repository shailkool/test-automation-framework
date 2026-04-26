package com.smbc.raft.core.health;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.Instant;
import lombok.extern.log4j.Log4j2;

/**
 * Checks whether a named Windows service is in the RUNNING state.
 *
 * <p>Uses {@code sc [\\host] query <serviceName>} — no WMI, no extra dependencies, works from any
 * JDK 17 process that has network access to the target host.
 *
 * <p>On non-Windows agents (Linux CI containers) the check returns {@link
 * HealthCheckResult.Status#SKIPPED} so the same suite runs unchanged in Docker without platform
 * guards in test code.
 *
 * <p>Usage:
 *
 * <pre>
 *   HealthCheck check = new WindowsServiceCheck("appServer", "MyAppService", "app-qa.internal");
 *   HealthCheckResult result = check.run();
 * </pre>
 */
@Log4j2
public class WindowsServiceCheck implements HealthCheck {

  private final String checkName;
  private final String serviceName;
  private final String host; // "localhost" or remote hostname

  public WindowsServiceCheck(String checkName, String serviceName, String host) {
    this.checkName = checkName;
    this.serviceName = serviceName;
    this.host = host == null || host.isBlank() ? "localhost" : host;
  }

  /** Convenience constructor for local service checks. */
  public WindowsServiceCheck(String checkName, String serviceName) {
    this(checkName, serviceName, "localhost");
  }

  @Override
  public HealthCheckResult run() {
    long start = System.currentTimeMillis();

    if (!isWindows() && host.equalsIgnoreCase("localhost")) {
      return HealthCheckResult.builder()
          .name(checkName)
          .category("WindowsService")
          .status(HealthCheckResult.Status.SKIPPED)
          .detail("Non-Windows agent — service check skipped")
          .durationMillis(0)
          .checkedAt(Instant.now())
          .build();
    }

    try {
      String[] cmd =
          host.equalsIgnoreCase("localhost")
              ? new String[] {"sc", "query", serviceName}
              : new String[] {"sc", "\\\\" + host, "query", serviceName};

      ProcessBuilder pb = new ProcessBuilder(cmd);
      pb.redirectErrorStream(true);
      Process process = pb.start();

      StringBuilder output = new StringBuilder();
      try (BufferedReader reader =
          new BufferedReader(new InputStreamReader(process.getInputStream()))) {
        String line;
        while ((line = reader.readLine()) != null) {
          output.append(line).append("\n");
        }
      }
      process.waitFor();
      String out = output.toString();
      long elapsed = System.currentTimeMillis() - start;

      log.debug("sc query [{}] on [{}]: {}", serviceName, host, out.trim());

      if (out.contains("FAILED 1060") || out.contains("does not exist")) {
        return result(
            HealthCheckResult.Status.DOWN,
            "Service '" + serviceName + "' not found on " + host,
            elapsed);
      }
      if (out.contains("RUNNING")) {
        return result(HealthCheckResult.Status.UP, serviceName + " is RUNNING on " + host, elapsed);
      }
      if (out.contains("STOPPED")) {
        return result(
            HealthCheckResult.Status.DOWN, serviceName + " is STOPPED on " + host, elapsed);
      }
      if (out.contains("STOP_PENDING") || out.contains("START_PENDING")) {
        return result(
            HealthCheckResult.Status.DEGRADED,
            serviceName + " is transitioning on " + host,
            elapsed);
      }
      return result(
          HealthCheckResult.Status.DEGRADED, "Unexpected sc output: " + out.trim(), elapsed);

    } catch (Exception e) {
      long elapsed = System.currentTimeMillis() - start;
      log.error("Service check failed for {} on {}: {}", serviceName, host, e.getMessage());
      return result(
          HealthCheckResult.Status.DOWN, "Error querying service: " + e.getMessage(), elapsed);
    }
  }

  private HealthCheckResult result(HealthCheckResult.Status status, String detail, long ms) {
    return HealthCheckResult.builder()
        .name(checkName)
        .category("WindowsService")
        .status(status)
        .detail(detail)
        .durationMillis(ms)
        .checkedAt(Instant.now())
        .build();
  }

  private static boolean isWindows() {
    return System.getProperty("os.name", "").toLowerCase().contains("windows");
  }
}
