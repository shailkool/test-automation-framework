package com.automation.tests.bdd.steps;

import com.automation.core.diff.DataDiff;
import com.automation.core.diff.DiffReportGenerator;
import com.automation.core.diff.DiffResult;
import com.automation.core.filter.CsvFilterEngine;
import com.automation.core.filter.FilterRule;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.extern.log4j.Log4j2;
import org.testng.Assert;

import java.net.URL;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Glue code for {@code data_filter.feature}.
 *
 * <p>Holds per-scenario state (source rows, rules, output) on instance fields;
 * Cucumber creates a fresh step-definition instance per scenario, so no extra
 * cleanup is required between scenarios.
 */
@Log4j2
public class CsvFilterSteps {

    private static final String DIFF_OUTPUT_DIR = "test-output/cucumber-diff";
    private static final DateTimeFormatter TIMESTAMP =
        DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private Scenario scenario;
    private String sourceName;
    private List<Map<String, String>> sourceRows = new ArrayList<>();
    private List<FilterRule> rules = new ArrayList<>();
    private List<Map<String, String>> filtered;

    @Before
    public void captureScenario(Scenario scenario) {
        this.scenario = scenario;
    }

    @Given("a source CSV file named {string} with the following data:")
    public void aSourceCsvFileWithData(String name, DataTable table) {
        this.sourceName = name;
        this.sourceRows = table.asMaps(String.class, String.class);
        log.info("Loaded inline source '{}' with {} rows", name, sourceRows.size());
    }

    @Given("a source CSV file loaded from {string}")
    public void aSourceCsvFileLoadedFrom(String resourcePath) {
        String absolute = resolveResource(resourcePath);
        this.sourceName = Paths.get(absolute).getFileName().toString();
        this.sourceRows = new com.automation.core.data.CSVHandler(absolute).getAllData();
        log.info("Loaded external source '{}' with {} rows", absolute, sourceRows.size());
    }

    @And("a configuration feature file defining:")
    public void aConfigurationFeatureFileDefining(DataTable table) {
        List<Map<String, String>> config = table.asMaps(String.class, String.class);
        List<FilterRule> parsed = new ArrayList<>();
        for (Map<String, String> row : config) {
            parsed.add(new FilterRule(
                row.get("column"),
                row.get("operator"),
                row.get("value")
            ));
        }
        this.rules = parsed;
        log.info("Loaded {} inline filter rules", rules.size());
    }

    @And("filter rules loaded from {string}")
    public void filterRulesLoadedFrom(String resourcePath) {
        String absolute = resolveResource(resourcePath);
        this.rules = CsvFilterEngine.loadRulesFromCsv(absolute);
        log.info("Loaded {} external filter rules from {}", rules.size(), absolute);
    }

    @When("the filtering engine is executed")
    public void theFilteringEngineIsExecuted() {
        CsvFilterEngine engine = new CsvFilterEngine(rules);
        this.filtered = engine.filter(sourceRows);
        log.info("Filter executed: {} source rows, {} rules, {} output rows",
            sourceRows.size(), rules.size(), filtered.size());
    }

    @Then("the output should contain {int} records")
    public void theOutputShouldContainRecords(Integer expectedCount) {
        Assert.assertNotNull(filtered, "Filter output has not been produced");
        Assert.assertEquals(
            filtered.size(),
            expectedCount.intValue(),
            String.format(
                "Expected %d records after filtering %s, got %d",
                expectedCount, sourceName, filtered.size()
            )
        );
    }

    /**
     * Compare the filtered output with the expected table using the DataDiff
     * library. The first column of the expected table is treated as the
     * composite key so added/deleted rows are detected as well as per-field
     * modifications. On mismatch an HTML diff report is rendered via
     * {@link DiffReportGenerator}, saved under
     * {@code test-output/cucumber-diff/}, and attached to the running scenario
     * so masterthought embeds it in the HTML report.
     */
    @And("the resulting data should be:")
    public void theResultingDataShouldBe(DataTable expectedTable) {
        Assert.assertNotNull(filtered, "Filter output has not been produced");

        List<Map<String, String>> expected = expectedTable.asMaps(String.class, String.class);
        if (expected.isEmpty()) {
            Assert.assertEquals(filtered.size(), 0, "Expected an empty result set");
            return;
        }

        String keyField = expected.get(0).keySet().iterator().next();
        DiffResult result = DataDiff.builder()
            .keyField(keyField)
            .build()
            .compare(expected, filtered);

        if (result.isIdentical()) {
            log.info("Data matched expected ({} rows)", result.getMatchedRows());
            return;
        }

        String title = buildReportTitle();
        DiffReportGenerator generator = new DiffReportGenerator();
        String html = generator.generateHtml(result, title);

        Path htmlPath = Paths.get(DIFF_OUTPUT_DIR, sanitize(title) + ".html");
        generator.saveReport(result, title, htmlPath.toString());

        attachDiff(html, title);

        Assert.fail(String.format(
            "Data mismatch in scenario '%s' - %s. HTML diff: %s",
            currentScenarioName(),
            result.getSummary(),
            htmlPath.toAbsolutePath()
        ));
    }

    private void attachDiff(String html, String title) {
        if (scenario == null) {
            return;
        }
        scenario.attach(
            html.getBytes(StandardCharsets.UTF_8),
            "text/html",
            "Data diff: " + title
        );
        scenario.log("Data mismatch detected - see attached HTML diff '" + title + "'.");
    }

    private String buildReportTitle() {
        String base = currentScenarioName();
        String timestamp = LocalDateTime.now().format(TIMESTAMP);
        return base + " [" + timestamp + "]";
    }

    private String currentScenarioName() {
        if (scenario != null && scenario.getName() != null && !scenario.getName().isBlank()) {
            return scenario.getName();
        }
        return sourceName == null ? "data-diff" : sourceName;
    }

    private String sanitize(String value) {
        return value.replaceAll("[^A-Za-z0-9._-]+", "_");
    }

    /**
     * Resolve a classpath-relative test resource to an absolute filesystem
     * path. Uses {@link URL#toURI()} rather than {@link URL#getPath()} so
     * Windows drive-letter URLs like {@code /C:/...} survive the conversion.
     * Falls back to {@code src/test/resources} for IDE runs where the
     * resource hasn't been copied to {@code target/test-classes} yet.
     */
    private String resolveResource(String relativePath) {
        URL resource = Thread.currentThread()
            .getContextClassLoader()
            .getResource(relativePath);
        if (resource != null) {
            try {
                return Paths.get(resource.toURI()).toString();
            } catch (URISyntaxException | IllegalArgumentException e) {
                log.warn("Falling back to source path for '{}': {}", relativePath, e.getMessage());
            }
        }
        return Paths.get("src/test/resources", relativePath).toAbsolutePath().toString();
    }
}
