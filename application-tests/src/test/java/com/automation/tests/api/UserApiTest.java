package com.automation.tests.api;

import com.automation.app.api.UserApiClient;
import com.automation.core.reporting.ExtentReportManager;
import com.automation.core.utils.BaseTest;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * API test examples for User endpoints
 */
public class UserApiTest extends BaseTest {
    
    private UserApiClient userApi;
    private String authToken;
    
    @BeforeClass
    public void setupClass() {
        userApi = new UserApiClient();
        
        // Authenticate and get token for protected endpoints
        authToken = userApi.getAuthToken("admin@example.com", "AdminPassword123");
        if (authToken != null) {
            userApi.withAuthToken(authToken);
        }
    }
    
    @Test(description = "Verify getting all users returns 200 OK")
    public void testGetAllUsers() {
        ExtentReportManager.assignCategory("API", "User", "Smoke");
        ExtentReportManager.assignAuthor("Test Automation Team");
        
        // Send GET request
        Response response = userApi.getAllUsers();
        
        // Verify status code
        Assert.assertEquals(response.getStatusCode(), 200, "Status code should be 200");
        
        // Verify response body is not empty
        Assert.assertNotNull(response.getBody().asString(), "Response body should not be null");
        
        // Verify response is a JSON array
        Assert.assertTrue(response.jsonPath().getList("$").size() >= 0, 
                         "Response should be a JSON array");
        
        ExtentReportManager.logPass("Get all users API test passed");
    }
    
    @Test(description = "Verify creating a new user")
    public void testCreateUser() {
        ExtentReportManager.assignCategory("API", "User", "Functional");
        
        // Prepare test data
        String name = "Test User";
        String email = "testuser_" + System.currentTimeMillis() + "@example.com";
        String role = "user";
        
        // Send POST request
        Response response = userApi.createUser(name, email, role);
        
        // Verify status code
        Assert.assertEquals(response.getStatusCode(), 201, "Status code should be 201 Created");
        
        // Verify response contains user data
        Assert.assertEquals(response.jsonPath().getString("name"), name, "Name should match");
        Assert.assertEquals(response.jsonPath().getString("email"), email, "Email should match");
        Assert.assertEquals(response.jsonPath().getString("role"), role, "Role should match");
        
        // Verify user ID is generated
        int userId = response.jsonPath().getInt("id");
        Assert.assertTrue(userId > 0, "User ID should be generated");
        
        ExtentReportManager.logPass("Create user API test passed");
        
        // Cleanup - delete created user
        userApi.deleteUser(userId);
    }
    
    @Test(description = "Verify getting user by ID")
    public void testGetUserById() {
        ExtentReportManager.assignCategory("API", "User", "Functional");
        
        // First create a user
        String email = "getuser_" + System.currentTimeMillis() + "@example.com";
        Response createResponse = userApi.createUser("Get Test User", email, "user");
        int userId = createResponse.jsonPath().getInt("id");
        
        // Get user by ID
        Response response = userApi.getUserById(userId);
        
        // Verify status code
        Assert.assertEquals(response.getStatusCode(), 200, "Status code should be 200");
        
        // Verify user data
        Assert.assertEquals(response.jsonPath().getInt("id"), userId, "User ID should match");
        Assert.assertEquals(response.jsonPath().getString("email"), email, "Email should match");
        
        ExtentReportManager.logPass("Get user by ID test passed");
        
        // Cleanup
        userApi.deleteUser(userId);
    }
    
    @Test(description = "Verify updating user information")
    public void testUpdateUser() {
        ExtentReportManager.assignCategory("API", "User", "Functional");
        
        // Create a user first
        String originalEmail = "updatetest_" + System.currentTimeMillis() + "@example.com";
        Response createResponse = userApi.createUser("Original Name", originalEmail, "user");
        int userId = createResponse.jsonPath().getInt("id");
        
        // Prepare update data
        Map<String, Object> updateBody = new HashMap<>();
        updateBody.put("name", "Updated Name");
        updateBody.put("role", "admin");
        
        // Update user
        Response response = userApi.partialUpdateUser(userId, updateBody);
        
        // Verify status code
        Assert.assertEquals(response.getStatusCode(), 200, "Status code should be 200");
        
        // Verify updated data
        Assert.assertEquals(response.jsonPath().getString("name"), "Updated Name", 
                          "Name should be updated");
        Assert.assertEquals(response.jsonPath().getString("role"), "admin", 
                          "Role should be updated");
        
        ExtentReportManager.logPass("Update user test passed");
        
        // Cleanup
        userApi.deleteUser(userId);
    }
    
    @Test(description = "Verify deleting a user")
    public void testDeleteUser() {
        ExtentReportManager.assignCategory("API", "User", "Functional");
        
        // Create a user first
        String email = "deletetest_" + System.currentTimeMillis() + "@example.com";
        Response createResponse = userApi.createUser("Delete Test User", email, "user");
        int userId = createResponse.jsonPath().getInt("id");
        
        // Delete user
        Response response = userApi.deleteUser(userId);
        
        // Verify status code
        Assert.assertTrue(response.getStatusCode() == 200 || response.getStatusCode() == 204,
                         "Status code should be 200 or 204");
        
        // Verify user is deleted by trying to get it
        Response getResponse = userApi.getUserById(userId);
        Assert.assertEquals(getResponse.getStatusCode(), 404, 
                          "Getting deleted user should return 404");
        
        ExtentReportManager.logPass("Delete user test passed");
    }
    
    @Test(description = "Verify user search by role")
    public void testSearchUsersByRole() {
        ExtentReportManager.assignCategory("API", "User", "Functional");
        
        String role = "admin";
        
        // Search users by role
        Response response = userApi.searchUsersByRole(role);
        
        // Verify status code
        Assert.assertEquals(response.getStatusCode(), 200, "Status code should be 200");
        
        // Verify all returned users have the specified role
        java.util.List<Map<String, Object>> users = response.jsonPath().getList("$");
        for (Map<String, Object> user : users) {
            Assert.assertEquals(user.get("role"), role, "All users should have role: " + role);
        }
        
        ExtentReportManager.logPass("Search users by role test passed");
    }
    
    @Test(description = "Verify error handling for invalid user ID")
    public void testGetInvalidUserId() {
        ExtentReportManager.assignCategory("API", "User", "Negative");
        
        int invalidUserId = 999999;
        
        // Try to get non-existent user
        Response response = userApi.getUserById(invalidUserId);
        
        // Verify status code
        Assert.assertEquals(response.getStatusCode(), 404, "Status code should be 404 Not Found");
        
        ExtentReportManager.logPass("Invalid user ID handled correctly");
    }
}
