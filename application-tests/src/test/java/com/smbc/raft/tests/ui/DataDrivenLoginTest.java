package com.smbc.raft.tests.ui;

import com.smbc.raft.app.pages.HomePage;
import com.smbc.raft.app.pages.LoginPage;
import com.smbc.raft.core.config.ConfigurationManager;
import com.smbc.raft.core.data.TestDataProvider;
import com.smbc.raft.core.playwright.PlaywrightManager;
import com.smbc.raft.core.reporting.ExtentReportManager;
import com.smbc.raft.core.utils.BaseTest;
import java.util.Map;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** Data-driven login tests using CSV test data */
public class DataDrivenLoginTest extends BaseTest {

  private LoginPage loginPage;
  private HomePage homePage;
  private ConfigurationManager config;

  @BeforeMethod
  public void setup() {
    PlaywrightManager.initializeBrowser();
    loginPage = new LoginPage();
    homePage = new HomePage();
    config = ConfigurationManager.getInstance();

    String appUrl = config.getProperty("app.url", "https://example.com");
    loginPage.navigateToLoginPage(appUrl + "/login");
  }

  @Test(
      dataProvider = "csvDataProvider",
      dataProviderClass = TestDataProvider.class,
      description = "Data-driven login test from CSV")
  public void testLoginWithCSVData(Map<String, String> testData) {
    ExtentReportManager.assignCategory("UI", "Login", "Data-Driven");

    String testCase = testData.get("TestCase");
    String username = testData.get("Username");
    String password = testData.get("Password");
    String expectedResult = testData.get("ExpectedResult");
    String description = testData.get("Description");

    ExtentReportManager.logInfo("Test Case: " + testCase);
    ExtentReportManager.logInfo("Description: " + description);

    // Perform login
    loginPage.login(username, password);

    // Verify result based on expected outcome
    if ("Success".equalsIgnoreCase(expectedResult)) {
      Assert.assertTrue(homePage.isHomePageLoaded(), "Login should succeed for: " + description);
      ExtentReportManager.logPass("Login successful as expected");
    } else {
      Assert.assertTrue(
          loginPage.isErrorMessageDisplayed(),
          "Error message should be displayed for: " + description);
      ExtentReportManager.logPass("Login failed as expected");
    }
  }

  @Test(description = "Login test using specific test case from CSV")
  public void testSpecificLoginScenario() {
    ExtentReportManager.assignCategory("UI", "Login", "Specific");

    // Get specific test case data
    String dataFile = "src/test/resources/testdata/login_testdata.csv";
    Map<String, String> testData = TestDataProvider.getCSVTestCase(dataFile, "TC_LOGIN_001");

    if (testData.isEmpty()) {
      Assert.fail("Test data not found for TC_LOGIN_001");
      return;
    }

    String username = testData.get("Username");
    String password = testData.get("Password");

    ExtentReportManager.logInfo("Testing with: " + testData.get("Description"));

    loginPage.login(username, password);

    Assert.assertTrue(homePage.isHomePageLoaded(), "Login should succeed");
    ExtentReportManager.logPass("Login test passed");
  }
}
