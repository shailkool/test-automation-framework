package com.smbc.raft.core.bdd.steps;

import com.smbc.raft.core.data.CentralTestContext;
import com.smbc.raft.core.data.DataTableJoinEngine;
import com.smbc.raft.core.data.JoinCondition;
import com.smbc.raft.core.data.JoinType;
import com.smbc.raft.core.filter.FilterRule;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.extern.log4j.Log4j2;
import org.testng.Assert;

@Log4j2
public class DataTableSteps {

  @Given("I load {string} as {string}")
  public void iLoadAs(String relativePath, String tableName) {
    String baseFolder = CentralTestContext.getBaseFolder();
    String fullPath = Paths.get(baseFolder, relativePath).toString();
    log.info("Loading table '{}' from {}", tableName, fullPath);

    com.smbc.raft.core.data.CSVHandler handler = new com.smbc.raft.core.data.CSVHandler(fullPath);
    CentralTestContext.saveTable(tableName, handler.getAllData());
  }

  @When("I join {string} and {string} using {string} into {string} with the following rules:")
  public void iJoinAndUsingIntoWithTheFollowingRules(
      String leftName,
      String rightName,
      String joinTypeStr,
      String resultName,
      DataTable ruleTable) {
    executeJoin(leftName, rightName, joinTypeStr, resultName, ruleTable);
  }

  @When("I join {string} and {string} using {string} with the following rules:")
  public void iJoinAndUsingWithTheFollowingRules(
      String leftName, String rightName, String joinTypeStr, DataTable ruleTable) {
    executeJoin(leftName, rightName, joinTypeStr, "CurrentJoinResult", ruleTable);
  }

  private void executeJoin(
      String leftName,
      String rightName,
      String joinTypeStr,
      String resultName,
      DataTable ruleTable) {
    List<Map<String, String>> left = CentralTestContext.getTable(leftName);
    List<Map<String, String>> right = CentralTestContext.getTable(rightName);

    Assert.assertNotNull(left, "Table '" + leftName + "' not found in context");
    Assert.assertNotNull(right, "Table '" + rightName + "' not found in context");

    JoinType type = parseJoinType(joinTypeStr);
    List<JoinCondition> joinConditions = new ArrayList<>();
    List<FilterRule> leftFilters = new ArrayList<>();
    List<FilterRule> rightFilters = new ArrayList<>();

    for (Map<String, String> row : ruleTable.asMaps()) {
      String rowType = row.get("Type").toUpperCase();
      String col = row.get("Column");
      String op = row.get("Operator");
      String val = row.get("Value/Target Column");

      switch (rowType) {
        case "JOIN":
          joinConditions.add(new JoinCondition(col, val, op));
          break;
        case "LEFT":
          leftFilters.add(new FilterRule(col, op, val));
          break;
        case "RIGHT":
          rightFilters.add(new FilterRule(col, op, val));
          break;
        default:
          log.warn("Ignoring unknown rule type: {}", rowType);
          break;
      }
    }

    DataTableJoinEngine engine = new DataTableJoinEngine();
    List<Map<String, String>> result =
        engine.join(left, right, type, joinConditions, leftFilters, rightFilters);

    CentralTestContext.saveTable(resultName, result);
    CentralTestContext.setFilteredRows(
        result); // Also set as current result for "Then the resulting data should be:"
    log.info("Join completed. Resulting table '{}' has {} rows", resultName, result.size());
    prettyPrint(result);
  }

  private void prettyPrint(List<Map<String, String>> data) {
    if (data == null || data.isEmpty()) {
      log.info("\n[Table is Empty]");
      return;
    }

    // Convert List<Map> to List<List<String>> for Cucumber DataTable
    List<String> headers = new ArrayList<>(data.get(0).keySet());
    List<List<String>> raw = new ArrayList<>();
    raw.add(headers);

    for (Map<String, String> row : data) {
      List<String> values = new ArrayList<>();
      for (String header : headers) {
        values.add(row.getOrDefault(header, ""));
      }
      raw.add(values);
    }

    DataTable table = DataTable.create(raw);
    log.info("\nResulting Data Table (Gherkin format):\n\n" + table.toString());
  }

  @Then("the table {string} should be:")
  public void theTableShouldBe(String tableName, DataTable expectedTable) {
    List<Map<String, String>> actual = CentralTestContext.getTable(tableName);
    Assert.assertNotNull(actual, "Table '" + tableName + "' not found in context");

    // Reuse the validation logic from CsvFilterSteps
    CentralTestContext.setFilteredRows(actual);
    new CsvFilterSteps().theResultingDataShouldBe(expectedTable);
  }

  private JoinType parseJoinType(String str) {
    String normalized = str.toUpperCase().replace(" ", "");
    if (normalized.contains("INNER")) return JoinType.INNER;
    if (normalized.contains("LEFT")) return JoinType.LEFT;
    if (normalized.contains("RIGHT")) return JoinType.RIGHT;
    if (normalized.contains("FULL")) return JoinType.FULL;
    throw new IllegalArgumentException("Unknown join type: " + str);
  }
}
