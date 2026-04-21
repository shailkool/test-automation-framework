@filter
Feature: Filter CSV Data by Age and Status
  As a data analyst
  I want to filter the customer list based on configuration rules
  So that I only process relevant records

  @smoke @inline
  Scenario: Filter active users over the age of 25 (inline data)
    Given a source CSV file named "customers.csv" with the following data:
      | name    | age | status   |
      | Alice   | 30  | active   |
      | Bob     | 22  | active   |
      | Charlie | 35  | inactive |
      | David   | 28  | active   |
    And a configuration feature file defining:
      | column | operator | value  |
      | age    | >        | 25     |
      | status | ==       | active |
    When the filtering engine is executed
    Then the output should contain 2 records
    And the resulting data should be:
      | name    | age | status   |
      | Alice   | 30  | active   |
      | David   | 28  | active   |

  @regression @external
  Scenario: Filter active users using externally stored CSV files
    Given a source CSV file loaded from "testdata/filter/customers.csv"
    And filter rules loaded from "testdata/filter/filter_rules.csv"
    When the filtering engine is executed
    Then the output should contain 2 records
    And the resulting data should be:
      | name    | age | status   |
      | Alice   | 30  | active   |
      | David   | 28  | active   |

  @regression @operators
  Scenario: Multiple operator types against an externally stored CSV
    Given a source CSV file loaded from "testdata/filter/customers.csv"
    And a configuration feature file defining:
      | column | operator   | value |
      | status | !=         | inactive |
      | name   | startsWith | A        |
    When the filtering engine is executed
    Then the output should contain 1 records
    And the resulting data should be:
      | name  | age | status |
      | Alice | 30  | active |
