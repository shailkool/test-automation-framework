package com.app.api;

import com.framework.api.RestAssuredClient;
import io.restassured.response.Response;
import lombok.extern.log4j.Log4j2;

import java.util.HashMap;
import java.util.Map;

/**
 * API client for User related endpoints
 * This is an example implementation
 */
@Log4j2
public class UserApiClient {
    
    private RestAssuredClient client;
    private static final String USERS_ENDPOINT = "/api/users";
    
    public UserApiClient() {
        this.client = new RestAssuredClient();
    }
    
    /**
     * Create a new user
     */
    public Response createUser(String name, String email, String role) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("name", name);
        requestBody.put("email", email);
        requestBody.put("role", role);
        
        log.info("Creating user with email: {}", email);
        return client.post(USERS_ENDPOINT, requestBody);
    }
    
    /**
     * Get user by ID
     */
    public Response getUserById(int userId) {
        log.info("Getting user with ID: {}", userId);
        return client.get(USERS_ENDPOINT + "/" + userId);
    }
    
    /**
     * Get all users
     */
    public Response getAllUsers() {
        log.info("Getting all users");
        return client.get(USERS_ENDPOINT);
    }
    
    /**
     * Update user
     */
    public Response updateUser(int userId, String name, String email) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("name", name);
        requestBody.put("email", email);
        
        log.info("Updating user with ID: {}", userId);
        return client.put(USERS_ENDPOINT + "/" + userId, requestBody);
    }
    
    /**
     * Delete user
     */
    public Response deleteUser(int userId) {
        log.info("Deleting user with ID: {}", userId);
        return client.delete(USERS_ENDPOINT + "/" + userId);
    }
    
    /**
     * Search users by name
     */
    public Response searchUsersByName(String name) {
        log.info("Searching users by name: {}", name);
        return client
                .addQueryParam("name", name)
                .get(USERS_ENDPOINT + "/search");
    }
    
    /**
     * Authenticate user
     */
    public Response authenticateUser(String email, String password) {
        Map<String, Object> credentials = new HashMap<>();
        credentials.put("email", email);
        credentials.put("password", password);
        
        log.info("Authenticating user: {}", email);
        return client.post("/api/auth/login", credentials);
    }
    
    /**
     * Get user token and set for subsequent requests
     */
    public String getUserToken(String email, String password) {
        Response response = authenticateUser(email, password);
        String token = response.jsonPath().getString("token");
        
        if (token != null) {
            client.setOAuth2Token(token);
            log.info("Token obtained and set for user: {}", email);
        }
        
        return token;
    }
}
