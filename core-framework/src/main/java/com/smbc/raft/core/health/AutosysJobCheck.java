package com.smbc.raft.core.health;

import com.smbc.raft.core.database.DatabaseManager;
import com.smbc.raft.core.environment.AutosysJobSettings;
import lombok.extern.log4j.Log4j2;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * {@link HealthCheck} implementation that queries one or more AutoSys jobs
 * and returns an aggregated {@link HealthCheckResult}.
 *
 * <p>Supports three query modes declared in the environment JSON:
 * <ul>
 *   <li><b>CLI</b>  — invokes {@code autorep -j <name> -s} via Runtime.exec</li>
 *   <li><b>REST</b> — calls the AutoSys WCC REST API (r11.3.6+)</li>
 *   <li><b>DB</b>   — queries the {@code ujo_job} view via DatabaseManager</li>
 * </ul>
 *
 * <p>Each instance covers one named job group (a list of job names that
 * must all reach their expected status). Register one check per group
 * in the {@link HealthCheckRegistry}.
 *
 * <p>Usage:
 * <pre>
 *   AutosysJobSettings settings = EnvironmentContext.get()
 *       .getAutosys().get("batchScheduler");
 *
 *   registry.register("dailyFeed",
 *       new AutosysJobCheck("dailyFeed", settings,
 *           settings.getJobGroups().get("dailyFeed")));
 * </pre>
 */
@Log4j2
public class AutosysJobCheck implements HealthCheck {

    private final String checkName;
    private final AutosysJobSettings settings;
    private final AutosysJobSettings.AutosysJobGroup group;

    public AutosysJobCheck(String checkName,
                           AutosysJobSettings settings,
                           AutosysJobSettings.AutosysJobGroup group) {
        this.checkName = checkName;
        this.settings  = settings;
        this.group     = group;
    }

    @Override
    public HealthCheckResult run() {
        long start = System.currentTimeMillis();

        if (group.getJobs() == null || group.getJobs().isEmpty()) {
            return result(HealthCheckResult.Status.SKIPPED,
                "No jobs configured in group", start);
        }

        try {
            return switch (settings.getQueryMode().toUpperCase()) {
                case "CLI"  -> runCli(start);
                case "REST" -> runRest(start);
                case "DB"   -> runDb(start);
                default     -> result(HealthCheckResult.Status.DOWN,
                    "Unknown queryMode: " + settings.getQueryMode(), start);
            };
        } catch (Exception e) {
            log.error("AutosysJobCheck [{}] failed: {}", checkName, e.getMessage(), e);
            return result(HealthCheckResult.Status.DOWN,
                "Unexpected error: " + e.getMessage(), start);
        }
    }

    // ── CLI mode ──────────────────────────────────────────────────────────

    private HealthCheckResult runCli(long start) {
        List<String> failures = new ArrayList<>();
        List<String> details  = new ArrayList<>();
        AutosysJobStatus expected = AutosysJobStatus.fromLabel(group.getExpectedStatus());

        for (String jobName : group.getJobs()) {
            AutosysJobStatus actual = queryViaCli(jobName);
            String line = jobName + "=" + actual;
            details.add(line);

            if (actual == AutosysJobStatus.UNKNOWN) {
                failures.add(jobName + ": job not found or autorep unavailable");
            } else if (actual != expected) {
                failures.add(jobName + ": expected " + expected + ", got " + actual);
            }
        }

        return buildResult(failures, details, start);
    }

    /**
     * Parses autorep output. Standard format:
     *
     *   Job Name                        Last Start           Last End     ST/Ex
     *   -------------------------------- -------------------- ------------ -----
     *   TRADE_FEED_EXTRACT              03/15/2025 02:00:00  03/15 02:14  SU/0
     *
     * The status code is in the "ST" column — two letters before the slash.
     */
    private AutosysJobStatus queryViaCli(String jobName) {
        try {
            String[] cmd = {settings.getAutorepPath(), "-j", jobName, "-s"};
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            String statusCode = null;
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    // Skip header lines
                    if (line.isBlank() || line.startsWith("---") ||
                        line.startsWith("Job Name")) continue;
                    // The status is the last token before "/exitcode"
                    // e.g. "TRADE_FEED_EXTRACT    ... SU/0"
                    if (line.contains(jobName)) {
                        String trimmed = line.trim();
                        String[] parts = trimmed.split("\\s+");
                        if (parts.length >= 1) {
                            String last = parts[parts.length - 1];
                            // "SU/0" → take part before slash
                            statusCode = last.contains("/")
                                ? last.split("/")[0]
                                : last;
                        }
                    }
                }
            }
            process.waitFor();
            log.debug("autorep [{}] status code: {}", jobName, statusCode);
            return statusCode != null
                ? AutosysJobStatus.fromCliCode(statusCode)
                : AutosysJobStatus.UNKNOWN;

        } catch (Exception e) {
            log.warn("autorep query failed for job {}: {}", jobName, e.getMessage());
            return AutosysJobStatus.UNKNOWN;
        }
    }

    // ── REST mode ─────────────────────────────────────────────────────────

    /**
     * AutoSys WCC REST API endpoint (r11.3.6+):
     *   GET /CA7/api/job/{jobName}/status
     *
     * Requires a session token obtained via POST /CA7/api/login.
     * Uses basic RestAssured directly to keep the health library
     * free of the application-automation layer.
     */
    private HealthCheckResult runRest(long start) {
        List<String> failures = new ArrayList<>();
        List<String> details  = new ArrayList<>();
        AutosysJobStatus expected = AutosysJobStatus.fromLabel(group.getExpectedStatus());

        String token = authenticateRest();
        if (token == null) {
            return result(HealthCheckResult.Status.DOWN,
                "Failed to authenticate with AutoSys REST API at " + settings.getRestBaseUrl(),
                start);
        }

        for (String jobName : group.getJobs()) {
            AutosysJobStatus actual = queryViaRest(jobName, token);
            String line = jobName + "=" + actual;
            details.add(line);

            if (actual == AutosysJobStatus.UNKNOWN) {
                failures.add(jobName + ": job not found or REST query failed");
            } else if (actual != expected) {
                failures.add(jobName + ": expected " + expected + ", got " + actual);
            }
        }

        return buildResult(failures, details, start);
    }

    private String authenticateRest() {
        try {
            io.restassured.response.Response response = io.restassured.RestAssured
                .given()
                .baseUri(settings.getRestBaseUrl())
                .relaxedHTTPSValidation()
                .contentType("application/json")
                .body("{\"username\":\"" + settings.getRestUsername()
                    + "\",\"password\":\"" + resolvePassword(settings.getRestPassword()) + "\"}")
                .post("/CA7/api/login");

            if (response.getStatusCode() == 200) {
                return response.jsonPath().getString("token");
            }
            log.warn("AutoSys REST auth failed: HTTP {}", response.getStatusCode());
            return null;
        } catch (Exception e) {
            log.error("AutoSys REST auth error: {}", e.getMessage());
            return null;
        }
    }

    private AutosysJobStatus queryViaRest(String jobName, String token) {
        try {
            io.restassured.response.Response response = io.restassured.RestAssured
                .given()
                .baseUri(settings.getRestBaseUrl())
                .relaxedHTTPSValidation()
                .header("Authorization", "Bearer " + token)
                .get("/CA7/api/job/{job}/status", jobName);

            if (response.getStatusCode() == 200) {
                String status = response.jsonPath().getString("status");
                log.debug("REST [{}] status: {}", jobName, status);
                return AutosysJobStatus.fromLabel(status);
            }
            log.warn("REST query for job {} returned HTTP {}", jobName, response.getStatusCode());
            return AutosysJobStatus.UNKNOWN;
        } catch (Exception e) {
            log.warn("REST query failed for job {}: {}", jobName, e.getMessage());
            return AutosysJobStatus.UNKNOWN;
        }
    }

    // ── DB mode ───────────────────────────────────────────────────────────

    /**
     * Queries the AutoSys database view directly.
     * The standard view is "ujo_job" — available in all AutoSys versions.
     *
     * Column used: "status" (integer in older versions, varchar in newer).
     * Integer status codes:
     *   1=INACTIVE, 3=WAITING, 5=ON_HOLD, 7=ON_ICE, 9=ACTIVATED,
     *   11=STARTING, 13=RUNNING, 15=SUCCESS, 17=FAILURE
     */
    private HealthCheckResult runDb(long start) {
        List<String> failures = new ArrayList<>();
        List<String> details  = new ArrayList<>();
        AutosysJobStatus expected = AutosysJobStatus.fromLabel(group.getExpectedStatus());

        DatabaseManager db = DatabaseManager.getInstance(settings.getDatabaseKey());

        for (String jobName : group.getJobs()) {
            AutosysJobStatus actual = queryViaDb(db, jobName);
            String line = jobName + "=" + actual;
            details.add(line);

            if (actual == AutosysJobStatus.UNKNOWN) {
                failures.add(jobName + ": job not found in AutoSys DB");
            } else if (actual != expected) {
                failures.add(jobName + ": expected " + expected + ", got " + actual);
            }
        }

        return buildResult(failures, details, start);
    }

    private AutosysJobStatus queryViaDb(DatabaseManager db, String jobName) {
        try {
            // ujo_job is the standard AutoSys job status view
            List<Map<String, Object>> rows = db.executeQuery(
                "SELECT status FROM ujo_job WHERE job_name = ?", jobName);

            if (rows.isEmpty()) {
                log.warn("No ujo_job row found for job: {}", jobName);
                return AutosysJobStatus.UNKNOWN;
            }

            Object rawStatus = rows.get(0).get("status");
            log.debug("DB [{}] raw status: {}", jobName, rawStatus);

            if (rawStatus instanceof Number) {
                return fromNumericStatus(((Number) rawStatus).intValue());
            }
            return AutosysJobStatus.fromLabel(rawStatus.toString());

        } catch (Exception e) {
            log.warn("DB query failed for job {}: {}", jobName, e.getMessage());
            return AutosysJobStatus.UNKNOWN;
        }
    }

    /** Maps AutoSys integer status codes from the ujo_job table. */
    private static AutosysJobStatus fromNumericStatus(int code) {
        return switch (code) {
            case 1  -> AutosysJobStatus.INACTIVE;
            case 3  -> AutosysJobStatus.WAITING;
            case 5  -> AutosysJobStatus.ON_HOLD;
            case 7  -> AutosysJobStatus.ON_ICE;
            case 9  -> AutosysJobStatus.ACTIVATED;
            case 11 -> AutosysJobStatus.STARTING;
            case 13 -> AutosysJobStatus.RUNNING;
            case 15 -> AutosysJobStatus.SUCCESS;
            case 17 -> AutosysJobStatus.FAILURE;
            default -> AutosysJobStatus.UNKNOWN;
        };
    }

    // ── Shared helpers ────────────────────────────────────────────────────

    private HealthCheckResult buildResult(List<String> failures,
                                          List<String> details, long start) {
        long elapsed = System.currentTimeMillis() - start;
        String detail = String.join(", ", details);

        if (failures.isEmpty()) {
            return result(HealthCheckResult.Status.UP,
                group.getDescription() + " — all jobs " + group.getExpectedStatus()
                    + " | " + detail, start);
        }

        HealthCheckResult.Status status = group.isCritical()
            ? HealthCheckResult.Status.DOWN
            : HealthCheckResult.Status.DEGRADED;

        return HealthCheckResult.builder()
            .name(checkName).category("AutosysJob")
            .status(status)
            .detail(failures.size() + " job(s) not in expected state: "
                + String.join("; ", failures) + " | " + detail)
            .durationMillis(elapsed)
            .checkedAt(Instant.now())
            .build();
    }

    private HealthCheckResult result(HealthCheckResult.Status status,
                                     String detail, long start) {
        return HealthCheckResult.builder()
            .name(checkName).category("AutosysJob")
            .status(status).detail(detail)
            .durationMillis(System.currentTimeMillis() - start)
            .checkedAt(Instant.now()).build();
    }

    /** Resolves ${ENV_VAR} placeholders in credential fields. */
    private static String resolvePassword(String value) {
        if (value != null && value.startsWith("${") && value.endsWith("}")) {
            String var = value.substring(2, value.length() - 1);
            String env = System.getenv(var);
            return env != null ? env : value;
        }
        return value;
    }
}
