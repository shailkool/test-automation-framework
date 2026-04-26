package com.smbc.raft.core.reporting;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smbc.raft.core.config.ConfigurationManager;
import lombok.Builder;
import lombok.Data;
import lombok.extern.log4j.Log4j2;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;

/**
 * TestNG Listener that collects machine-readable test metrics.
 * 
 * <p>Emits a JSONL (JSON Lines) file containing detailed data for every test execution,
 * including duration, status, environment, and retry counts. This file serves as
 * the source for time-series analysis and trend dashboards (e.g., in Grafana or ELK).
 */
@Log4j2
public class TestMetricsCollector implements ITestListener {

    private static final ConfigurationManager CONFIG = ConfigurationManager.getInstance();
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private String metricsFilePath;

    @Override
    public void onStart(ITestContext context) {
        String timestamp = Instant.now().toString().replace(":", "-").replace(".", "-");
        String metricsDir = CONFIG.getMetricsDir();
        metricsFilePath = metricsDir + "run-" + timestamp + ".jsonl";

        try {
            Files.createDirectories(Paths.get(metricsDir));
            log.info("Metrics collector initialized. Writing to: {}", metricsFilePath);
        } catch (IOException e) {
            log.error("Failed to create metrics directory: {}", metricsDir, e);
        }
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        recordMetric(result, "PASS");
    }

    @Override
    public void onTestFailure(ITestResult result) {
        recordMetric(result, "FAIL");
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        // TestNG skips can be due to dependOnMethods or intentional skip
        recordMetric(result, "SKIP");
    }

    private void recordMetric(ITestResult result, String status) {
        if (metricsFilePath == null) {
            return;
        }

        // Retrieve retry count if set by FlakeRetryAnalyzer
        Object retryAttr = result.getAttribute("retryCount");
        int retryCount = (retryAttr instanceof Integer) ? (Integer) retryAttr : 0;

        TestMetric metric = TestMetric.builder()
                .testName(result.getTestClass().getName() + "." + result.getName())
                .suiteName(result.getTestContext().getSuite().getName())
                .durationMillis(result.getEndMillis() - result.getStartMillis())
                .status(status)
                .timestamp(Instant.ofEpochMilli(result.getEndMillis()).toString())
                .environment(CONFIG.getEnvironment())
                .retryCount(retryCount)
                .errorMessage(result.getThrowable() != null ? result.getThrowable().getMessage() : null)
                .build();

        try (FileWriter writer = new FileWriter(metricsFilePath, true)) {
            writer.write(MAPPER.writeValueAsString(metric) + "\n");
        } catch (IOException e) {
            log.error("Failed to write test metric for {}", metric.getTestName(), e);
        }
    }

    /** Data model for a single test execution metric. */
    @Data
    @Builder
    public static class TestMetric {
        private String testName;
        private String suiteName;
        private long durationMillis;
        private String status;
        private String timestamp;
        private String environment;
        private int retryCount;
        private String errorMessage;
    }
}
