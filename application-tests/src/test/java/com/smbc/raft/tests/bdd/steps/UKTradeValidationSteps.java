package com.smbc.raft.tests.bdd.steps;

import com.smbc.raft.core.data.CSVHandler;
import com.smbc.raft.core.data.CentralTestContext;
import com.smbc.raft.core.data.DynamicDataResolver;
import com.smbc.raft.core.environment.EnvironmentContext;
import com.smbc.raft.core.filter.CsvFilterEngine;
import com.smbc.raft.core.filter.FilterRule;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.extern.log4j.Log4j2;
import org.testng.Assert;

/**
 * Step definitions for UK Repo Trade Data Validation. Validates trade records and lifecycle events
 * using mapping files.
 */
@Log4j2
public class UKTradeValidationSteps {

  @Given("the test data is related to {string}")
  public void theTestDataIsRelatedTo(String key) {
    String baseFolder = EnvironmentContext.get().getCustomProperties().get(key);
    CentralTestContext.setBaseFolder(baseFolder);
    log.info("Base folder for '{}' resolved to: {}", key, baseFolder);
    Assert.assertNotNull(
        baseFolder, "Base folder property '" + key + "' not found in environment config");
  }

  @And("I read the trade mappings from {string}")
  public void iReadTheTradeMappingsFrom(String relativePath) {
    String fullPath = Paths.get(CentralTestContext.getBaseFolder(), relativePath).toString();
    log.info("Reading mappings from: {}", fullPath);
    CSVHandler handler = new CSVHandler(fullPath);
    List<Map<String, String>> rows = handler.getAllData();

    for (Map<String, String> row : rows) {
      String testCaseId = row.get("Test Case ID");
      if (testCaseId != null) {
        DynamicDataResolver.storeData(testCaseId, row);
      }
    }
    log.info("Loaded {} test case mappings into central resolver", rows.size());
  }

  @When("the csv file is {string}")
  @And("I use the csv file {string}")
  public void theCsvFileIs(String relativePath) {
    String fullPath = Paths.get(CentralTestContext.getBaseFolder(), relativePath).toString();
    log.info("Loading source CSV: {}", fullPath);
    CentralTestContext.setSourceRows(new CSVHandler(fullPath).getAllData());
  }

  @And("the following csv filter is applied")
  public void theFollowingCsvFilterIsApplied(DataTable table) {
    List<Map<String, String>> config = table.asMaps(String.class, String.class);
    List<Map<String, String>> resolvedConfig = DynamicDataResolver.resolveTable(config);

    List<FilterRule> parsed = new ArrayList<>();
    for (Map<String, String> row : resolvedConfig) {
      String columnName = row.get("column");
      String operator = row.get("operator");
      String value = row.get("mapping_field");

      parsed.add(new FilterRule(columnName, operator, value));
      log.debug("Applied rule: {} {} {}", columnName, operator, value);
    }

    CsvFilterEngine engine = new CsvFilterEngine(parsed);
    List<Map<String, String>> filtered = engine.filter(CentralTestContext.getSourceRows());
    CentralTestContext.setFilteredRows(filtered);
  }
}
