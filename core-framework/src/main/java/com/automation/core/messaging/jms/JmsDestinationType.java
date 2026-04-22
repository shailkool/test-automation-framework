package com.automation.core.messaging.jms;

/**
 * JMS destination types supported by the framework.
 */
public enum JmsDestinationType {
    QUEUE,
    TOPIC;

    public static JmsDestinationType fromString(String value) {
        if (value == null) {
            return QUEUE;
        }
        switch (value.trim().toLowerCase()) {
            case "queue":
                return QUEUE;
            case "topic":
                return TOPIC;
            default:
                throw new IllegalArgumentException("Unsupported destination type: " + value);
        }
    }
}
