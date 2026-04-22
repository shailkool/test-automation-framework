@navigation
Feature: Site navigation journeys per environment
  As a test engineer
  I want to drive a browser against sites configured for the active environment
  So that the same feature file runs unchanged against dev, qa or uat2

  Background:
    Given the active environment configuration is loaded

  @smoke @bbc
  Scenario: Trace a reader's navigation across BBC sections
    Given a browser session for the "BBC" website
    When I open the BBC home page
    And I navigate to the "newsPath" section
    And I navigate to the "sportPath" section
    Then the navigation journey should contain 3 steps
    And every visited page should belong to the "BBC" base URL

  @smoke @yahoo
  Scenario: Trace a guest's navigation across Yahoo sections
    Given a browser session for the "Yahoo" website
    When I open the Yahoo home page
    And I navigate to the "financePath" section
    And I navigate to the "sportsPath" section
    Then the navigation journey should contain 3 steps
    And every visited page should belong to the "Yahoo" base URL

  @regression @multi-site
  Scenario: Mixed journey touches BBC then Yahoo using per-site users
    Given a browser session for the "BBC" website as user "reader"
    When I open the BBC home page
    And I navigate to the "newsPath" section
    And I switch the browser session to the "Yahoo" website as user "guest"
    And I open the Yahoo home page
    And I navigate to the "financePath" section
    Then the navigation journey should contain 4 steps
    And the journey should include a page under the "BBC" base URL
    And the journey should include a page under the "Yahoo" base URL
