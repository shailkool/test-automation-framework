package com.smbc.raft.tests.bdd.runner;
 
import io.cucumber.testng.AbstractTestNGCucumberTests;
import io.cucumber.testng.CucumberOptions;
import com.smbc.raft.core.bdd.runner.MasterthoughtReportGenerator;

/**
 * TestNG entry point for the Gherkin {@code data_filter} feature.
 *
 * <p>Delegating to {@link AbstractTestNGCucumberTests} lets the existing
 * surefire/TestNG configuration discover and execute Cucumber scenarios
 * alongside the rest of the suite.
 */
@CucumberOptions(
    features = "src/test/resources/features/data_filter.feature",
    glue = {"com.smbc.raft.tests.bdd.steps", "com.smbc.raft.core.bdd.steps"},
    plugin = {
        "pretty",
        "summary",
        "html:test-output/cucumber/data_filter.html",
        "json:test-output/cucumber/data_filter.json"
    },
    monochrome = true
)
public class CsvFilterRunner extends AbstractTestNGCucumberTests {
    static {
        MasterthoughtReportGenerator.registerShutdownHook();
    }
}
