package com.automation.tests.database;

import com.automation.app.database.UserDatabaseHelper;
import com.automation.core.reporting.ExtentReportManager;
import com.automation.core.utils.BaseTest;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;

/**
 * Database test examples for User table
 */
public class UserDatabaseTest extends BaseTest {
    
    private UserDatabaseHelper userDb;
    private static final String TEST_EMAIL_PREFIX = "dbtest_";
    
    @BeforeClass
    public void setupClass() {
        userDb = new UserDatabaseHelper();
        
        // Clean up any existing test data
        userDb.cleanupTestUsers(TEST_EMAIL_PREFIX + "%");
    }
    
    @Test(description = "Verify user can be created in database")
    public void testCreateUser() {
        ExtentReportManager.assignCategory("Database", "User", "CRUD");
        ExtentReportManager.assignAuthor("Test Automation Team");
        
        // Create test user
        String email = TEST_EMAIL_PREFIX + System.currentTimeMillis() + "@example.com";
        int rowsAffected = userDb.createUser("DB Test User", email, "hashedPassword123", "user");
        
        // Verify user was created
        Assert.assertEquals(rowsAffected, 1, "One row should be affected");
        
        // Verify user exists
        Assert.assertTrue(userDb.emailExists(email), "User should exist in database");
        
        // Get user and verify details
        Map<String, Object> user = userDb.getUserByEmail(email);
        Assert.assertNotNull(user, "User should be retrieved");
        Assert.assertEquals(user.get("EMAIL"), email, "Email should match");
        Assert.assertEquals(user.get("NAME"), "DB Test User", "Name should match");
        
        ExtentReportManager.logPass("User created successfully in database");
    }
    
    @Test(description = "Verify user can be retrieved by ID")
    public void testGetUserById() {
        ExtentReportManager.assignCategory("Database", "User", "Read");
        
        // Create test user first
        String email = TEST_EMAIL_PREFIX + System.currentTimeMillis() + "@example.com";
        userDb.createUser("Get User Test", email, "password", "user");
        
        // Get user by email to get the ID
        Map<String, Object> createdUser = userDb.getUserByEmail(email);
        int userId = ((Number) createdUser.get("USER_ID")).intValue();
        
        // Retrieve user by ID
        Map<String, Object> user = userDb.getUserById(userId);
        
        // Verify user data
        Assert.assertNotNull(user, "User should be retrieved");
        Assert.assertEquals(((Number) user.get("USER_ID")).intValue(), userId, "User ID should match");
        Assert.assertEquals(user.get("EMAIL"), email, "Email should match");
        
        ExtentReportManager.logPass("User retrieved successfully by ID");
    }
    
    @Test(description = "Verify user can be updated in database")
    public void testUpdateUser() {
        ExtentReportManager.assignCategory("Database", "User", "CRUD");
        
        // Create test user
        String originalEmail = TEST_EMAIL_PREFIX + System.currentTimeMillis() + "@example.com";
        userDb.createUser("Original Name", originalEmail, "password", "user");
        
        // Get user ID
        Map<String, Object> createdUser = userDb.getUserByEmail(originalEmail);
        int userId = ((Number) createdUser.get("USER_ID")).intValue();
        
        // Update user
        String newEmail = TEST_EMAIL_PREFIX + System.currentTimeMillis() + "_updated@example.com";
        int rowsAffected = userDb.updateUser(userId, "Updated Name", newEmail, "admin");
        
        // Verify update
        Assert.assertEquals(rowsAffected, 1, "One row should be affected");
        
        // Retrieve and verify updated data
        Map<String, Object> updatedUser = userDb.getUserById(userId);
        Assert.assertEquals(updatedUser.get("NAME"), "Updated Name", "Name should be updated");
        Assert.assertEquals(updatedUser.get("EMAIL"), newEmail, "Email should be updated");
        Assert.assertEquals(updatedUser.get("ROLE"), "admin", "Role should be updated");
        
        ExtentReportManager.logPass("User updated successfully in database");
    }
    
    @Test(description = "Verify user can be deleted from database")
    public void testDeleteUser() {
        ExtentReportManager.assignCategory("Database", "User", "CRUD");
        
        // Create test user
        String email = TEST_EMAIL_PREFIX + System.currentTimeMillis() + "@example.com";
        userDb.createUser("Delete Test User", email, "password", "user");
        
        // Verify user exists
        Assert.assertTrue(userDb.emailExists(email), "User should exist before deletion");
        
        // Delete user
        int rowsAffected = userDb.deleteUserByEmail(email);
        
        // Verify deletion
        Assert.assertEquals(rowsAffected, 1, "One row should be affected");
        Assert.assertFalse(userDb.emailExists(email), "User should not exist after deletion");
        
        ExtentReportManager.logPass("User deleted successfully from database");
    }
    
    @Test(description = "Verify getting users by role")
    public void testGetUsersByRole() {
        ExtentReportManager.assignCategory("Database", "User", "Query");
        
        // Create test users with specific role
        String role = "test_role_" + System.currentTimeMillis();
        String email1 = TEST_EMAIL_PREFIX + "role1_" + System.currentTimeMillis() + "@example.com";
        String email2 = TEST_EMAIL_PREFIX + "role2_" + System.currentTimeMillis() + "@example.com";
        
        userDb.createUser("Role Test 1", email1, "password", role);
        userDb.createUser("Role Test 2", email2, "password", role);
        
        // Get users by role
        List<Map<String, Object>> users = userDb.getUsersByRole(role);
        
        // Verify results
        Assert.assertTrue(users.size() >= 2, "At least 2 users should be found with the role");
        
        // Verify all users have the correct role
        for (Map<String, Object> user : users) {
            Assert.assertEquals(user.get("ROLE"), role, "User should have the correct role");
        }
        
        ExtentReportManager.logPass("Users retrieved successfully by role");
    }
    
    @Test(description = "Verify user count functionality")
    public void testUserCount() {
        ExtentReportManager.assignCategory("Database", "User", "Query");
        
        // Get initial count
        int initialCount = userDb.getUserCount();
        
        // Create a new user
        String email = TEST_EMAIL_PREFIX + System.currentTimeMillis() + "@example.com";
        userDb.createUser("Count Test User", email, "password", "user");
        
        // Get new count
        int newCount = userDb.getUserCount();
        
        // Verify count increased by 1
        Assert.assertEquals(newCount, initialCount + 1, "User count should increase by 1");
        
        ExtentReportManager.logPass("User count verification successful");
    }
    
    @Test(description = "Verify database connection to multiple database types")
    public void testMultipleDatabaseConnections() {
        ExtentReportManager.assignCategory("Database", "Connection", "Smoke");
        
        // Test default connection (Oracle)
        UserDatabaseHelper defaultDb = new UserDatabaseHelper();
        int count1 = defaultDb.getUserCount();
        Assert.assertTrue(count1 >= 0, "Should be able to query default database");
        
        // If secondary database is configured (MS SQL), test it
        try {
            UserDatabaseHelper secondaryDb = new UserDatabaseHelper("secondary");
            int count2 = secondaryDb.getUserCount();
            Assert.assertTrue(count2 >= 0, "Should be able to query secondary database");
            ExtentReportManager.logInfo("Successfully connected to multiple databases");
        } catch (Exception e) {
            ExtentReportManager.logWarning("Secondary database not configured, skipping multi-db test");
        }
        
        ExtentReportManager.logPass("Database connection test successful");
    }
    
    @AfterClass
    public void cleanupClass() {
        // Clean up all test data
        userDb.cleanupTestUsers(TEST_EMAIL_PREFIX + "%");
        ExtentReportManager.logInfo("Test data cleaned up");
    }
}
