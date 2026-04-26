package com.smbc.raft.core.exceptions;

/** Custom exception for database-related errors */
public class DatabaseException extends RuntimeException {

  public DatabaseException(String message) {
    super(message);
  }

  public DatabaseException(String message, Throwable cause) {
    super(message, cause);
  }
}
