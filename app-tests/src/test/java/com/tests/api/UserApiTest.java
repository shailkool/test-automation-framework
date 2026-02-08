package com.tests.api;

import com.app.api.UserApiClient;
import com.framework.base.BaseTest;
import com.framework.reporting.ExtentReportManager;
import com.framework.utils.DataUtils;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * API Tests for User Management
 */
public class UserApiTest extends BaseTest {
    
    private UserApiClient userApiClient;
    private String testEmail;
    
    @BeforeClass
    public void setupClass() {
        userApiClient = new UserApiClient();
        testEmail = DataUtils.generateRandomEmail();
    }
    
    @Test(description = "Verify user creation via API", priority = 1)
    public void testCreateUser() {
        ExtentReportManager.logInfo("Starting user creation API test");
        
        // Create user
        Response response = userApiClient.createUser(
                "Test User",
                testEmail,
                "USER"
        );
        
        ExtentReportManager.logInfo("API request sent to create user");
        
        // Verify status code
        Assert.assertEquals(response.getStatusCode(), 201, "Status code should be 201 Created");
        ExtentReportManager.logPass("Status code validated: 201");
        
        // Verify response body
        String responseEmail = response.jsonPath().getString("email");
        Assert.assertEquals(responseEmail, testEmail, "Email in response should match");
        ExtentReportManager.logPass("User created successfully with email: " + testEmail);
        
        // Verify response contains ID
        int userId = response.jsonPath().getInt("id");
        Assert.assertTrue(userId > 0, "User ID should be greater than 0");
        ExtentReportManager.logPass("User ID generated: " + userId);
    }
    
    @Test(description = "Verify get all users API", priority = 2)
    public void testGetAllUsers() {
        ExtentReportManager.logInfo("Starting get all users API test");
        
        // Get all users
        Response response = userApiClient.getAllUsers();
        
        // Verify status code
        Assert.assertEquals(response.getStatusCode(), 200, "Status code should be 200 OK");
        ExtentReportManager.logPass("Status code validated: 200");
        
        // Verify response is not empty
        int userCount = response.jsonPath().getList("$").size();
        Assert.assertTrue(userCount > 0, "User list should not be empty");
        ExtentReportManager.logPass("Retrieved " + userCount + " users");
    }
    
    @Test(description = "Verify get user by ID API", priority = 3, dependsOnMethods = "testCreateUser")
    public void testGetUserById() {
        ExtentReportManager.logInfo("Starting get user by ID API test");
        
        // First create a user to get the ID
        Response createResponse = userApiClient.createUser(
                "John Doe",
                DataUtils.generateRandomEmail(),
                "ADMIN"
        );
        int userId = createResponse.jsonPath().getInt("id");
        
        // Get user by ID
        Response response = userApiClient.getUserById(userId);
        
        // Verify status code
        Assert.assertEquals(response.getStatusCode(), 200, "Status code should be 200 OK");
        ExtentReportManager.logPass("Status code validated: 200");
        
        // Verify user details
        String name = response.jsonPath().getString("name");
        Assert.assertEquals(name, "John Doe", "Name should match");
        ExtentReportManager.logPass("User details retrieved successfully");
    }
    
    @Test(description = "Verify update user API", priority = 4)
    public void testUpdateUser() {
        ExtentReportManager.logInfo("Starting update user API test");
        
        // Create a user first
        Response createResponse = userApiClient.createUser(
                "Original Name",
                DataUtils.generateRandomEmail(),
                "USER"
        );
        int userId = createResponse.jsonPath().getInt("id");
        
        // Update user
        String updatedEmail = DataUtils.generateRandomEmail();
        Response response = userApiClient.updateUser(userId, "Updated Name", updatedEmail);
        
        // Verify status code
        Assert.assertEquals(response.getStatusCode(), 200, "Status code should be 200 OK");
        ExtentReportManager.logPass("Status code validated: 200");
        
        // Verify updated details
        String updatedName = response.jsonPath().getString("name");
        Assert.assertEquals(updatedName, "Updated Name", "Name should be updated");
        ExtentReportManager.logPass("User updated successfully");
    }
    
    @Test(description = "Verify delete user API", priority = 5)
    public void testDeleteUser() {
        ExtentReportManager.logInfo("Starting delete user API test");
        
        // Create a user first
        Response createResponse = userApiClient.createUser(
                "To Be Deleted",
                DataUtils.generateRandomEmail(),
                "USER"
        );
        int userId = createResponse.jsonPath().getInt("id");
        
        // Delete user
        Response response = userApiClient.deleteUser(userId);
        
        // Verify status code (either 200 or 204 is acceptable)
        int statusCode = response.getStatusCode();
        Assert.assertTrue(statusCode == 200 || statusCode == 204, 
                "Status code should be 200 or 204");
        ExtentReportManager.logPass("User deleted successfully. Status: " + statusCode);
        
        // Verify user no longer exists
        Response getResponse = userApiClient.getUserById(userId);
        Assert.assertEquals(getResponse.getStatusCode(), 404, 
                "Deleted user should return 404");
        ExtentReportManager.logPass("Verified user no longer exists");
    }
    
    @Test(description = "Verify user authentication API", priority = 6)
    public void testUserAuthentication() {
        ExtentReportManager.logInfo("Starting user authentication API test");
        
        // Create a user with known credentials
        String email = DataUtils.generateRandomEmail();
        String password = "testPassword123";
        
        userApiClient.createUser("Auth Test User", email, "USER");
        
        // Authenticate
        Response response = userApiClient.authenticateUser(email, password);
        
        // Verify status code
        Assert.assertEquals(response.getStatusCode(), 200, "Status code should be 200 OK");
        ExtentReportManager.logPass("Authentication successful");
        
        // Verify token is returned
        String token = response.jsonPath().getString("token");
        Assert.assertNotNull(token, "Token should be returned");
        Assert.assertFalse(token.isEmpty(), "Token should not be empty");
        ExtentReportManager.logPass("Authentication token received");
    }
}
