package com.smbc.raft.core.messaging.jms;

/**
 * Supported JMS providers. The framework ships connection-factory wiring for
 * ActiveMQ and IBM MQ; additional providers can be used by supplying a custom
 * {@link javax.jms.ConnectionFactory} directly to {@link JmsMessageClient}.
 */
public enum JmsProvider {
    ACTIVEMQ,
    IBM_MQ,
    CUSTOM;

    public static JmsProvider fromString(String value) {
        if (value == null) {
            return ACTIVEMQ;
        }
        switch (value.trim().toLowerCase()) {
            case "activemq":
                return ACTIVEMQ;
            case "ibm":
            case "ibm_mq":
            case "ibmmq":
            case "mq":
                return IBM_MQ;
            case "custom":
                return CUSTOM;
            default:
                throw new IllegalArgumentException("Unsupported JMS provider: " + value);
        }
    }
}
