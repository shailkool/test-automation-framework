package com.tests.ui;

import com.app.pages.HomePage;
import com.app.pages.LoginPage;
import com.framework.base.BaseTest;
import com.framework.config.ConfigManager;
import com.framework.reporting.ExtentReportManager;
import com.framework.ui.PlaywrightManager;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * UI Tests for Login functionality
 */
public class LoginUITest extends BaseTest {
    
    private LoginPage loginPage;
    private HomePage homePage;
    private ConfigManager config;
    
    @BeforeMethod
    public void setupTest() {
        PlaywrightManager.initializeBrowser();
        loginPage = new LoginPage();
        homePage = new HomePage();
        config = ConfigManager.getInstance();
    }
    
    @Test(description = "Verify successful login with valid credentials")
    public void testSuccessfulLogin() {
        ExtentReportManager.logInfo("Starting successful login test");
        
        // Navigate to login page
        String appUrl = config.getAppUiUrl();
        loginPage.navigateToLoginPage(appUrl);
        ExtentReportManager.logInfo("Navigated to login page");
        
        // Verify login page is loaded
        Assert.assertTrue(loginPage.isLoginPageLoaded(), "Login page should be loaded");
        ExtentReportManager.logPass("Login page loaded successfully");
        
        // Perform login
        loginPage.login("testuser@example.com", "password123");
        ExtentReportManager.logInfo("Entered credentials and clicked login");
        
        // Verify home page is loaded
        Assert.assertTrue(homePage.isHomePageLoaded(), "Home page should be loaded after login");
        ExtentReportManager.logPass("Login successful - Home page loaded");
        
        // Verify username is displayed
        String username = homePage.getLoggedInUsername();
        Assert.assertFalse(username.isEmpty(), "Username should be displayed");
        ExtentReportManager.logPass("User is logged in: " + username);
    }
    
    @Test(description = "Verify login fails with invalid credentials")
    public void testLoginWithInvalidCredentials() {
        ExtentReportManager.logInfo("Starting login with invalid credentials test");
        
        // Navigate to login page
        String appUrl = config.getAppUiUrl();
        loginPage.navigateToLoginPage(appUrl);
        
        // Attempt login with invalid credentials
        loginPage.login("invalid@example.com", "wrongpassword");
        ExtentReportManager.logInfo("Attempted login with invalid credentials");
        
        // Verify error message is displayed
        Assert.assertTrue(loginPage.isErrorMessageDisplayed(), "Error message should be displayed");
        ExtentReportManager.logPass("Error message displayed as expected");
        
        // Verify error message content
        String errorMessage = loginPage.getErrorMessage();
        Assert.assertTrue(errorMessage.contains("Invalid"), "Error message should contain 'Invalid'");
        ExtentReportManager.logPass("Correct error message displayed: " + errorMessage);
    }
    
    @Test(description = "Verify login page elements are present")
    public void testLoginPageElements() {
        ExtentReportManager.logInfo("Starting login page elements test");
        
        // Navigate to login page
        String appUrl = config.getAppUiUrl();
        loginPage.navigateToLoginPage(appUrl);
        
        // Verify all elements are present
        Assert.assertTrue(loginPage.isLoginPageLoaded(), "Login page should be fully loaded");
        ExtentReportManager.logPass("All login page elements are present");
    }
    
    @AfterMethod
    public void teardownTest() {
        PlaywrightManager.cleanup();
    }
}
