@uk_trade
Feature: Relational Data Joining
  As a data analyst
  I want to join multiple data tables with filters
  So that I can perform complex cross-file validations without loading millions of rows

  Background:
    Given the test data is related to "Trade Output"

  @complex_join
  Scenario: Join Trade Data and Lifecycle Events with Filters
    Given I load "UK/test_to_trade_data_mapping.csv" as "TradeMapping"
    And I load "UK/repo_trade_data.csv" as "TradeTable"
    
    When I join "TradeMapping" and "TradeTable" using "Inner Join" with the following rules:
      | Type   | Column          | Operator | Value/Target Column |
      | JOIN   | UTI             | ==       | UTI                 |

    Then the resulting data should be:
      | Test Case ID | Scenario                                  | Settlement Status | Collateral ISIN |  Principal Amount | Trade Type | Side     | Maturity Date | Haircut (%) | Counterparty   | Repo Rate (%) | Settlement Date | Trade Date |
      | TC-REPO-001  | Standard Overnight Repo Settlement        | Settled           | US912828L715    |  15,000,000       | Overnight  | Repo     | 24/04/2026    | 2           | Goldman Sachs  | 5.25          | 23/04/2026      | 23/04/2026 |
      | TC-REPO-002  | Term Reverse Repo Interest Accrual        | Pending           | DE0001102333    |  8,250,000        | Term       | Rev Repo | 30/04/2026    | 5           | JP Morgan      | 3.8           | 23/04/2026      | 23/04/2026 |
      | TC-REPO-003  | Repo Rate Modification Lifecycle          | New               | GB00BLH3HT58    |  12,000,000       | Term       | Repo     | 24/05/2026    | 2.5         | Morgan Stanley | 4.95          | 24/04/2026      | 23/04/2026 |
      | TC-REPO-004  | High Haircut Collateral Validation        | New               | FR0014007TY9    |  6,700,000        | Term       | Repo     | 24/05/2026    | 3           | BNP Paribas    | 4.15          | 24/04/2026      | 23/04/2026 |
      | TC-REPO-005  | Cross-Border Settlement Matching          | Pending           | US91282CGM73    |  5,000,000        | Term       | Repo     | 28/04/2026    | 4           | Citi Group     | 5.1           | 25/04/2026      | 23/04/2026 |
      | TC-REPO-006  | Zero Coupon Bond Collateral Pricing       | Pending           | DE0001102333    |  7,500,000        | Term       | Rev Repo | 30/04/2026    | 6           | Deutsche Bank  | 3.9           | 23/04/2026      | 23/04/2026 |
      | TC-REPO-007  | Intra-day Margin Call Execution           | New               | FR0010916929    |  5,500,000        | Term       | Repo     | 24/06/2026    | 4           | Natixis        | 4.25          | 24/04/2026      | 23/04/2026 |
      | TC-REPO-008  | JGB Negative Rate Processing              | Settled           | JP1103521L12    |  25,000,000       | Overnight  | Rev Repo | 24/04/2026    | 2           | Daiwa          | 0.1           | 23/04/2026      | 23/04/2026 |
      | TC-REPO-009  | Multiple Event Audit Trail Reconciliation | New               | JP1103521L12    |  11,000,000       | Term       | Repo     | 24/05/2026    | 3.5         | Mizuho         | 4.85          | 24/04/2026      | 23/04/2026 |
      | TC-REPO-010  | Overnight Auto-Closure Validation         | Settled           | US912828L715    |  10,000,000       | Overnight  | Repo     | 24/04/2026    | 2           | Goldman Sachs  | 5.25          | 23/04/2026      | 23/04/2026 |

  @named_join
  Scenario: Sequential Join into Named Variables
    Given I load "UK/repo_trade_data.csv" as "Trades"
    And I load "UK/repo_trade_event_data.csv" as "Events"
    
    When I join "Trades" and "Events" using "Inner Join" into "JoinedData" with the following rules:
      | Type   | Column | Operator | Value/Target Column |
      | JOIN   | UTI    | ==       | UTI                 |
      | RIGHT  | Action | ==       | Full-Lifecycle      |
      
    Then the table "JoinedData" should be:
      | UTI        | Side     | Action         |
      | UTI8293011 | Repo     | Full-Lifecycle |
      | UTI8293012 | Rev Repo | Full-Lifecycle |
