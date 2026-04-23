@uk_trade
Feature: UK Repo Trade Data Validation
  As a trade analyst
  I want to filter and validate repo trade data and events
  So that I can verify the lifecycle of specific trades identified by test cases

  @trade_filter
  Scenario Outline: Validate trade and lifecycle events for a specific test case
    Given the test data is related to "Trade Output"
    And I read the trade mappings from "UK/test_to_trade_data_mapping.csv"
    
    # --- Trade Data Validation ---
    When the csv file is "UK/repo_trade_data.csv"
    And the following csv filter is applied
      | column          | operator | mapping_field                   |
      | Front Office ID | ==       | ${<TestCaseID>.Front Office ID} |
    Then the resulting data should be:
      | Front Office ID                 | Collateral ISIN | Collateral Description |
      | ${<TestCaseID>.Front Office ID} | <ISIN>          | <Description>          |

    Examples:
      | TestCaseID  | ISIN         | Description   |
      | TC-REPO-001 | US912828L715 | UST 2.0% 2030 |
      | TC-REPO-002 | US912810SN37 | UST 4.5% 2026 |

  @trade_filter_2
  Scenario Outline: Validate trade lifecycle events for a specific test case
    Given the test data is related to "Trade Output"
    And I read the trade mappings from "UK/test_to_trade_data_mapping.csv"

    # --- Trade Lifecycle Validation ---
    When the csv file is "UK/repo_trade_event_data.csv"
    And the following csv filter is applied
      | column | operator | mapping_field       |
      | UTI    | ==       | ${<TestCaseID>.UTI} |
      | Action | !=       | Termination         |
    Then the resulting data should be:
       | UTI                 | Event Type | Event Timestamp  | Event Description                | Action         | Status    |
       | ${<TestCaseID>.UTI} | NEWT       | 23/04/2026 09:00 | New Trade - Initial Execution    | Full-Lifecycle | Confirmed |
       | ${<TestCaseID>.UTI} | MODI       | 23/04/2026 11:00 | Trade Modification - Rate Change | Correction     | Confirmed |
#       | ${<TestCaseID>.UTI} | MATU       | 24/04/2026 09:00 | Maturity - Final Repayment       | Termination    | Settled   |
       
    Examples:
      | TestCaseID  |
      | TC-REPO-002 |

