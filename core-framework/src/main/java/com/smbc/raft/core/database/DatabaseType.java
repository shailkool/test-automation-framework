package com.smbc.raft.core.database;

/** Supported database types */
public enum DatabaseType {
  ORACLE("oracle.jdbc.OracleDriver"),
  MSSQL("com.microsoft.sqlserver.jdbc.SQLServerDriver"),
  MYSQL("com.mysql.cj.jdbc.Driver"),
  POSTGRESQL("org.postgresql.Driver");

  private final String driverClassName;

  DatabaseType(String driverClassName) {
    this.driverClassName = driverClassName;
  }

  public String getDriverClassName() {
    return driverClassName;
  }

  public static DatabaseType fromString(String type) {
    for (DatabaseType dbType : DatabaseType.values()) {
      if (dbType.name().equalsIgnoreCase(type)) {
        return dbType;
      }
    }
    throw new IllegalArgumentException("Unknown database type: " + type);
  }
}
