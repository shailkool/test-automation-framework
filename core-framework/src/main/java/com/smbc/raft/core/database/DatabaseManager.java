package com.smbc.raft.core.database;

import com.smbc.raft.core.config.ConfigurationManager;
import com.smbc.raft.core.exceptions.DatabaseException;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import lombok.extern.log4j.Log4j2;

/**
 * Manages database connections and operations via HikariCP connection pool. Replaces DBCP2 — public
 * API is identical; only the pool implementation changes.
 */
@Log4j2
public class DatabaseManager {

  private static final Map<String, DatabaseManager> INSTANCES = new HashMap<>();
  private HikariDataSource dataSource;
  private final String connectionName;

  private DatabaseManager(
      String connectionName, String dbType, String url, String username, String password) {
    this.connectionName = connectionName;
    initializeDataSource(dbType, url, username, password);
  }

  /** Get database manager instance for default connection */
  public static DatabaseManager getInstance() {
    return getInstance("default");
  }

  /** Get database manager instance for named connection */
  public static synchronized DatabaseManager getInstance(String connectionName) {
    if (!INSTANCES.containsKey(connectionName)) {
      ConfigurationManager config = ConfigurationManager.getInstance();
      String prefix = connectionName.equals("default") ? "db" : "db." + connectionName;

      String dbType = config.getProperty(prefix + ".type");
      String url = config.getProperty(prefix + ".url");
      String username = config.getProperty(prefix + ".username");
      String password = config.getProperty(prefix + ".password");

      if (dbType == null || url == null) {
        throw new DatabaseException("Database configuration not found for: " + connectionName);
      }

      INSTANCES.put(
          connectionName, new DatabaseManager(connectionName, dbType, url, username, password));
    }
    return INSTANCES.get(connectionName);
  }

  /** Initialize HikariCP data source */
  private void initializeDataSource(String dbType, String url, String username, String password) {
    try {
      DatabaseType type = DatabaseType.fromString(dbType);
      ConfigurationManager config = ConfigurationManager.getInstance();

      HikariConfig cfg = new HikariConfig();
      cfg.setPoolName("HikariPool-" + connectionName);
      cfg.setDriverClassName(type.getDriverClassName());
      cfg.setJdbcUrl(url);
      cfg.setUsername(username);
      cfg.setPassword(password);

      // Pool sizing - can be overridden via properties
      cfg.setMaximumPoolSize(Integer.parseInt(config.getProperty("db.pool.maximumPoolSize", "10")));
      cfg.setMinimumIdle(Integer.parseInt(config.getProperty("db.pool.minimumIdle", "2")));
      cfg.setIdleTimeout(
          Long.parseLong(config.getProperty("db.pool.idleTimeout", "600000"))); // 10 mins

      // Timeouts
      cfg.setConnectionTimeout(
          Long.parseLong(config.getProperty("db.pool.connectionTimeout", "10000"))); // 10s
      cfg.setValidationTimeout(TimeUnit.SECONDS.toMillis(5));
      cfg.setMaxLifetime(
          Long.parseLong(config.getProperty("db.pool.maxLifetime", "1800000"))); // 30 mins

      // Database-specific tuning
      applyDatabaseTuning(cfg, type);

      // Warm up: create the pool and verify at least one connection works
      dataSource = new HikariDataSource(cfg);
      try (Connection probe = dataSource.getConnection()) {
        log.info(
            "Database pool initialised [{}]: type={}, poolSize={}",
            connectionName,
            dbType,
            cfg.getMaximumPoolSize());
      }

    } catch (Exception e) {
      throw new DatabaseException(
          String.format(
              "Failed to initialise database connection [%s] for URL: %s",
              connectionName, maskUrl(url)),
          e);
    }
  }

  /**
   * Database-specific optimisations applied at pool creation time. HikariCP passes these directly
   * to the JDBC driver, avoiding per-connection overhead.
   */
  private void applyDatabaseTuning(HikariConfig cfg, DatabaseType type) {
    switch (type) {
      case ORACLE:
        cfg.addDataSourceProperty("oracle.jdbc.implicitStatementCacheSize", "20");
        cfg.addDataSourceProperty("oracle.net.CONNECT_TIMEOUT", "5000");
        break;
      case MSSQL:
        cfg.addDataSourceProperty("loginTimeout", "5");
        cfg.addDataSourceProperty("socketTimeout", "30000");
        break;
      case MYSQL:
        cfg.addDataSourceProperty("cachePrepStmts", "true");
        cfg.addDataSourceProperty("prepStmtCacheSize", "250");
        cfg.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        cfg.addDataSourceProperty("useServerPrepStmts", "true");
        break;
      case POSTGRESQL:
        cfg.addDataSourceProperty("preparedStatementCacheQueries", "256");
        cfg.addDataSourceProperty("tcpKeepAlive", "true");
        break;
    }
  }

  /** Execute SELECT query and return results as list of maps */
  public List<Map<String, Object>> executeQuery(String query, Object... params) {
    log.debug("Executing query: {}", query);
    List<Map<String, Object>> results = new ArrayList<>();

    try (Connection conn = dataSource.getConnection();
        PreparedStatement stmt = conn.prepareStatement(query)) {

      setParameters(stmt, params);

      try (ResultSet rs = stmt.executeQuery()) {
        ResultSetMetaData meta = rs.getMetaData();
        int cols = meta.getColumnCount();
        while (rs.next()) {
          Map<String, Object> row = new HashMap<>();
          for (int i = 1; i <= cols; i++) {
            row.put(meta.getColumnName(i), rs.getObject(i));
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

  /** Execute INSERT, UPDATE, DELETE query */
  public int executeUpdate(String query, Object... params) {
    log.debug("Executing update: {}", query);
    try (Connection conn = dataSource.getConnection();
        PreparedStatement stmt = conn.prepareStatement(query)) {

      setParameters(stmt, params);
      int rows = stmt.executeUpdate();
      log.debug("Update affected {} rows", rows);
      return rows;

    } catch (SQLException e) {
      throw new DatabaseException("Error executing update: " + query, e);
    }
  }

  /** Execute query and return single value */
  public Object executeScalar(String query, Object... params) {
    List<Map<String, Object>> results = executeQuery(query, params);
    if (results.isEmpty()) return null;
    return results.get(0).values().iterator().next();
  }

  /** Execute stored procedure */
  public void executeProcedure(String procedureName, Object... params) {
    log.debug("Executing procedure: {}", procedureName);
    StringBuilder call = new StringBuilder("{call ").append(procedureName).append("(");
    for (int i = 0; i < params.length; i++) {
      call.append("?");
      if (i < params.length - 1) call.append(",");
    }
    call.append(")}");

    try (Connection conn = dataSource.getConnection();
        CallableStatement stmt = conn.prepareCall(call.toString())) {

      setParameters(stmt, params);
      stmt.execute();
      log.debug("Procedure executed: {}", procedureName);

    } catch (SQLException e) {
      throw new DatabaseException("Error executing procedure: " + procedureName, e);
    }
  }

  /** Check if record exists */
  public boolean recordExists(String query, Object... params) {
    return !executeQuery(query, params).isEmpty();
  }

  /** Get record count */
  public int getRecordCount(String tableName, String whereClause) {
    String query =
        "SELECT COUNT(*) FROM "
            + tableName
            + (whereClause != null && !whereClause.isEmpty() ? " WHERE " + whereClause : "");
    Object count = executeScalar(query);
    return count != null ? ((Number) count).intValue() : 0;
  }

  private void setParameters(PreparedStatement stmt, Object... params) throws SQLException {
    for (int i = 0; i < params.length; i++) stmt.setObject(i + 1, params[i]);
  }

  /** Close all database connections */
  public static void closeAll() {
    INSTANCES
        .values()
        .forEach(
            m -> {
              try {
                if (m.dataSource != null && !m.dataSource.isClosed()) {
                  m.dataSource.close();
                  log.info("Database pool closed: {}", m.connectionName);
                }
              } catch (Exception e) {
                log.error("Error closing pool: {}", m.connectionName, e);
              }
            });
    INSTANCES.clear();
  }

  /** Close specific connection */
  public void close() {
    if (dataSource != null && !dataSource.isClosed()) {
      dataSource.close();
      INSTANCES.remove(connectionName);
      log.info("Database pool closed: {}", connectionName);
    }
  }

  @Override
  public String toString() {
    return String.format(
        "DatabaseManager[connection=%s, type=%s]",
        connectionName, dataSource != null ? "initialized" : "uninitialized");
  }

  /** Masks sensitive information in a JDBC URL for safe logging. */
  private String maskUrl(String url) {
    if (url == null) {
      return null;
    }
    // Mask passwords in query parameters: ?password=... or ;password=...
    String masked = url.replaceAll("(?i)(password|pwd|pass)=[^&;]*", "$1=********");

    // Mask credentials in Oracle thin style: user/pass@host
    masked = masked.replaceAll("(?i)(?<=:)[^:/@]+(?=@)", "********");

    return masked;
  }
}
