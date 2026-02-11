package com.automation.tests.validation;

import com.automation.app.api.UserApiClient;
import com.automation.app.database.UserDatabaseHelper;
import com.automation.app.pages.HomePage;
import com.automation.app.pages.LoginPage;
import com.automation.core.config.ConfigurationManager;
import com.automation.core.playwright.PlaywrightManager;
import com.automation.core.reporting.ExtentReportManager;
import com.automation.core.retry.*;
import com.automation.core.utils.BaseTest;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Map;

/**
 * Examples demonstrating ValidationLoop for polling and retry logic
 */
public class ValidationLoopExamplesTest extends BaseTest {
    
    @Test(description = "UI validation with short loop - wait for element to appear")
    public void testUIValidationShortLoop() {
        ExtentReportManager.assignCategory("Validation", "UI", "Short Loop");
        
        PlaywrightManager.initializeBrowser();
        LoginPage loginPage = new LoginPage();
        HomePage homePage = new HomePage();
        
        ConfigurationManager config = ConfigurationManager.getInstance();
        String appUrl = config.getProperty("app.url", "https://example.com");
        
        loginPage.navigateToLoginPage(appUrl);
        loginPage.login("user@example.com", "password");
        
        // Validate that home page loads within 30 seconds (15 iterations x 2 sec)
        ValidationLoop loop = ValidationLoop.shortLoop();
        
        ValidationResult<Boolean> result = loop.validateTrue(() -> {
            return homePage.isHomePageLoaded();
        });
        
        Assert.assertTrue(result.isSuccess(), "Home page should load");
        ExtentReportManager.logInfo("Home page loaded after " + result.getIterations() + " iterations");
        
        PlaywrightManager.closeBrowser();
    }
    
    @Test(description = "API validation with once - single check")
    public void testAPIValidationOnce() {
        ExtentReportManager.assignCategory("Validation", "API", "Once");
        
        UserApiClient userApi = new UserApiClient();
        
        // Create a user
        Response createResponse = userApi.createUser("Test User", 
                "testuser@example.com", "user");
        int userId = createResponse.jsonPath().getInt("id");
        
        // Validate user was created (check once)
        ValidationLoop loop = ValidationLoop.once();
        
        ValidationResult<Response> result = loop.validate(
            () -> userApi.getUserById(userId),
            (actual, expected) -> actual.getStatusCode() == 200,
            null
        );
        
        Assert.assertTrue(result.isSuccess(), "User should be retrievable");
        
        // Cleanup
        userApi.deleteUser(userId);
    }
    
    @Test(description = "Database validation with long loop - wait for data processing")
    public void testDatabaseValidationLongLoop() {
        ExtentReportManager.assignCategory("Validation", "Database", "Long Loop");
        
        UserDatabaseHelper userDb = new UserDatabaseHelper();
        
        // Simulate async data processing - create user
        String email = "asyncuser_" + System.currentTimeMillis() + "@example.com";
        userDb.createUser("Async User", email, "password", "user");
        
        // Wait up to 5 minutes for user to become active (simulating async processing)
        ValidationLoop loop = ValidationLoop.longLoop();
        
        ValidationResult<Map<String, Object>> result = loop.validate(
            () -> userDb.getUserByEmail(email),
            (actual, expected) -> {
                if (actual == null) return false;
                // In real scenario, check if status is "active"
                return actual.get("EMAIL").equals(email);
            },
            null
        );
        
        Assert.assertTrue(result.isSuccess(), "User should be processed");
        ExtentReportManager.logInfo("User processed after " + 
            String.format("%.1f", result.getDurationSeconds()) + " seconds");
        
        // Cleanup
        userDb.deleteUserByEmail(email);
    }
    
    @Test(description = "Custom validation loop with specific parameters")
    public void testCustomValidationLoop() {
        ExtentReportManager.assignCategory("Validation", "Custom", "API");
        
        UserApiClient userApi = new UserApiClient();
        
        // Create custom loop: 10 iterations, 1 second interval
        ValidationLoop loop = ValidationLoop.custom(10, 1000);
        
        ValidationResult<Response> result = loop.validateCondition(
            () -> userApi.getAllUsers(),
            response -> response.getStatusCode() == 200
        );
        
        Assert.assertTrue(result.isSuccess());
        ExtentReportManager.logPass("Custom validation completed in " + 
            result.getDurationMillis() + " ms");
    }
    
    @Test(description = "Validate API response contains expected data")
    public void testValidateResponseContains() {
        ExtentReportManager.assignCategory("Validation", "API", "Contains");
        
        UserApiClient userApi = new UserApiClient();
        
        // Create user with specific name
        String uniqueName = "SearchUser_" + System.currentTimeMillis();
        Response createResponse = userApi.createUser(uniqueName, 
                "searchuser@example.com", "user");
        int userId = createResponse.jsonPath().getInt("id");
        
        // Validate response contains the name (with retry)
        ValidationLoop loop = ValidationLoop.shortLoop();
        
        ValidationResult<String> result = loop.validateContains(
            () -> {
                Response response = userApi.getUserById(userId);
                return response.jsonPath().getString("name");
            },
            uniqueName
        );
        
        Assert.assertTrue(result.isSuccess());
        ExtentReportManager.logPass("User name found in response");
        
        // Cleanup
        userApi.deleteUser(userId);
    }
    
    @Test(description = "Validate database record count with polling")
    public void testValidateDatabaseCount() {
        ExtentReportManager.assignCategory("Validation", "Database", "Count");
        
        UserDatabaseHelper userDb = new UserDatabaseHelper();
        
        int initialCount = userDb.getUserCount();
        
        // Create new user
        String email = "counttest_" + System.currentTimeMillis() + "@example.com";
        userDb.createUser("Count Test", email, "password", "user");
        
        // Validate count increased (with retry for async scenarios)
        ValidationLoop loop = ValidationLoop.shortLoop();
        
        ValidationResult<Integer> result = loop.validateEquals(
            () -> userDb.getUserCount(),
            initialCount + 1
        );
        
        Assert.assertTrue(result.isSuccess(), "User count should increase");
        
        // Cleanup
        userDb.deleteUserByEmail(email);
    }
    
    @Test(description = "Validate with multiple conditions")
    public void testMultipleConditions() {
        ExtentReportManager.assignCategory("Validation", "API", "Multiple");
        
        UserApiClient userApi = new UserApiClient();
        
        String email = "multitest_" + System.currentTimeMillis() + "@example.com";
        Response createResponse = userApi.createUser("Multi Test", email, "admin");
        int userId = createResponse.jsonPath().getInt("id");
        
        // Validate multiple conditions
        ValidationLoop loop = ValidationLoop.shortLoop();
        
        ValidationResult<Response> result = loop.validateCondition(
            () -> userApi.getUserById(userId),
            response -> {
                // Check multiple conditions
                boolean statusOk = response.getStatusCode() == 200;
                boolean emailMatches = response.jsonPath().getString("email").equals(email);
                boolean roleMatches = response.jsonPath().getString("role").equals("admin");
                
                return statusOk && emailMatches && roleMatches;
            }
        );
        
        Assert.assertTrue(result.isSuccess());
        ExtentReportManager.logPass("All conditions validated successfully");
        
        // Cleanup
        userApi.deleteUser(userId);
    }
    
    @Test(description = "Handle validation failure gracefully")
    public void testValidationFailure() {
        ExtentReportManager.assignCategory("Validation", "Failure", "Handling");
        
        UserApiClient userApi = new UserApiClient();
        
        // Create loop that won't throw exception on failure
        LoopConfig config = LoopConfig.custom(3, 500);
        config.setThrowOnFailure(false);
        
        ValidationLoop loop = new ValidationLoop(config);
        
        // Try to get non-existent user
        ValidationResult<Response> result = loop.validateEquals(
            () -> userApi.getUserById(999999),
            null
        );
        
        Assert.assertFalse(result.isSuccess(), "Validation should fail");
        Assert.assertEquals(result.getIterations(), 3, "Should retry 3 times");
        
        ExtentReportManager.logInfo("Validation failed as expected after " + 
            result.getIterations() + " attempts");
    }
    
    @Test(description = "End-to-end validation: API -> Database -> UI")
    public void testEndToEndValidation() {
        ExtentReportManager.assignCategory("Validation", "E2E", "Full Flow");
        
        UserApiClient userApi = new UserApiClient();
        UserDatabaseHelper userDb = new UserDatabaseHelper();
        
        String email = "e2etest_" + System.currentTimeMillis() + "@example.com";
        
        // Step 1: Create user via API
        Response createResponse = userApi.createUser("E2E User", email, "user");
        int userId = createResponse.jsonPath().getInt("id");
        
        // Step 2: Validate in database (with retry for async propagation)
        ValidationLoop dbLoop = ValidationLoop.shortLoop();
        ValidationResult<Boolean> dbResult = dbLoop.validateTrue(
            () -> userDb.emailExists(email)
        );
        
        Assert.assertTrue(dbResult.isSuccess(), "User should exist in database");
        ExtentReportManager.logPass("Step 2: Database validation passed");
        
        // Step 3: Validate via API again
        ValidationLoop apiLoop = ValidationLoop.shortLoop();
        ValidationResult<Response> apiResult = apiLoop.validateCondition(
            () -> userApi.getUserById(userId),
            response -> response.getStatusCode() == 200 && 
                       response.jsonPath().getString("email").equals(email)
        );
        
        Assert.assertTrue(apiResult.isSuccess(), "User should be retrievable via API");
        ExtentReportManager.logPass("Step 3: API validation passed");
        
        ExtentReportManager.logPass("End-to-end validation completed successfully");
        
        // Cleanup
        userApi.deleteUser(userId);
    }
}
