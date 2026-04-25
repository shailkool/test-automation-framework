package com.smbc.raft.tests.retry;

import com.smbc.raft.app.api.UserApiClient;
import com.smbc.raft.app.database.UserDatabaseHelper;
import com.smbc.raft.app.pages.HomePage;
import com.smbc.raft.core.config.ConfigurationManager;
import com.smbc.raft.core.playwright.PlaywrightManager;
import com.smbc.raft.core.reporting.ExtentReportManager;
import com.smbc.raft.core.retry.LoopType;
import com.smbc.raft.core.retry.RetryValidator;
import com.smbc.raft.core.retry.ValidationResult;
import com.smbc.raft.core.utils.BaseTest;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Examples demonstrating retry/loop validation mechanism
 */
public class RetryValidationTest extends BaseTest {
    
    @Test(description = "Validate API response with SHORT loop")
    public void testApiValidationWithShortLoop() {
        ExtentReportManager.assignCategory("Retry", "API", "Short Loop");
        
        UserApiClient userApi = new UserApiClient();
        
        // Create user
        String email = "retry_test_" + System.currentTimeMillis() + "@example.com";
        Response createResponse = userApi.createUser("Retry Test User", email, "user");
        int userId = createResponse.jsonPath().getInt("id");
        
        // Validate user can be retrieved with retry (in case of async processing)
        ValidationResult<Integer> result = RetryValidator.validateWithRetry(
            200,  // Expected status code
            () -> userApi.getUserById(userId).getStatusCode(),  // Actual supplier
            LoopType.SHORT,  // 15 iterations, 2 seconds interval
            "User retrieval API status"
        );
        
        // Assert validation passed
        Assert.assertTrue(result.isMatched(), 
            "API should return 200 status code: " + result.getSummary());
        
        ExtentReportManager.logInfo("Validation result: " + result.getSummary());
        
        // Cleanup
        userApi.deleteUser(userId);
    }
    
    @Test(description = "Validate database record with LONG loop")
    public void testDatabaseValidationWithLongLoop() {
        ExtentReportManager.assignCategory("Retry", "Database", "Long Loop");
        
        UserDatabaseHelper userDb = new UserDatabaseHelper();
        UserApiClient userApi = new UserApiClient();
        
        // Create user via API
        String email = "db_retry_" + System.currentTimeMillis() + "@example.com";
        Response response = userApi.createUser("DB Retry Test", email, "user");
        int userId = response.jsonPath().getInt("id");
        
        // Wait for database sync with LONG loop (60 iterations, 5 seconds each)
        boolean recordExists = RetryValidator.waitUntilRecordExists(
            () -> userDb.emailExists(email),
            LoopType.LONG,
            "users"
        );
        
        Assert.assertTrue(recordExists, "User should exist in database after API creation");
        
        ExtentReportManager.logPass("Database record validated successfully");
        
        // Cleanup
        userApi.deleteUser(userId);
        userDb.deleteUserByEmail(email);
    }
    
    @Test(description = "Validate UI element with ONCE (no retry)")
    public void testUiValidationOnce() {
        ExtentReportManager.assignCategory("Retry", "UI", "Once");
        
        PlaywrightManager.initializeBrowser();
        HomePage homePage = new HomePage();
        ConfigurationManager config = ConfigurationManager.getInstance();
        
        String appUrl = config.getProperty("app.url", "https://example.com");
        homePage.navigateTo(appUrl);
        
        // Validate page title with ONCE (single check, no retry)
        ValidationResult<String> result = RetryValidator.validateWithRetry(
            "Example Domain",  // Expected title
            () -> homePage.getTitle(),  // Actual supplier
            LoopType.ONCE,  // Only check once
            "Page title validation"
        );
        
        Assert.assertTrue(result.isMatched(), 
            "Page title should match: " + result.getSummary());
        
        PlaywrightManager.closeBrowser();
    }
    
    @Test(description = "Validate with custom loop parameters")
    public void testCustomLoopValidation() {
        ExtentReportManager.assignCategory("Retry", "API", "Custom Loop");
        
        UserApiClient userApi = new UserApiClient();
        
        // Custom loop: 10 iterations, 1 second interval
        ValidationResult<Integer> result = RetryValidator.validateWithCustomLoop(
            200,
            () -> userApi.getAllUsers().getStatusCode(),
            10,  // 10 iterations
            1000,  // 1 second interval
            "Get all users API"
        );
        
        Assert.assertTrue(result.isMatched());
        ExtentReportManager.logInfo(
            String.format("Validation succeeded in attempt %d/%d (%.2fs)", 
                result.getAttemptNumber(), 
                result.getTotalAttempts(),
                result.getElapsedTimeSeconds()));
    }
    
    @Test(description = "Wait until condition is true")
    public void testWaitUntilCondition() {
        ExtentReportManager.assignCategory("Retry", "Condition", "Wait Until");
        
        // Simulate a condition that becomes true after some time
        final long startTime = System.currentTimeMillis();
        final long delayMillis = 5000; // Condition becomes true after 5 seconds
        
        boolean success = RetryValidator.waitUntil(
            () -> (System.currentTimeMillis() - startTime) >= delayMillis,
            LoopType.SHORT,
            "Waiting for condition to be true"
        );
        
        Assert.assertTrue(success, "Condition should eventually be true");
        ExtentReportManager.logPass("Condition validated successfully");
    }
    
    @Test(description = "Validate string contains with retry")
    public void testStringContainsValidation() {
        ExtentReportManager.assignCategory("Retry", "String", "Contains");
        
        UserApiClient userApi = new UserApiClient();
        
        String email = "string_test_" + System.currentTimeMillis() + "@example.com";
        Response response = userApi.createUser("String Test", email, "user");
        int userId = response.jsonPath().getInt("id");
        
        // Validate response contains email
        ValidationResult<String> result = RetryValidator.validateStringContainsWithRetry(
            email,  // Expected substring
            () -> userApi.getUserById(userId).getBody().asString(),  // Actual string
            LoopType.SHORT,
            "API response contains email"
        );
        
        Assert.assertTrue(result.isMatched(), 
            "Response should contain user email");
        
        userApi.deleteUser(userId);
    }
    
    @Test(description = "Demonstrate failed validation after retries")
    public void testFailedValidationAfterRetries() {
        ExtentReportManager.assignCategory("Retry", "Negative", "Failed Validation");
        
        // This will fail as we're looking for a non-existent value
        ValidationResult<String> result = RetryValidator.validateWithRetry(
            "NonExistentValue",
            () -> "ActualValue",
            LoopType.custom(3, 500),  // 3 attempts, 500ms interval
            "Testing failed validation"
        );
        
        // Verify it failed
        Assert.assertFalse(result.isMatched(), "Validation should fail");
        Assert.assertEquals(result.getAttemptNumber(), 3, 
            "Should have attempted all 3 iterations");
        
        ExtentReportManager.logInfo("Expected failure: " + result.getSummary());
    }
    
    @Test(description = "Wait until API status code is correct")
    public void testWaitForApiStatus() {
        ExtentReportManager.assignCategory("Retry", "API", "Status Wait");
        
        UserApiClient userApi = new UserApiClient();
        
        boolean success = RetryValidator.waitUntilApiStatus(
            200,  // Expected status
            () -> userApi.getAllUsers().getStatusCode(),  // Status supplier
            LoopType.SHORT
        );
        
        Assert.assertTrue(success, "API should return 200 status");
        ExtentReportManager.logPass("API status validated");
    }
}
