package com.smbc.raft.core.exceptions;

/** Custom exception for messaging-related errors (Kafka, JMS, MQ). */
public class MessagingException extends RuntimeException {

  public MessagingException(String message) {
    super(message);
  }

  public MessagingException(String message, Throwable cause) {
    super(message, cause);
  }
}
