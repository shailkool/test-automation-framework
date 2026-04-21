package com.automation.tests.bdd.steps;

import com.automation.core.filter.CsvFilterEngine;
import com.automation.core.filter.FilterRule;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.extern.log4j.Log4j2;
import org.testng.Assert;

import java.net.URL;
import java.net.URISyntaxException;
import java.nio.file.Paths;
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

    private String sourceName;
    private List<Map<String, String>> sourceRows = new ArrayList<>();
    private List<FilterRule> rules = new ArrayList<>();
    private List<Map<String, String>> filtered;

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

    @And("the resulting data should be:")
    public void theResultingDataShouldBe(DataTable expectedTable) {
        List<Map<String, String>> expected = expectedTable.asMaps(String.class, String.class);
        Assert.assertEquals(
            filtered.size(),
            expected.size(),
            "Row count mismatch between expected and actual"
        );
        for (int i = 0; i < expected.size(); i++) {
            Map<String, String> expectedRow = expected.get(i);
            Map<String, String> actualRow = filtered.get(i);
            for (Map.Entry<String, String> e : expectedRow.entrySet()) {
                Assert.assertEquals(
                    actualRow.get(e.getKey()),
                    e.getValue(),
                    String.format(
                        "Row %d column '%s' mismatch", i, e.getKey()
                    )
                );
            }
        }
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
