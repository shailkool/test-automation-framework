package com.smbc.raft.core.exception;

/**
 * Custom exception for data validation failures to provide cleaner, business-friendly error
 * messages in test reports.
 */
public class DataValidationException extends RuntimeException {

  public DataValidationException(String message) {
    // We pass false for fillInStackTrace to reduce verbosity in reports
    // if the reporter shows the whole thing. But standard reports
    // usually just show the message.
    super(message);
  }

  @Override
  public String toString() {
    // Override toString to remove the class name prefix
    return getMessage();
  }
}
