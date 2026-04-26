package com.smbc.raft.core.bdd.steps;

import com.smbc.raft.core.data.CentralTestContext;
import com.smbc.raft.core.data.DynamicDataResolver;
import com.smbc.raft.core.diff.DataDiff;
import com.smbc.raft.core.diff.DiffReportGenerator;
import com.smbc.raft.core.diff.DiffResult;
import com.smbc.raft.core.filter.CsvFilterEngine;
import com.smbc.raft.core.filter.FilterRule;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.extern.log4j.Log4j2;
import org.testng.Assert;

/**
 * Glue code for {@code data_filter.feature}.
 *
 * <p>Holds per-scenario state (source rows, rules, output) on instance fields; Cucumber creates a
 * fresh step-definition instance per scenario, so no extra cleanup is required between scenarios.
 */
@Log4j2
public class CsvFilterSteps {

  private static final String DIFF_OUTPUT_DIR = "test-output/cucumber-diff";
  private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

  private Scenario scenario;
  private String sourceName;
  private List<FilterRule> rules = new ArrayList<>();

  @Before
  public void setup(Scenario scenario) {
    this.scenario = scenario;
    CentralTestContext.clear();
    DynamicDataResolver.clear();
  }

  @Given("a source CSV file named {string} with the following data:")
  public void aSourceCsvFileWithData(String name, DataTable table) {
    this.sourceName = name;
    List<Map<String, String>> rows = table.asMaps(String.class, String.class);
    CentralTestContext.setSourceRows(rows);
    log.info("Loaded inline source '{}' with {} rows", name, rows.size());
  }

  @Given("a source CSV file loaded from {string}")
  public void aSourceCsvFileLoadedFrom(String resourcePath) {
    String absolute = resolveResource(resourcePath);
    this.sourceName = Paths.get(absolute).getFileName().toString();
    List<Map<String, String>> rows = new com.smbc.raft.core.data.CSVHandler(absolute).getAllData();
    CentralTestContext.setSourceRows(rows);
    log.info("Loaded external source '{}' with {} rows", absolute, rows.size());
  }

  @And("a configuration feature file defining:")
  public void aConfigurationFeatureFileDefining(DataTable table) {
    List<Map<String, String>> config = table.asMaps(String.class, String.class);
    List<Map<String, String>> resolvedConfig = DynamicDataResolver.resolveTable(config);

    List<FilterRule> parsed = new ArrayList<>();
    for (Map<String, String> row : resolvedConfig) {
      parsed.add(new FilterRule(row.get("column"), row.get("operator"), row.get("value")));
    }
    this.rules = parsed;
    log.info("Loaded {} inline filter rules (dynamic variables resolved)", rules.size());
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
    List<Map<String, String>> filtered = engine.filter(CentralTestContext.getSourceRows());
    CentralTestContext.setFilteredRows(filtered);
    log.info(
        "Filter executed: {} source rows, {} rules, {} output rows",
        CentralTestContext.getSourceRows().size(),
        rules.size(),
        filtered.size());
  }

  @Then("the output should contain {int} records")
  public void theOutputShouldContainRecords(Integer expectedCount) {
    List<Map<String, String>> filtered = CentralTestContext.getFilteredRows();
    Assert.assertNotNull(filtered, "Filter output has not been produced");
    Assert.assertEquals(
        filtered.size(),
        expectedCount.intValue(),
        String.format(
            "Expected %d records after filtering %s, got %d",
            expectedCount, sourceName, filtered.size()));
  }

  /**
   * Compare the filtered output with the expected table using the DataDiff library. The first
   * column of the expected table is treated as the composite key so added/deleted rows are detected
   * as well as per-field modifications. On mismatch an HTML diff report is rendered via {@link
   * DiffReportGenerator}, saved under {@code test-output/cucumber-diff/}, and attached to the
   * running scenario so masterthought embeds it in the HTML report.
   */
  @Then("the resulting data should be:")
  public void theResultingDataShouldBe(DataTable expectedTable) {
    List<Map<String, String>> filtered = CentralTestContext.getFilteredRows();
    Assert.assertNotNull(filtered, "Filter output has not been produced");

    List<Map<String, String>> rawExpected = expectedTable.asMaps(String.class, String.class);
    List<Map<String, String>> expected = DynamicDataResolver.resolveTable(rawExpected);

    if (expected.isEmpty()) {
      Assert.assertEquals(filtered.size(), 0, "Expected an empty result set");
      return;
    }

    String keyField = expected.get(0).keySet().iterator().next();
    DiffResult result =
        DataDiff.builder()
            .keyField(keyField)
            .useLeftSchema(true)
            .build()
            .compare(expected, filtered);

    if (result.isIdentical()) {
      log.info("Data matched expected ({} rows)", result.getMatchedRows());
      return;
    }

    String title = currentScenarioName();
    String oldLabel = "expected (feature datatable)";
    String newLabel = sourceName == null ? "actual (filter output)" : "actual (" + sourceName + ")";

    DiffReportGenerator generator = new DiffReportGenerator();
    String html = generator.generateHtml(result, title, oldLabel, newLabel);

    String timestamp = LocalDateTime.now().format(TIMESTAMP);
    Path htmlPath = Paths.get(DIFF_OUTPUT_DIR, sanitize(title) + "_" + timestamp + ".html");
    generator.saveReport(result, title, oldLabel, newLabel, htmlPath.toString());

    attachDiff(html, title);

    String message =
        String.format(
            "\n"
                + "--------------------------------------------------------------------------------\n"
                + "🛑 DATA MISMATCH DETECTED\n"
                + "--------------------------------------------------------------------------------\n"
                + "Scenario : %s\n"
                + "Outcome  : %s\n"
                + "Report   : %s\n"
                + "--------------------------------------------------------------------------------",
            currentScenarioName(),
            result.getDataDiff().getSummary().toBusinessString(),
            htmlPath.toAbsolutePath());

    throw new com.smbc.raft.core.exception.DataValidationException(message);
  }

  private void attachDiff(String html, String title) {
    if (scenario == null) {
      return;
    }
    scenario.attach(html.getBytes(StandardCharsets.UTF_8), "text/html", "Data diff: " + title);
    scenario.log("Data mismatch detected - see attached HTML diff '" + title + "'.");
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
   * Resolve a classpath-relative test resource to an absolute filesystem path. Uses {@link
   * URL#toURI()} rather than {@link URL#getPath()} so Windows drive-letter URLs like {@code
   * /C:/...} survive the conversion. Falls back to {@code src/test/resources} for IDE runs where
   * the resource hasn't been copied to {@code target/test-classes} yet.
   */
  private String resolveResource(String relativePath) {
    // Support base folder if set by other step classes
    String base = CentralTestContext.getBaseFolder();
    if (base != null) {
      Path fullPath = Paths.get(base, relativePath);
      if (fullPath.toFile().exists()) {
        return fullPath.toString();
      }
    }

    URL resource = Thread.currentThread().getContextClassLoader().getResource(relativePath);
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
