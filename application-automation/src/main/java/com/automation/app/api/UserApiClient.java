package com.automation.app.api;

import com.automation.core.api.RestAssuredClient;
import com.automation.core.reporting.ExtentReportManager;
import io.restassured.response.Response;
import lombok.extern.log4j.Log4j2;

import java.util.HashMap;
import java.util.Map;

/**
 * API client for User endpoints
 * This is an example - modify according to your application API
 */
@Log4j2
public class UserApiClient {
    
    private RestAssuredClient client;
    private static final String USERS_ENDPOINT = "/api/users";
    
    public UserApiClient() {
        this.client = new RestAssuredClient();
    }
    
    public UserApiClient(String baseUrl) {
        this.client = new RestAssuredClient(baseUrl);
    }
    
    /**
     * Get all users
     */
    public Response getAllUsers() {
        log.info("Getting all users");
        Response response = client.get(USERS_ENDPOINT);
        ExtentReportManager.logInfo("GET all users - Status: " + response.getStatusCode());
        return response;
    }
    
    /**
     * Get user by ID
     */
    public Response getUserById(int userId) {
        log.info("Getting user by ID: {}", userId);
        String endpoint = USERS_ENDPOINT + "/" + userId;
        Response response = client.get(endpoint);
        ExtentReportManager.logInfo("GET user by ID: " + userId + " - Status: " + response.getStatusCode());
        return response;
    }
    
    /**
     * Create new user
     */
    public Response createUser(String name, String email, String role) {
        log.info("Creating new user: {}", email);
        
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("name", name);
        requestBody.put("email", email);
        requestBody.put("role", role);
        
        Response response = client.post(USERS_ENDPOINT, requestBody);
        ExtentReportManager.logInfo("POST create user - Status: " + response.getStatusCode());
        return response;
    }
    
    /**
     * Create user with custom body
     */
    public Response createUser(Map<String, Object> userBody) {
        log.info("Creating new user");
        Response response = client.post(USERS_ENDPOINT, userBody);
        ExtentReportManager.logInfo("POST create user - Status: " + response.getStatusCode());
        return response;
    }
    
    /**
     * Update user
     */
    public Response updateUser(int userId, Map<String, Object> updateBody) {
        log.info("Updating user: {}", userId);
        String endpoint = USERS_ENDPOINT + "/" + userId;
        Response response = client.put(endpoint, updateBody);
        ExtentReportManager.logInfo("PUT update user - Status: " + response.getStatusCode());
        return response;
    }
    
    /**
     * Partial update user
     */
    public Response partialUpdateUser(int userId, Map<String, Object> updateBody) {
        log.info("Partially updating user: {}", userId);
        String endpoint = USERS_ENDPOINT + "/" + userId;
        Response response = client.patch(endpoint, updateBody);
        ExtentReportManager.logInfo("PATCH update user - Status: " + response.getStatusCode());
        return response;
    }
    
    /**
     * Delete user
     */
    public Response deleteUser(int userId) {
        log.info("Deleting user: {}", userId);
        String endpoint = USERS_ENDPOINT + "/" + userId;
        Response response = client.delete(endpoint);
        ExtentReportManager.logInfo("DELETE user - Status: " + response.getStatusCode());
        return response;
    }
    
    /**
     * Search users by role
     */
    public Response searchUsersByRole(String role) {
        log.info("Searching users by role: {}", role);
        Response response = client.setQueryParam("role", role).get(USERS_ENDPOINT);
        ExtentReportManager.logInfo("GET users by role - Status: " + response.getStatusCode());
        return response;
    }
    
    /**
     * Authenticate user
     */
    public Response authenticateUser(String email, String password) {
        log.info("Authenticating user: {}", email);
        
        Map<String, Object> credentials = new HashMap<>();
        credentials.put("email", email);
        credentials.put("password", password);
        
        Response response = client.post("/api/auth/login", credentials);
        ExtentReportManager.logInfo("POST authenticate user - Status: " + response.getStatusCode());
        return response;
    }
    
    /**
     * Get authentication token
     */
    public String getAuthToken(String email, String password) {
        Response response = authenticateUser(email, password);
        if (response.getStatusCode() == 200) {
            return response.jsonPath().getString("token");
        }
        return null;
    }
    
    /**
     * Set authentication token for subsequent requests
     */
    public UserApiClient withAuthToken(String token) {
        client.setAuthToken(token);
        return this;
    }
}
