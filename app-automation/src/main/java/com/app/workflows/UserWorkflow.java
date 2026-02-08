package com.app.workflows;

import com.app.api.UserApiClient;
import com.app.database.UserDatabaseQueries;
import com.app.pages.HomePage;
import com.app.pages.LoginPage;
import com.framework.config.ConfigManager;
import com.framework.database.DatabaseManager.DatabaseType;
import io.restassured.response.Response;
import lombok.extern.log4j.Log4j2;

import java.util.Map;

/**
 * Workflow class for User-related operations
 * Combines UI, API, and Database operations
 */
@Log4j2
public class UserWorkflow {
    
    private LoginPage loginPage;
    private HomePage homePage;
    private UserApiClient userApiClient;
    private UserDatabaseQueries userDbQueries;
    private ConfigManager config;
    
    public UserWorkflow(DatabaseType dbType) {
        this.loginPage = new LoginPage();
        this.homePage = new HomePage();
        this.userApiClient = new UserApiClient();
        this.userDbQueries = new UserDatabaseQueries(dbType);
        this.config = ConfigManager.getInstance();
    }
    
    /**
     * Complete user registration workflow
     * Creates user via API and verifies in database
     */
    public Map<String, Object> registerNewUser(String name, String email, String password, String role) {
        log.info("Starting user registration workflow for: {}", email);
        
        // Step 1: Create user via API
        Response createResponse = userApiClient.createUser(name, email, role);
        
        if (createResponse.getStatusCode() != 201) {
            throw new RuntimeException("User creation failed. Status: " + createResponse.getStatusCode());
        }
        
        int userId = createResponse.jsonPath().getInt("id");
        log.info("User created via API with ID: {}", userId);
        
        // Step 2: Verify user in database
        Map<String, Object> userFromDb = userDbQueries.getUserById(userId);
        
        if (userFromDb == null) {
            throw new RuntimeException("User not found in database after creation");
        }
        
        log.info("User registration workflow completed successfully");
        return userFromDb;
    }
    
    /**
     * Login workflow via UI
     */
    public void loginViaUI(String username, String password) {
        log.info("Starting UI login workflow for: {}", username);
        
        String appUrl = config.getAppUiUrl();
        loginPage.navigateToLoginPage(appUrl);
        loginPage.login(username, password);
        
        // Verify login success
        if (!homePage.isHomePageLoaded()) {
            throw new RuntimeException("Login failed - Home page not loaded");
        }
        
        log.info("UI login workflow completed successfully");
    }
    
    /**
     * Complete end-to-end user verification workflow
     * Verifies user exists in all layers: DB, API, and UI
     */
    public boolean verifyUserInAllLayers(String email, String password) {
        log.info("Starting complete user verification for: {}", email);
        
        // Step 1: Verify in Database
        Map<String, Object> userFromDb = userDbQueries.getUserByEmail(email);
        if (userFromDb == null) {
            log.error("User not found in database");
            return false;
        }
        log.info("User verified in database");
        
        // Step 2: Verify via API
        String token = userApiClient.getUserToken(email, password);
        if (token == null || token.isEmpty()) {
            log.error("User authentication via API failed");
            return false;
        }
        log.info("User verified via API");
        
        // Step 3: Verify via UI
        try {
            loginViaUI(email, password);
            String loggedInUser = homePage.getLoggedInUsername();
            
            if (!loggedInUser.contains((String) userFromDb.get("name"))) {
                log.error("Username mismatch in UI");
                return false;
            }
            log.info("User verified in UI");
            
        } catch (Exception e) {
            log.error("UI verification failed: {}", e.getMessage());
            return false;
        }
        
        log.info("User verification completed successfully in all layers");
        return true;
    }
    
    /**
     * Update user profile workflow
     */
    public void updateUserProfile(int userId, String newName, String newEmail) {
        log.info("Starting user profile update workflow for user ID: {}", userId);
        
        // Update via API
        Response updateResponse = userApiClient.updateUser(userId, newName, newEmail);
        
        if (updateResponse.getStatusCode() != 200) {
            throw new RuntimeException("User update failed. Status: " + updateResponse.getStatusCode());
        }
        
        // Verify in database
        Map<String, Object> updatedUser = userDbQueries.getUserById(userId);
        
        if (!updatedUser.get("name").equals(newName) || !updatedUser.get("email").equals(newEmail)) {
            throw new RuntimeException("User data not updated correctly in database");
        }
        
        log.info("User profile update workflow completed successfully");
    }
    
    /**
     * Delete user and cleanup workflow
     */
    public void deleteUserCompletely(int userId) {
        log.info("Starting user deletion workflow for user ID: {}", userId);
        
        // Delete via API
        Response deleteResponse = userApiClient.deleteUser(userId);
        
        if (deleteResponse.getStatusCode() != 204 && deleteResponse.getStatusCode() != 200) {
            throw new RuntimeException("User deletion failed. Status: " + deleteResponse.getStatusCode());
        }
        
        // Verify deletion in database
        Map<String, Object> user = userDbQueries.getUserById(userId);
        
        if (user != null) {
            throw new RuntimeException("User still exists in database after deletion");
        }
        
        log.info("User deletion workflow completed successfully");
    }
    
    /**
     * Cleanup test data
     */
    public void cleanupTestData() {
        log.info("Cleaning up test data");
        userDbQueries.deleteTestUsers();
        log.info("Test data cleanup completed");
    }
}
