package com.smbc.raft.tests.health;

import com.smbc.raft.core.environment.AutosysJobSettings;
import com.smbc.raft.core.environment.EnvironmentContext;
import com.smbc.raft.core.health.AutosysJobCheck;
import com.smbc.raft.core.health.HealthCheckResult;
import com.smbc.raft.core.reporting.ExtentReportManager;
import com.smbc.raft.core.retry.ValidationLoop;
import com.smbc.raft.core.utils.BaseTest;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.concurrent.TimeUnit;

/**
 * AutoSys job status validation.
 * Covers two patterns:
 *   - snapshot: assert jobs are SUCCESS right now
 *   - wait:     poll until jobs reach SUCCESS within a timeout
 */
public class AutosysJobCheckTest extends BaseTest {

    @Test(groups = {"nightly", "health"},
          description = "Assert all configured AutoSys job groups are in expected state")
    public void testAutosysJobsSnapshot() {
        ExtentReportManager.assignCategory("Health", "AutoSys", "Snapshot");

        var autosysMap = EnvironmentContext.get().getAutosys();
        if (autosysMap == null || autosysMap.isEmpty()) {
            ExtentReportManager.logInfo("No AutoSys configuration for this environment — skipping");
            return;
        }

        boolean allHealthy = true;
        for (var schedulerEntry : autosysMap.entrySet()) {
            AutosysJobSettings settings = schedulerEntry.getValue();
            if (settings.getJobGroups() == null) continue;

            for (var groupEntry : settings.getJobGroups().entrySet()) {
                AutosysJobCheck check = new AutosysJobCheck(
                    groupEntry.getKey(), settings, groupEntry.getValue());

                HealthCheckResult result = check.run();

                if (result.isHealthy()) {
                    ExtentReportManager.logPass(result.summary());
                } else {
                    ExtentReportManager.logFail(result.summary());
                    if (groupEntry.getValue().isCritical()) {
                        allHealthy = false;
                    }
                }
            }
        }

        Assert.assertTrue(allHealthy, "One or more critical AutoSys job groups not in expected state");
    }

    @Test(groups = {"nightly", "health"},
          description = "Poll AutoSys until daily feed jobs complete within 30 minutes")
    public void testDailyFeedJobsCompleteWithinTimeout() {
        ExtentReportManager.assignCategory("Health", "AutoSys", "WaitForCompletion");

        var autosysSettings = EnvironmentContext.get().getAutosys();
        if (autosysSettings == null || !autosysSettings.containsKey("batchScheduler")) {
            ExtentReportManager.logInfo("batchScheduler not configured — skipping");
            return;
        }

        AutosysJobSettings settings = autosysSettings.get("batchScheduler");
        AutosysJobSettings.AutosysJobGroup feedGroup =
            settings.getJobGroups().get("dailyFeed");

        if (feedGroup == null) {
            ExtentReportManager.logInfo("dailyFeed group not configured — skipping");
            return;
        }

        AutosysJobCheck check = new AutosysJobCheck("dailyFeed", settings, feedGroup);

        // Poll every 2 minutes for up to 30 minutes using ValidationLoop
        // 15 iterations × 2 min = 30 min maximum wait
        ValidationLoop loop = ValidationLoop.custom(15, TimeUnit.MINUTES.toMillis(2));

        var result = loop.validateCondition(
            () -> check.run(),
            healthResult -> {
                ExtentReportManager.logInfo("Poll: " + healthResult.summary());
                return healthResult.isHealthy();
            }
        );

        Assert.assertTrue(result.isSuccess(),
            "Daily feed jobs did not complete within 30 minutes. "
                + "Last status: " + result.getActual().getDetail());
    }
}
