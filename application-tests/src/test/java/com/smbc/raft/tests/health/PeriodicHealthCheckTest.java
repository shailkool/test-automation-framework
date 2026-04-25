package com.smbc.raft.tests.health;

import com.smbc.raft.core.environment.EnvironmentContext;
import com.smbc.raft.core.health.HealthCheckRegistry;
import com.smbc.raft.core.health.HealthCheckResult;
import com.smbc.raft.core.health.WindowsServiceCheck;
import com.smbc.raft.core.health.FilePresenceCheck;
import com.smbc.raft.core.health.AutosysJobCheck;
import com.smbc.raft.core.reporting.ExtentReportManager;
import com.smbc.raft.core.utils.BaseTest;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.testng.annotations.AfterClass;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Periodic health validation using HealthCheckRegistry.
 *
 * Two modes shown:
 *  - on-demand: run checks once inside a @Test method (smoke/nightly)
 *  - periodic:  start scheduler in @BeforeClass, poll in @Test, stop in @AfterClass
 */
public class PeriodicHealthCheckTest extends BaseTest {

    private HealthCheckRegistry registry;

    @BeforeClass
    public void buildRegistry() {
        registry = new HealthCheckRegistry(EnvironmentContext.get().getName());

        // Windows service checks — sourced from environment JSON
        var services = EnvironmentContext.get().getServices();
        if (services != null) {
            services.forEach((key, svc) ->
                registry.register(key, new WindowsServiceCheck(
                    svc.getDisplayName(), svc.getName(), svc.getHost())));
        }

        // File presence checks — sourced from environment JSON
        var filePaths = EnvironmentContext.get().getFilePaths();
        if (filePaths != null) {
            filePaths.forEach((key, fp) ->
                registry.register(key, FilePresenceCheck.builder()
                    .name(key)
                    .path(fp.getPath())
                    .expectedFormats(fp.getExpectedFormats() != null
                        ? Set.copyOf(fp.getExpectedFormats()) : Set.of())
                    .maxAgeMinutes(fp.getMaxAgeMinutes())
                    .minFileCount(fp.getMinFileCount())
                    .build()));
        }

        // AutoSys job checks
        var autosysMap = EnvironmentContext.get().getAutosys();
        if (autosysMap != null) {
            autosysMap.forEach((schedulerKey, autosysSettings) -> {
                if (autosysSettings.getJobGroups() != null) {
                    autosysSettings.getJobGroups().forEach((groupKey, group) ->
                        registry.register(schedulerKey + "." + groupKey,
                            new AutosysJobCheck(groupKey, autosysSettings, group)));
                }
            });
        }
    }

    // ── On-demand: single snapshot ────────────────────────────────────────

    @Test(groups = {"nightly", "health"},
          description = "Run all health checks once and assert all critical checks pass")
    public void testAllChecksOnDemand() {
        ExtentReportManager.assignCategory("Health", "OnDemand");

        List<HealthCheckResult> results = registry.runAll();

        if (results.isEmpty()) {
            ExtentReportManager.logInfo("No health checks configured for this environment");
            return;
        }

        results.forEach(r -> {
            if (r.isHealthy()) {
                ExtentReportManager.logPass(r.summary());
            } else {
                ExtentReportManager.logFail(r.summary());
            }
        });

        List<HealthCheckResult> failures = registry.failures();
        Assert.assertTrue(failures.isEmpty(),
            failures.size() + " check(s) failed: " +
            failures.stream().map(HealthCheckResult::getName)
                .reduce((a, b) -> a + ", " + b).orElse(""));
    }

    // ── Periodic: poll every N minutes, validate at end ──────────────────

    @Test(groups = {"nightly", "health"},
          description = "Poll health checks every 2 minutes for 10 minutes, assert stable")
    public void testChecksRemainHealthyOverTime() throws InterruptedException {
        ExtentReportManager.assignCategory("Health", "Periodic");

        // 5 polls at 2-minute intervals = 10 minutes total observation window
        registry.startScheduler(2, TimeUnit.MINUTES);

        int totalPolls  = 5;
        int pollCount   = 0;
        int degraded    = 0;

        while (pollCount < totalPolls) {
            TimeUnit.MINUTES.sleep(2);
            pollCount++;

            List<HealthCheckResult> results = registry.latestResults();
            long failCount = results.stream().filter(r -> !r.isHealthy()).count();

            ExtentReportManager.logInfo(String.format(
                "Poll %d/%d — %d/%d healthy at %s",
                pollCount, totalPolls,
                results.size() - failCount, results.size(),
                results.isEmpty() ? "N/A" : results.get(0).timestamp()));

            if (failCount > 0) {
                degraded++;
                registry.failures().forEach(r ->
                    ExtentReportManager.logWarning("  " + r.summary()));
            }
        }

        registry.stopScheduler();

        // Tolerate at most 1 degraded poll out of 5 (transient blip)
        Assert.assertTrue(degraded <= 1,
            "Health checks degraded in " + degraded + "/" + totalPolls +
            " polls — indicates sustained instability");
    }

    @AfterClass(alwaysRun = true)
    public void tearDownRegistry() {
        if (registry != null) {
            registry.stopScheduler();
        }
    }
}
