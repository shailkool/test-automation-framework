package com.automation.tests.bdd.runner;

import com.automation.core.environment.EnvironmentContext;
import com.automation.core.runprofile.RunProfile;
import com.automation.core.runprofile.RunProfileContext;
import lombok.extern.log4j.Log4j2;
import net.masterthought.cucumber.Configuration;
import net.masterthought.cucumber.ReportBuilder;
import net.masterthought.cucumber.presentation.PresentationMode;
import net.masterthought.cucumber.sorting.SortingMethod;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

/**
 * Post-run hook that converts Cucumber's JSON output into a masterthought
 * multi-tab HTML report.
 *
 * <p>Registered from {@link TaggedCucumberRunner}'s static initialiser as a
 * JVM shutdown hook so it fires after Cucumber has finished flushing
 * {@code cucumber.json} regardless of how the runner was launched (Maven,
 * IDE, or {@code java -jar}).
 *
 * <p>If the JSON file does not exist (e.g. no scenarios matched the tag
 * filter) the generator logs a warning and exits quietly rather than
 * failing the build.
 */
@Log4j2
final class MasterthoughtReportGenerator {

    private static final String JSON_PATH = "test-output/cucumber/cucumber.json";
    /**
     * masterthought appends its own {@code cucumber-html-reports} subdirectory
     * to whatever is configured here, so {@code test-output} produces a final
     * path of {@code test-output/cucumber-html-reports/}.
     */
    private static final String OUTPUT_DIR = "test-output";
    private static final String PROJECT_NAME = "Test Automation Framework";
    private static final String REPORT_SUBDIR = "cucumber-html-reports";

    private static volatile boolean registered = false;

    private MasterthoughtReportGenerator() {
    }

    /** Register a one-shot shutdown hook that invokes {@link #generate()}. */
    static synchronized void registerShutdownHook() {
        if (registered) {
            return;
        }
        Runtime.getRuntime().addShutdownHook(new Thread(
            MasterthoughtReportGenerator::generate,
            "masterthought-report-generator"
        ));
        registered = true;
    }

    static void generate() {
        Path json = Path.of(JSON_PATH);
        if (!Files.exists(json)) {
            log.warn("Skipping masterthought report: {} does not exist", json.toAbsolutePath());
            return;
        }

        File outputDir = new File(OUTPUT_DIR);
        if (!outputDir.exists() && !outputDir.mkdirs()) {
            log.warn("Could not create masterthought output directory: {}", outputDir);
        }

        Configuration configuration = new Configuration(outputDir, PROJECT_NAME);
        configuration.setBuildNumber(resolveBuildNumber());
        configuration.setSortingMethod(SortingMethod.NATURAL);
        configuration.addPresentationModes(PresentationMode.RUN_WITH_JENKINS);
        configuration.addClassifications("Module", "application-tests");
        configuration.addClassifications("OS", System.getProperty("os.name"));
        configuration.addClassifications("JVM", System.getProperty("java.version"));
        addEnvironmentClassifications(configuration);
        addRunProfileClassifications(configuration);
        String tagExpression = System.getProperty("cucumber.filter.tags");
        if (tagExpression != null && !tagExpression.isBlank()) {
            configuration.addClassifications("Tag filter", tagExpression);
        }

        List<String> jsonFiles = Collections.singletonList(json.toString());
        try {
            new ReportBuilder(jsonFiles, configuration).generateReports();
            log.info(
                "Masterthought report generated: {}",
                new File(new File(outputDir, REPORT_SUBDIR), "overview-features.html")
                    .getAbsolutePath()
            );
        } catch (Exception e) {
            log.error("Failed to generate masterthought report", e);
        }
    }

    private static String resolveBuildNumber() {
        String buildNumber = System.getProperty("build.number");
        if (buildNumber != null && !buildNumber.isBlank()) {
            return buildNumber;
        }
        return String.valueOf(System.currentTimeMillis() / 1000);
    }

    private static void addEnvironmentClassifications(Configuration configuration) {
        try {
            com.automation.core.environment.EnvironmentConfig config = EnvironmentContext.get();
            configuration.addClassifications("Environment", config.getName());
            String description = config.getDescription();
            if (description != null && !description.isBlank()) {
                configuration.addClassifications("Environment details", description);
            }
        } catch (RuntimeException e) {
            log.warn("Could not add environment classifications to report: {}", e.getMessage());
        }
    }

    private static void addRunProfileClassifications(Configuration configuration) {
        try {
            RunProfileContext ctx = RunProfileContext.getInstance();
            RunProfile profile = ctx.getProfile();
            configuration.addClassifications("Run profile", ctx.getProfileName());

            // Browser / Headless / Screenshot mode are only meaningful when
            // the profile actually drives a browser; hide them for API- or
            // data-only suites that don't configure one.
            if (profile.hasBrowser()) {
                String channel = profile.resolveBrowserChannel();
                String browser = profile.resolveBrowserEngine()
                    + (channel == null ? "" : " (" + channel + ")");
                configuration.addClassifications("Browser", browser);
                configuration.addClassifications("Headless", String.valueOf(profile.isHeadless()));
                configuration.addClassifications("Screenshot mode",
                    profile.resolveScreenshotMode().name());
            }

            Path externalDir = ctx.getEnvironmentConfigDir();
            if (externalDir != null) {
                configuration.addClassifications(
                    "External env-config dir", externalDir.toString());
            }
        } catch (RuntimeException e) {
            log.warn("Could not add run profile classifications to report: {}", e.getMessage());
        }
    }
}
