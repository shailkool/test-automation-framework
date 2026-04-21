package com.automation.tests.bdd.runner;

import io.cucumber.testng.AbstractTestNGCucumberTests;
import io.cucumber.testng.CucumberOptions;

/**
 * Tag-driven Cucumber/TestNG runner.
 *
 * <p>Discovers every {@code .feature} file under
 * {@code src/test/resources/features} and executes only the scenarios that
 * match the tag expression in {@link CucumberOptions#tags()}. The tag
 * expression defaults to {@code @smoke} but can be overridden per-run via
 * the {@code cucumber.filter.tags} system property, e.g.
 * {@code -Dcucumber.filter.tags="@regression and not @wip"}.
 *
 * <p>Report plugins emit three artefacts on every run:
 * <ul>
 *   <li>{@code test-output/cucumber/cucumber.html} &mdash; Cucumber's native
 *       standalone HTML report.</li>
 *   <li>{@code test-output/cucumber/cucumber.json} &mdash; machine-readable
 *       output for CI dashboards.</li>
 *   <li>{@code test-output/cucumber-extent/SparkReport.html} &mdash; the
 *       themed Extent Spark report configured via {@code extent.properties}
 *       and {@code extent-spark-config.xml}.</li>
 * </ul>
 */
@CucumberOptions(
    features = "src/test/resources/features",
    glue = {"com.automation.tests.bdd.steps"},
    tags = "@smoke",
    plugin = {
        "pretty",
        "summary",
        "html:test-output/cucumber/cucumber.html",
        "json:test-output/cucumber/cucumber.json",
        "com.aventstack.extentreports.cucumber.adapter.ExtentCucumberAdapter:"
    },
    monochrome = true,
    publish = false
)
public class TaggedCucumberRunner extends AbstractTestNGCucumberTests {
}
