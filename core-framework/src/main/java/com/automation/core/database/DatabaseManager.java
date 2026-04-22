package com.automation.core.database;

import com.automation.core.config.ConfigurationManager;
import com.automation.core.exceptions.DatabaseException;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.dbcp2.BasicDataSource;

import java.sql.*;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages database connections and operations
 */
@Log4j2
public class DatabaseManager {
    
    private static final Map<String, DatabaseManager> instances = new HashMap<>();
    private BasicDataSource dataSource;
    private final String connectionName;
    
    private DatabaseManager(String connectionName, String dbType, String url, String username, String password) {
        this.connectionName = connectionName;
        initializeDataSource(dbType, url, username, password);
    }
    
    /**
     * Get database manager instance for default connection
     */
    public static DatabaseManager getInstance() {
        return getInstance("default");
    }
    
    /**
     * Get database manager instance for named connection
     */
    public static synchronized DatabaseManager getInstance(String connectionName) {
        if (!instances.containsKey(connectionName)) {
            ConfigurationManager config = ConfigurationManager.getInstance();
            
            String prefix = connectionName.equals("default") ? "db" : "db." + connectionName;
            String dbType = config.getProperty(prefix + ".type");
            String url = config.getProperty(prefix + ".url");
            String username = config.getProperty(prefix + ".username");
            String password = config.getProperty(prefix + ".password");
            
            if (dbType == null || url == null) {
                throw new DatabaseException("Database configuration not found for: " + connectionName);
            }
            
            instances.put(connectionName, new DatabaseManager(connectionName, dbType, url, username, password));
        }
        return instances.get(connectionName);
    }
    
    private void initializeDataSource(String dbType, String url, String username, String password) {
        try {
            DatabaseType type = DatabaseType.fromString(dbType);
            
            dataSource = new BasicDataSource();
            dataSource.setDriverClassName(type.getDriverClassName());
            dataSource.setUrl(url);
            dataSource.setUsername(username);
            dataSource.setPassword(password);
            
            // Connection pool settings
            dataSource.setInitialSize(2);
            dataSource.setMaxTotal(10);
            dataSource.setMaxIdle(5);
            dataSource.setMinIdle(2);
            dataSource.setMaxWait(Duration.ofMillis(10000));
            
            // Test connection
            try (Connection conn = dataSource.getConnection()) {
                log.info("Database connection initialized for {}: {}", connectionName, dbType);
            }
            
        } catch (Exception e) {
            throw new DatabaseException("Failed to initialize database connection: " + connectionName, e);
        }
    }
    
    /**
     * Execute SELECT query and return results as list of maps
     */
    public List<Map<String, Object>> executeQuery(String query, Object... params) {
        log.debug("Executing query: {}", query);
        List<Map<String, Object>> results = new ArrayList<>();
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            setParameters(stmt, params);
            
            try (ResultSet rs = stmt.executeQuery()) {
                ResultSetMetaData metaData = rs.getMetaData();
                int columnCount = metaData.getColumnCount();
                
                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();
                    for (int i = 1; i <= columnCount; i++) {
                        row.put(metaData.getColumnName(i), rs.getObject(i));
                    }
                    results.add(row);
                }
            }
            
            log.debug("Query returned {} rows", results.size());
            
        } catch (SQLException e) {
            throw new DatabaseException("Error executing query: " + query, e);
        }
        
        return results;
    }
    
    /**
     * Execute INSERT, UPDATE, DELETE query
     */
    public int executeUpdate(String query, Object... params) {
        log.debug("Executing update: {}", query);
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            setParameters(stmt, params);
            int rowsAffected = stmt.executeUpdate();
            
            log.debug("Update affected {} rows", rowsAffected);
            return rowsAffected;
            
        } catch (SQLException e) {
            throw new DatabaseException("Error executing update: " + query, e);
        }
    }
    
    /**
     * Execute query and return single value
     */
    public Object executeScalar(String query, Object... params) {
        List<Map<String, Object>> results = executeQuery(query, params);
        if (results.isEmpty()) {
            return null;
        }
        Map<String, Object> firstRow = results.get(0);
        return firstRow.values().iterator().next();
    }
    
    /**
     * Execute stored procedure
     */
    public void executeProcedure(String procedureName, Object... params) {
        log.debug("Executing procedure: {}", procedureName);
        
        StringBuilder callString = new StringBuilder("{call ").append(procedureName).append("(");
        for (int i = 0; i < params.length; i++) {
            callString.append("?");
            if (i < params.length - 1) {
                callString.append(",");
            }
        }
        callString.append(")}");
        
        try (Connection conn = dataSource.getConnection();
             CallableStatement stmt = conn.prepareCall(callString.toString())) {
            
            setParameters(stmt, params);
            stmt.execute();
            
            log.debug("Procedure executed successfully");
            
        } catch (SQLException e) {
            throw new DatabaseException("Error executing procedure: " + procedureName, e);
        }
    }
    
    /**
     * Check if record exists
     */
    public boolean recordExists(String query, Object... params) {
        List<Map<String, Object>> results = executeQuery(query, params);
        return !results.isEmpty();
    }
    
    /**
     * Get record count
     */
    public int getRecordCount(String tableName, String whereClause) {
        String query = "SELECT COUNT(*) FROM " + tableName;
        if (whereClause != null && !whereClause.isEmpty()) {
            query += " WHERE " + whereClause;
        }
        Object count = executeScalar(query);
        return count != null ? ((Number) count).intValue() : 0;
    }
    
    private void setParameters(PreparedStatement stmt, Object... params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            stmt.setObject(i + 1, params[i]);
        }
    }
    
    /**
     * Close all database connections
     */
    public static void closeAll() {
        for (DatabaseManager manager : instances.values()) {
            try {
                if (manager.dataSource != null) {
                    manager.dataSource.close();
                    log.info("Database connection closed: {}", manager.connectionName);
                }
            } catch (SQLException e) {
                log.error("Error closing database connection: {}", manager.connectionName, e);
            }
        }
        instances.clear();
    }
    
    /**
     * Close specific connection
     */
    public void close() {
        try {
            if (dataSource != null) {
                dataSource.close();
                instances.remove(connectionName);
                log.info("Database connection closed: {}", connectionName);
            }
        } catch (SQLException e) {
            log.error("Error closing database connection: {}", connectionName, e);
        }
    }
}
