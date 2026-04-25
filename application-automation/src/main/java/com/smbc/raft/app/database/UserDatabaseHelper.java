package com.smbc.raft.app.database;

import com.smbc.raft.core.database.DatabaseManager;
import com.smbc.raft.core.reporting.ExtentReportManager;
import lombok.extern.log4j.Log4j2;

import java.util.List;
import java.util.Map;

/**
 * Database helper for User-related database operations
 * This is an example - modify according to your database schema
 */
@Log4j2
public class UserDatabaseHelper {
    
    private DatabaseManager dbManager;
    
    public UserDatabaseHelper() {
        this.dbManager = DatabaseManager.getInstance();
    }
    
    public UserDatabaseHelper(String connectionName) {
        this.dbManager = DatabaseManager.getInstance(connectionName);
    }
    
    /**
     * Get user by ID
     */
    public Map<String, Object> getUserById(int userId) {
        log.info("Getting user from database: {}", userId);
        String query = "SELECT * FROM users WHERE user_id = ?";
        List<Map<String, Object>> results = dbManager.executeQuery(query, userId);
        
        if (!results.isEmpty()) {
            ExtentReportManager.logInfo("User found in database: " + userId);
            return results.get(0);
        }
        
        ExtentReportManager.logInfo("User not found in database: " + userId);
        return null;
    }
    
    /**
     * Get user by email
     */
    public Map<String, Object> getUserByEmail(String email) {
        log.info("Getting user by email: {}", email);
        String query = "SELECT * FROM users WHERE email = ?";
        List<Map<String, Object>> results = dbManager.executeQuery(query, email);
        
        if (!results.isEmpty()) {
            ExtentReportManager.logInfo("User found with email: " + email);
            return results.get(0);
        }
        
        ExtentReportManager.logInfo("User not found with email: " + email);
        return null;
    }
    
    /**
     * Get all users
     */
    public List<Map<String, Object>> getAllUsers() {
        log.info("Getting all users from database");
        String query = "SELECT * FROM users ORDER BY user_id";
        List<Map<String, Object>> users = dbManager.executeQuery(query);
        ExtentReportManager.logInfo("Retrieved " + users.size() + " users from database");
        return users;
    }
    
    /**
     * Get users by role
     */
    public List<Map<String, Object>> getUsersByRole(String role) {
        log.info("Getting users by role: {}", role);
        String query = "SELECT * FROM users WHERE role = ? ORDER BY user_id";
        List<Map<String, Object>> users = dbManager.executeQuery(query, role);
        ExtentReportManager.logInfo("Found " + users.size() + " users with role: " + role);
        return users;
    }
    
    /**
     * Create user
     */
    public int createUser(String name, String email, String password, String role) {
        log.info("Creating user in database: {}", email);
        String query = "INSERT INTO users (name, email, password, role, created_at) VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP)";
        int rowsAffected = dbManager.executeUpdate(query, name, email, password, role);
        ExtentReportManager.logInfo("User created in database: " + email);
        return rowsAffected;
    }
    
    /**
     * Update user
     */
    public int updateUser(int userId, String name, String email, String role) {
        log.info("Updating user in database: {}", userId);
        String query = "UPDATE users SET name = ?, email = ?, role = ?, updated_at = CURRENT_TIMESTAMP WHERE user_id = ?";
        int rowsAffected = dbManager.executeUpdate(query, name, email, role, userId);
        ExtentReportManager.logInfo("User updated in database: " + userId);
        return rowsAffected;
    }
    
    /**
     * Delete user
     */
    public int deleteUser(int userId) {
        log.info("Deleting user from database: {}", userId);
        String query = "DELETE FROM users WHERE user_id = ?";
        int rowsAffected = dbManager.executeUpdate(query, userId);
        ExtentReportManager.logInfo("User deleted from database: " + userId);
        return rowsAffected;
    }
    
    /**
     * Delete user by email
     */
    public int deleteUserByEmail(String email) {
        log.info("Deleting user by email: {}", email);
        String query = "DELETE FROM users WHERE email = ?";
        int rowsAffected = dbManager.executeUpdate(query, email);
        ExtentReportManager.logInfo("User deleted with email: " + email);
        return rowsAffected;
    }
    
    /**
     * Check if user exists
     */
    public boolean userExists(int userId) {
        log.info("Checking if user exists: {}", userId);
        String query = "SELECT COUNT(*) FROM users WHERE user_id = ?";
        Object count = dbManager.executeScalar(query, userId);
        boolean exists = count != null && ((Number) count).intValue() > 0;
        ExtentReportManager.logInfo("User exists: " + exists);
        return exists;
    }
    
    /**
     * Check if email exists
     */
    public boolean emailExists(String email) {
        log.info("Checking if email exists: {}", email);
        String query = "SELECT COUNT(*) FROM users WHERE email = ?";
        Object count = dbManager.executeScalar(query, email);
        boolean exists = count != null && ((Number) count).intValue() > 0;
        ExtentReportManager.logInfo("Email exists: " + exists);
        return exists;
    }
    
    /**
     * Get user count
     */
    public int getUserCount() {
        log.info("Getting total user count");
        int count = dbManager.getRecordCount("users", null);
        ExtentReportManager.logInfo("Total users: " + count);
        return count;
    }
    
    /**
     * Get active user count
     */
    public int getActiveUserCount() {
        log.info("Getting active user count");
        int count = dbManager.getRecordCount("users", "status = 'active'");
        ExtentReportManager.logInfo("Active users: " + count);
        return count;
    }
    
    /**
     * Cleanup test data
     */
    public void cleanupTestUsers(String emailPattern) {
        log.info("Cleaning up test users with pattern: {}", emailPattern);
        String query = "DELETE FROM users WHERE email LIKE ?";
        int rowsAffected = dbManager.executeUpdate(query, emailPattern);
        ExtentReportManager.logInfo("Cleaned up " + rowsAffected + " test users");
    }
}
