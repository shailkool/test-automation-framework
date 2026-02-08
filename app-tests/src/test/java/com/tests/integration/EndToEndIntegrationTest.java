package com.tests.integration;

import com.app.workflows.UserWorkflow;
import com.framework.base.BaseTest;
import com.framework.database.DatabaseManager.DatabaseType;
import com.framework.reporting.ExtentReportManager;
import com.framework.ui.PlaywrightManager;
import com.framework.utils.DataUtils;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Map;

/**
 * End-to-End Integration Tests
 * Tests that span across UI, API, and Database layers
 */
public class EndToEndIntegrationTest extends BaseTest {
    
    private UserWorkflow userWorkflow;
    private DatabaseType dbType = DatabaseType.ORACLE; // Change as needed
    
    @BeforeMethod
    public void setupTest() {
        PlaywrightManager.initializeBrowser();
        userWorkflow = new UserWorkflow(dbType);
    }
    
    @Test(description = "End-to-end user registration and verification")
    public void testCompleteUserRegistrationFlow() {
        ExtentReportManager.logInfo("Starting end-to-end user registration test");
        
        // Generate test data
        String name = "E2E Test User";
        String email = DataUtils.generateRandomEmail();
        String password = "TestPass123!";
        String role = "USER";
        
        ExtentReportManager.logInfo("Test data generated - Email: " + email);
        
        // Step 1: Register user (API + Database verification)
        Map<String, Object> registeredUser = userWorkflow.registerNewUser(name, email, password, role);
        
        Assert.assertNotNull(registeredUser, "User should be registered");
        Assert.assertEquals(registeredUser.get("email"), email, "Email should match");
        ExtentReportManager.logPass("User registered successfully via API and verified in database");
        
        int userId = ((Number) registeredUser.get("user_id")).intValue();
        ExtentReportManager.logInfo("User ID: " + userId);
        
        // Step 2: Verify user in all layers
        boolean verifiedInAllLayers = userWorkflow.verifyUserInAllLayers(email, password);
        
        Assert.assertTrue(verifiedInAllLayers, "User should be verified in all layers");
        ExtentReportManager.logPass("User verified successfully in Database, API, and UI");
    }
    
    @Test(description = "End-to-end user profile update flow")
    public void testCompleteUserUpdateFlow() {
        ExtentReportManager.logInfo("Starting end-to-end user update test");
        
        // Step 1: Create user
        String originalName = "Original User";
        String originalEmail = DataUtils.generateRandomEmail();
        
        Map<String, Object> user = userWorkflow.registerNewUser(
                originalName,
                originalEmail,
                "password123",
                "USER"
        );
        
        int userId = ((Number) user.get("user_id")).intValue();
        ExtentReportManager.logPass("User created with ID: " + userId);
        
        // Step 2: Update user profile
        String updatedName = "Updated User";
        String updatedEmail = DataUtils.generateRandomEmail();
        
        userWorkflow.updateUserProfile(userId, updatedName, updatedEmail);
        ExtentReportManager.logPass("User profile updated successfully");
        
        // Step 3: Verify update persisted
        // This is already verified in the workflow method
        ExtentReportManager.logPass("Update verified in API and Database");
    }
    
    @Test(description = "End-to-end user deletion flow")
    public void testCompleteUserDeletionFlow() {
        ExtentReportManager.logInfo("Starting end-to-end user deletion test");
        
        // Step 1: Create user
        Map<String, Object> user = userWorkflow.registerNewUser(
                "User To Delete",
                DataUtils.generateRandomEmail(),
                "password123",
                "USER"
        );
        
        int userId = ((Number) user.get("user_id")).intValue();
        ExtentReportManager.logPass("User created with ID: " + userId);
        
        // Step 2: Delete user
        userWorkflow.deleteUserCompletely(userId);
        ExtentReportManager.logPass("User deleted successfully from all layers");
        
        // Verification is already done in the workflow method
        ExtentReportManager.logPass("Deletion verified - user no longer exists");
    }
    
    @Test(description = "End-to-end multi-user scenario")
    public void testMultiUserScenario() {
        ExtentReportManager.logInfo("Starting multi-user scenario test");
        
        // Create multiple users
        int numberOfUsers = 3;
        int[] userIds = new int[numberOfUsers];
        
        for (int i = 0; i < numberOfUsers; i++) {
            Map<String, Object> user = userWorkflow.registerNewUser(
                    "Multi User " + (i + 1),
                    DataUtils.generateRandomEmail(),
                    "password123",
                    "USER"
            );
            
            userIds[i] = ((Number) user.get("user_id")).intValue();
            ExtentReportManager.logInfo("Created user " + (i + 1) + " with ID: " + userIds[i]);
        }
        
        ExtentReportManager.logPass(numberOfUsers + " users created successfully");
        
        // Update middle user
        userWorkflow.updateUserProfile(
                userIds[1],
                "Updated Multi User",
                DataUtils.generateRandomEmail()
        );
        ExtentReportManager.logPass("User 2 updated successfully");
        
        // Delete first user
        userWorkflow.deleteUserCompletely(userIds[0]);
        ExtentReportManager.logPass("User 1 deleted successfully");
        
        // Clean up remaining users
        for (int i = 1; i < numberOfUsers; i++) {
            userWorkflow.deleteUserCompletely(userIds[i]);
        }
        
        ExtentReportManager.logPass("Multi-user scenario completed and cleaned up");
    }
    
    @AfterMethod
    public void teardownTest() {
        // Clean up any remaining test data
        userWorkflow.cleanupTestData();
        PlaywrightManager.cleanup();
        ExtentReportManager.logInfo("Test cleanup completed");
    }
}
