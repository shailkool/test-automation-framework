package com.app.database;

import com.framework.database.DatabaseManager;
import com.framework.database.DatabaseManager.DatabaseType;
import lombok.extern.log4j.Log4j2;

import java.util.List;
import java.util.Map;

/**
 * Database queries specific to User entity
 * This is an example implementation
 */
@Log4j2
public class UserDatabaseQueries {
    
    private DatabaseManager dbManager;
    private DatabaseType dbType;
    
    public UserDatabaseQueries(DatabaseType dbType) {
        this.dbManager = DatabaseManager.getInstance();
        this.dbType = dbType;
    }
    
    /**
     * Get user by ID
     */
    public Map<String, Object> getUserById(int userId) {
        String query = "SELECT * FROM users WHERE user_id = ?";
        List<Map<String, Object>> results = dbManager.executeQuery(dbType, query, userId);
        
        if (results.isEmpty()) {
            log.warn("No user found with ID: {}", userId);
            return null;
        }
        
        log.info("Retrieved user with ID: {}", userId);
        return results.get(0);
    }
    
    /**
     * Get user by email
     */
    public Map<String, Object> getUserByEmail(String email) {
        String query = "SELECT * FROM users WHERE email = ?";
        List<Map<String, Object>> results = dbManager.executeQuery(dbType, query, email);
        
        if (results.isEmpty()) {
            log.warn("No user found with email: {}", email);
            return null;
        }
        
        log.info("Retrieved user with email: {}", email);
        return results.get(0);
    }
    
    /**
     * Get all users
     */
    public List<Map<String, Object>> getAllUsers() {
        String query = "SELECT * FROM users";
        List<Map<String, Object>> users = dbManager.executeQuery(dbType, query);
        log.info("Retrieved {} users from database", users.size());
        return users;
    }
    
    /**
     * Get users by role
     */
    public List<Map<String, Object>> getUsersByRole(String role) {
        String query = "SELECT * FROM users WHERE role = ?";
        List<Map<String, Object>> users = dbManager.executeQuery(dbType, query, role);
        log.info("Retrieved {} users with role: {}", users.size(), role);
        return users;
    }
    
    /**
     * Create a new user
     */
    public int createUser(String name, String email, String password, String role) {
        String query = "INSERT INTO users (name, email, password, role, created_date) VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP)";
        int rowsAffected = dbManager.executeUpdate(dbType, query, name, email, password, role);
        log.info("Created user with email: {}", email);
        return rowsAffected;
    }
    
    /**
     * Update user
     */
    public int updateUser(int userId, String name, String email) {
        String query = "UPDATE users SET name = ?, email = ?, modified_date = CURRENT_TIMESTAMP WHERE user_id = ?";
        int rowsAffected = dbManager.executeUpdate(dbType, query, name, email, userId);
        log.info("Updated user with ID: {}", userId);
        return rowsAffected;
    }
    
    /**
     * Delete user
     */
    public int deleteUser(int userId) {
        String query = "DELETE FROM users WHERE user_id = ?";
        int rowsAffected = dbManager.executeUpdate(dbType, query, userId);
        log.info("Deleted user with ID: {}", userId);
        return rowsAffected;
    }
    
    /**
     * Get user count
     */
    public int getUserCount() {
        String query = "SELECT COUNT(*) as count FROM users";
        Object count = dbManager.getSingleValue(dbType, query);
        log.info("Total user count: {}", count);
        return ((Number) count).intValue();
    }
    
    /**
     * Check if user exists by email
     */
    public boolean userExistsByEmail(String email) {
        String query = "SELECT COUNT(*) FROM users WHERE email = ?";
        Object count = dbManager.getSingleValue(dbType, query, email);
        boolean exists = ((Number) count).intValue() > 0;
        log.info("User exists check for {}: {}", email, exists);
        return exists;
    }
    
    /**
     * Get active users (example of business logic query)
     */
    public List<Map<String, Object>> getActiveUsers() {
        String query = "SELECT * FROM users WHERE status = 'ACTIVE' AND is_deleted = 0";
        List<Map<String, Object>> users = dbManager.executeQuery(dbType, query);
        log.info("Retrieved {} active users", users.size());
        return users;
    }
    
    /**
     * Clean up test data (useful for test teardown)
     */
    public void deleteTestUsers() {
        String query = "DELETE FROM users WHERE email LIKE 'test_%@example.com'";
        int rowsAffected = dbManager.executeUpdate(dbType, query);
        log.info("Deleted {} test users", rowsAffected);
    }
}
