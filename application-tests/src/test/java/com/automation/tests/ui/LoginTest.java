package com.automation.tests.ui;

import com.automation.app.pages.HomePage;
import com.automation.app.pages.LoginPage;
import com.automation.core.config.ConfigurationManager;
import com.automation.core.playwright.PlaywrightManager;
import com.automation.core.reporting.ExtentReportManager;
import com.automation.core.utils.BaseTest;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * UI test examples for Login functionality
 */
public class LoginTest extends BaseTest {
    
    private LoginPage loginPage;
    private HomePage homePage;
    private ConfigurationManager config;
    
    @BeforeMethod
    public void setup() {
        // Initialize browser
        PlaywrightManager.initializeBrowser();
        
        // Initialize page objects
        loginPage = new LoginPage();
        homePage = new HomePage();
        config = ConfigurationManager.getInstance();
        
        // Navigate to application
        String appUrl = config.getProperty("app.url", "https://example.com");
        loginPage.navigateToLoginPage(appUrl + "/login");
    }
    
    @Test(description = "Verify successful login with valid credentials")
    public void testSuccessfulLogin() {
        ExtentReportManager.assignCategory("UI", "Login", "Smoke");
        ExtentReportManager.assignAuthor("Test Automation Team");
        
        // Test data
        String username = "testuser@example.com";
        String password = "TestPassword123";
        
        // Perform login
        loginPage.login(username, password);
        
        // Verify home page is displayed
        Assert.assertTrue(homePage.isHomePageLoaded(), "Home page should be loaded after successful login");
        
        // Verify welcome message
        String welcomeMessage = homePage.getWelcomeMessage();
        Assert.assertNotNull(welcomeMessage, "Welcome message should be displayed");
        
        ExtentReportManager.logPass("Login successful with valid credentials");
    }
    
    @Test(description = "Verify login fails with invalid credentials")
    public void testLoginWithInvalidCredentials() {
        ExtentReportManager.assignCategory("UI", "Login", "Negative");
        
        // Test data
        String username = "invalid@example.com";
        String password = "WrongPassword";
        
        // Attempt login
        loginPage.login(username, password);
        
        // Verify error message is displayed
        Assert.assertTrue(loginPage.isErrorMessageDisplayed(), "Error message should be displayed");
        
        // Verify error message text
        String errorMessage = loginPage.getErrorMessage();
        Assert.assertTrue(errorMessage.contains("Invalid credentials") || 
                         errorMessage.contains("Login failed"),
                         "Error message should indicate invalid credentials");
        
        ExtentReportManager.logPass("Login correctly failed with invalid credentials");
    }
    
    @Test(description = "Verify login with remember me functionality")
    public void testLoginWithRememberMe() {
        ExtentReportManager.assignCategory("UI", "Login", "Functional");
        
        // Test data
        String username = "testuser@example.com";
        String password = "TestPassword123";
        
        // Perform login with remember me
        loginPage.loginWithRememberMe(username, password);
        
        // Verify home page is displayed
        Assert.assertTrue(homePage.isHomePageLoaded(), "Home page should be loaded");
        
        // Logout and verify remember me cookie exists
        homePage.logout();
        
        // Note: Additional verification for remember me cookie would go here
        
        ExtentReportManager.logPass("Login with remember me successful");
    }
    
    @Test(description = "Verify login page elements are displayed")
    public void testLoginPageElements() {
        ExtentReportManager.assignCategory("UI", "Login", "Smoke");
        
        // Verify all login page elements are present
        Assert.assertTrue(loginPage.isLoginPageLoaded(), "All login page elements should be displayed");
        
        ExtentReportManager.logPass("All login page elements are displayed correctly");
    }
}
