package com.automation.tests.bdd.runner;

import io.cucumber.testng.AbstractTestNGCucumberTests;
import io.cucumber.testng.CucumberOptions;

/**
 * Tag-driven Cucumber/TestNG runner with a masterthought HTML report.
 *
 * <p>Discovers every {@code .feature} file under
 * {@code src/test/resources/features} and executes only the scenarios that
 * match the tag expression in {@link CucumberOptions#tags()}. The default
 * tag expression is {@code @smoke}; override per-run with
 * {@code -Dcucumber.filter.tags="<expression>"}, e.g.
 * {@code -Dcucumber.filter.tags="@regression and not @wip"}.
 *
 * <p>During execution Cucumber emits two primary artefacts:
 * <ul>
 *   <li>{@code test-output/cucumber/cucumber.json} &mdash; the machine-
 *       readable source of truth.</li>
 *   <li>{@code test-output/cucumber/cucumber.html} &mdash; Cucumber's
 *       bundled standalone HTML (kept as a fallback).</li>
 * </ul>
 *
 * <p>After the JVM finishes the test phase,
 * {@link MasterthoughtReportGenerator} runs as a shutdown hook and converts
 * the JSON into the themed masterthought multi-tab report at
 * {@code test-output/cucumber-html-reports/overview-features.html}.
 */
@CucumberOptions(
    features = "src/test/resources/features",
    glue = {"com.automation.tests.bdd.steps"},
    tags = "@smoke",
    plugin = {
        "pretty",
        "summary",
        "html:test-output/cucumber/cucumber.html",
        "json:test-output/cucumber/cucumber.json"
    },
    monochrome = true,
    publish = false
)
public class TaggedCucumberRunner extends AbstractTestNGCucumberTests {

    static {
        MasterthoughtReportGenerator.registerShutdownHook();
    }
}
