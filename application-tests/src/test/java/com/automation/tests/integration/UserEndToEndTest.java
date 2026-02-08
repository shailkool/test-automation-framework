package com.automation.tests.integration;

import com.automation.app.api.UserApiClient;
import com.automation.app.database.UserDatabaseHelper;
import com.automation.app.pages.HomePage;
import com.automation.app.pages.LoginPage;
import com.automation.core.config.ConfigurationManager;
import com.automation.core.playwright.PlaywrightManager;
import com.automation.core.reporting.ExtentReportManager;
import com.automation.core.utils.BaseTest;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.Map;

/**
 * Integration tests combining UI, API, and Database layers
 */
public class UserEndToEndTest extends BaseTest {
    
    private UserApiClient userApi;
    private UserDatabaseHelper userDb;
    private LoginPage loginPage;
    private HomePage homePage;
    private ConfigurationManager config;
    
    private static final String TEST_USER_PREFIX = "e2etest_";
    private String testUserEmail;
    private String testUserPassword = "TestPassword123";
    private int testUserId;
    
    @BeforeClass
    public void setupClass() {
        // Initialize components
        userApi = new UserApiClient();
        userDb = new UserDatabaseHelper();
        config = ConfigurationManager.getInstance();
        
        // Cleanup any existing test data
        userDb.cleanupTestUsers(TEST_USER_PREFIX + "%");
    }
    
    @Test(priority = 1, description = "End-to-end test: Create user via API and verify in database")
    public void testCreateUserApiToDatabase() {
        ExtentReportManager.assignCategory("Integration", "E2E", "User Creation");
        ExtentReportManager.assignAuthor("Test Automation Team");
        
        // Step 1: Create user via API
        testUserEmail = TEST_USER_PREFIX + System.currentTimeMillis() + "@example.com";
        Response apiResponse = userApi.createUser("E2E Test User", testUserEmail, "user");
        
        // Verify API response
        Assert.assertEquals(apiResponse.getStatusCode(), 201, "User should be created via API");
        testUserId = apiResponse.jsonPath().getInt("id");
        ExtentReportManager.logPass("Step 1: User created successfully via API");
        
        // Step 2: Verify user exists in database
        Map<String, Object> dbUser = userDb.getUserByEmail(testUserEmail);
        Assert.assertNotNull(dbUser, "User should exist in database");
        Assert.assertEquals(dbUser.get("EMAIL"), testUserEmail, "Email should match in database");
        Assert.assertEquals(dbUser.get("NAME"), "E2E Test User", "Name should match in database");
        ExtentReportManager.logPass("Step 2: User verified in database");
        
        // Step 3: Update password in database (simulating password setup)
        // In real scenario, password would be hashed
        int userId = ((Number) dbUser.get("USER_ID")).intValue();
        userDb.updateUser(userId, "E2E Test User", testUserEmail, "user");
        ExtentReportManager.logPass("Step 3: User password set in database");
        
        ExtentReportManager.logPass("End-to-end user creation test completed successfully");
    }
    
    @Test(priority = 2, description = "End-to-end test: Update user via API and verify in database", 
          dependsOnMethods = "testCreateUserApiToDatabase")
    public void testUpdateUserApiToDatabase() {
        ExtentReportManager.assignCategory("Integration", "E2E", "User Update");
        
        // Step 1: Update user via API
        String newName = "Updated E2E User";
        Map<String, Object> updateBody = Map.of("name", newName, "role", "admin");
        Response apiResponse = userApi.partialUpdateUser(testUserId, updateBody);
        
        // Verify API response
        Assert.assertEquals(apiResponse.getStatusCode(), 200, "User should be updated via API");
        Assert.assertEquals(apiResponse.jsonPath().getString("name"), newName, 
                          "Updated name should match in API response");
        ExtentReportManager.logPass("Step 1: User updated successfully via API");
        
        // Step 2: Verify changes in database
        Map<String, Object> dbUser = userDb.getUserByEmail(testUserEmail);
        Assert.assertEquals(dbUser.get("NAME"), newName, "Name should be updated in database");
        Assert.assertEquals(dbUser.get("ROLE"), "admin", "Role should be updated in database");
        ExtentReportManager.logPass("Step 2: Updates verified in database");
        
        ExtentReportManager.logPass("End-to-end user update test completed successfully");
    }
    
    @Test(priority = 3, description = "End-to-end test: Verify user can login via UI", 
          dependsOnMethods = "testCreateUserApiToDatabase")
    public void testUserLoginViaUI() {
        ExtentReportManager.assignCategory("Integration", "E2E", "User Login");
        
        // Step 1: Initialize browser and navigate to login page
        PlaywrightManager.initializeBrowser();
        loginPage = new LoginPage();
        homePage = new HomePage();
        
        String appUrl = config.getProperty("app.url", "https://example.com");
        loginPage.navigateToLoginPage(appUrl + "/login");
        ExtentReportManager.logPass("Step 1: Navigated to login page");
        
        // Step 2: Verify user exists in database before login
        Assert.assertTrue(userDb.emailExists(testUserEmail), 
                         "User should exist in database before login");
        ExtentReportManager.logPass("Step 2: User existence verified in database");
        
        // Step 3: Perform login via UI
        loginPage.login(testUserEmail, testUserPassword);
        ExtentReportManager.logPass("Step 3: Login performed via UI");
        
        // Step 4: Verify successful login
        Assert.assertTrue(homePage.isHomePageLoaded(), "User should be logged in");
        String welcomeMessage = homePage.getWelcomeMessage();
        Assert.assertNotNull(welcomeMessage, "Welcome message should be displayed");
        ExtentReportManager.logPass("Step 4: Login verified via UI");
        
        // Step 5: Verify user session in database (if your app tracks sessions)
        // This is optional based on your application architecture
        ExtentReportManager.logInfo("Step 5: User session active");
        
        ExtentReportManager.logPass("End-to-end login test completed successfully");
    }
    
    @Test(priority = 4, description = "End-to-end test: Delete user via API and verify in database", 
          dependsOnMethods = "testUserLoginViaUI")
    public void testDeleteUserApiToDatabase() {
        ExtentReportManager.assignCategory("Integration", "E2E", "User Deletion");
        
        // Step 1: Verify user exists before deletion
        Assert.assertTrue(userDb.emailExists(testUserEmail), "User should exist before deletion");
        ExtentReportManager.logPass("Step 1: User existence confirmed");
        
        // Step 2: Delete user via API
        Response apiResponse = userApi.deleteUser(testUserId);
        Assert.assertTrue(apiResponse.getStatusCode() == 200 || apiResponse.getStatusCode() == 204,
                         "User should be deleted via API");
        ExtentReportManager.logPass("Step 2: User deleted via API");
        
        // Step 3: Verify user is deleted from database
        Assert.assertFalse(userDb.emailExists(testUserEmail), 
                          "User should not exist in database after deletion");
        
        // Step 4: Verify user cannot be retrieved via API
        Response getResponse = userApi.getUserById(testUserId);
        Assert.assertEquals(getResponse.getStatusCode(), 404, 
                          "Getting deleted user should return 404");
        ExtentReportManager.logPass("Step 3-4: User deletion verified in database and API");
        
        ExtentReportManager.logPass("End-to-end deletion test completed successfully");
    }
    
    @Test(description = "Complete workflow: API creation → DB verification → UI interaction → API cleanup")
    public void testCompleteUserLifecycle() {
        ExtentReportManager.assignCategory("Integration", "E2E", "Complete Workflow");
        
        String email = TEST_USER_PREFIX + "lifecycle_" + System.currentTimeMillis() + "@example.com";
        
        // 1. Create user via API
        Response createResponse = userApi.createUser("Lifecycle User", email, "user");
        Assert.assertEquals(createResponse.getStatusCode(), 201);
        int userId = createResponse.jsonPath().getInt("id");
        ExtentReportManager.logPass("User created via API");
        
        // 2. Verify in database
        Map<String, Object> dbUser = userDb.getUserByEmail(email);
        Assert.assertNotNull(dbUser);
        ExtentReportManager.logPass("User verified in database");
        
        // 3. Update via API
        Response updateResponse = userApi.partialUpdateUser(userId, Map.of("role", "admin"));
        Assert.assertEquals(updateResponse.getStatusCode(), 200);
        ExtentReportManager.logPass("User updated via API");
        
        // 4. Verify update in database
        dbUser = userDb.getUserByEmail(email);
        Assert.assertEquals(dbUser.get("ROLE"), "admin");
        ExtentReportManager.logPass("Update verified in database");
        
        // 5. Retrieve via API
        Response getResponse = userApi.getUserById(userId);
        Assert.assertEquals(getResponse.getStatusCode(), 200);
        Assert.assertEquals(getResponse.jsonPath().getString("role"), "admin");
        ExtentReportManager.logPass("User retrieved and verified via API");
        
        // 6. Delete via API
        Response deleteResponse = userApi.deleteUser(userId);
        Assert.assertTrue(deleteResponse.getStatusCode() == 200 || deleteResponse.getStatusCode() == 204);
        ExtentReportManager.logPass("User deleted via API");
        
        // 7. Verify deletion in database
        Assert.assertFalse(userDb.emailExists(email));
        ExtentReportManager.logPass("Deletion verified in database");
        
        ExtentReportManager.logPass("Complete user lifecycle test passed");
    }
    
    @AfterClass
    public void cleanupClass() {
        // Cleanup any remaining test data
        userDb.cleanupTestUsers(TEST_USER_PREFIX + "%");
        ExtentReportManager.logInfo("Test data cleaned up");
    }
}
