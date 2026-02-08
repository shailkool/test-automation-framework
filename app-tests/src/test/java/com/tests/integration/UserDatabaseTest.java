package com.tests.integration;

import com.app.database.UserDatabaseQueries;
import com.framework.base.BaseTest;
import com.framework.database.DatabaseManager;
import com.framework.database.DatabaseManager.DatabaseType;
import com.framework.reporting.ExtentReportManager;
import com.framework.utils.DataUtils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;

/**
 * Database Integration Tests
 */
public class UserDatabaseTest extends BaseTest {
    
    private UserDatabaseQueries userDbQueries;
    private DatabaseType dbType = DatabaseType.ORACLE; // Change as needed
    private int testUserId;
    
    @BeforeClass
    public void setupClass() {
        // Initialize database connection
        DatabaseManager.getInstance().initializeDataSource(dbType);
        
        // Test connection
        boolean connected = DatabaseManager.getInstance().testConnection(dbType);
        Assert.assertTrue(connected, "Database connection should be successful");
        ExtentReportManager.logInfo("Database connection established");
        
        userDbQueries = new UserDatabaseQueries(dbType);
    }
    
    @Test(description = "Verify database connection", priority = 1)
    public void testDatabaseConnection() {
        ExtentReportManager.logInfo("Testing database connection");
        
        boolean isConnected = DatabaseManager.getInstance().testConnection(dbType);
        Assert.assertTrue(isConnected, "Should be able to connect to database");
        ExtentReportManager.logPass("Database connection successful");
    }
    
    @Test(description = "Verify create user in database", priority = 2)
    public void testCreateUserInDatabase() {
        ExtentReportManager.logInfo("Starting create user in database test");
        
        String email = DataUtils.generateRandomEmail();
        
        // Create user
        int rowsAffected = userDbQueries.createUser(
                "Test User",
                email,
                "hashedPassword123",
                "USER"
        );
        
        Assert.assertEquals(rowsAffected, 1, "One row should be affected");
        ExtentReportManager.logPass("User created in database");
        
        // Verify user exists
        Map<String, Object> user = userDbQueries.getUserByEmail(email);
        Assert.assertNotNull(user, "User should exist in database");
        Assert.assertEquals(user.get("email"), email, "Email should match");
        ExtentReportManager.logPass("User verified in database");
        
        // Store user ID for other tests
        testUserId = ((Number) user.get("user_id")).intValue();
    }
    
    @Test(description = "Verify get user by ID from database", priority = 3, dependsOnMethods = "testCreateUserInDatabase")
    public void testGetUserById() {
        ExtentReportManager.logInfo("Starting get user by ID test");
        
        Map<String, Object> user = userDbQueries.getUserById(testUserId);
        
        Assert.assertNotNull(user, "User should be found");
        Assert.assertEquals(((Number) user.get("user_id")).intValue(), testUserId, "User ID should match");
        ExtentReportManager.logPass("User retrieved successfully by ID");
    }
    
    @Test(description = "Verify get all users from database", priority = 4)
    public void testGetAllUsers() {
        ExtentReportManager.logInfo("Starting get all users test");
        
        List<Map<String, Object>> users = userDbQueries.getAllUsers();
        
        Assert.assertNotNull(users, "Users list should not be null");
        Assert.assertTrue(users.size() > 0, "Should have at least one user");
        ExtentReportManager.logPass("Retrieved " + users.size() + " users from database");
    }
    
    @Test(description = "Verify get users by role", priority = 5)
    public void testGetUsersByRole() {
        ExtentReportManager.logInfo("Starting get users by role test");
        
        List<Map<String, Object>> users = userDbQueries.getUsersByRole("USER");
        
        Assert.assertNotNull(users, "Users list should not be null");
        
        // Verify all returned users have correct role
        for (Map<String, Object> user : users) {
            Assert.assertEquals(user.get("role"), "USER", "All users should have USER role");
        }
        
        ExtentReportManager.logPass("Retrieved " + users.size() + " users with USER role");
    }
    
    @Test(description = "Verify update user in database", priority = 6, dependsOnMethods = "testCreateUserInDatabase")
    public void testUpdateUser() {
        ExtentReportManager.logInfo("Starting update user test");
        
        String newEmail = DataUtils.generateRandomEmail();
        
        // Update user
        int rowsAffected = userDbQueries.updateUser(testUserId, "Updated Name", newEmail);
        
        Assert.assertEquals(rowsAffected, 1, "One row should be affected");
        ExtentReportManager.logPass("User updated in database");
        
        // Verify update
        Map<String, Object> updatedUser = userDbQueries.getUserById(testUserId);
        Assert.assertEquals(updatedUser.get("name"), "Updated Name", "Name should be updated");
        Assert.assertEquals(updatedUser.get("email"), newEmail, "Email should be updated");
        ExtentReportManager.logPass("User update verified");
    }
    
    @Test(description = "Verify user exists check", priority = 7)
    public void testUserExistsByEmail() {
        ExtentReportManager.logInfo("Starting user exists check test");
        
        // Create a user
        String email = DataUtils.generateRandomEmail();
        userDbQueries.createUser("Exists Test", email, "password", "USER");
        
        // Check if exists
        boolean exists = userDbQueries.userExistsByEmail(email);
        Assert.assertTrue(exists, "User should exist");
        ExtentReportManager.logPass("User existence verified");
        
        // Check non-existent user
        boolean notExists = userDbQueries.userExistsByEmail("nonexistent@example.com");
        Assert.assertFalse(notExists, "Non-existent user should return false");
        ExtentReportManager.logPass("Non-existent user check passed");
    }
    
    @Test(description = "Verify get user count", priority = 8)
    public void testGetUserCount() {
        ExtentReportManager.logInfo("Starting get user count test");
        
        int count = userDbQueries.getUserCount();
        
        Assert.assertTrue(count > 0, "User count should be greater than 0");
        ExtentReportManager.logPass("Total user count: " + count);
    }
    
    @Test(description = "Verify delete user from database", priority = 9, dependsOnMethods = "testUpdateUser")
    public void testDeleteUser() {
        ExtentReportManager.logInfo("Starting delete user test");
        
        // Delete user
        int rowsAffected = userDbQueries.deleteUser(testUserId);
        
        Assert.assertEquals(rowsAffected, 1, "One row should be affected");
        ExtentReportManager.logPass("User deleted from database");
        
        // Verify deletion
        Map<String, Object> deletedUser = userDbQueries.getUserById(testUserId);
        Assert.assertNull(deletedUser, "User should not exist after deletion");
        ExtentReportManager.logPass("User deletion verified");
    }
    
    @AfterClass
    public void cleanupClass() {
        // Clean up any test data
        userDbQueries.deleteTestUsers();
        ExtentReportManager.logInfo("Test data cleaned up");
    }
}
