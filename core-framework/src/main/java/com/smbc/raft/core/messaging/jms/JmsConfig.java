package com.smbc.raft.core.messaging.jms;

import com.smbc.raft.core.config.ConfigurationManager;
import lombok.Builder;
import lombok.Data;

/**
 * Resolved JMS connection settings. Read from {@link ConfigurationManager} under {@code jms.*}
 * (default) or {@code jms.<name>.*}.
 *
 * <p>For IBM MQ, additional properties are read: {@code jms.mq.queueManager}, {@code
 * jms.mq.channel}, {@code jms.mq.host}, {@code jms.mq.port}, {@code jms.mq.transportType}.
 */
@Data
@Builder
public class JmsConfig {

  private final String clientName;
  private final JmsProvider provider;
  private final JmsDestinationType destinationType;

  // Generic (ActiveMQ / custom URL-based providers)
  private final String brokerUrl;
  private final String username;
  private final String password;

  // IBM MQ specific
  private final String mqHost;
  private final Integer mqPort;
  private final String mqQueueManager;
  private final String mqChannel;
  private final String mqTransportType;

  private final Integer receiveTimeoutMs;

  public static JmsConfig fromConfig(String clientName) {
    ConfigurationManager config = ConfigurationManager.getInstance();
    String prefix =
        (clientName == null || clientName.equals("default")) ? "jms" : "jms." + clientName;

    return JmsConfig.builder()
        .clientName(clientName == null ? "default" : clientName)
        .provider(JmsProvider.fromString(config.getProperty(prefix + ".provider", "activemq")))
        .destinationType(
            JmsDestinationType.fromString(
                config.getProperty(prefix + ".destination.type", "queue")))
        .brokerUrl(config.getProperty(prefix + ".broker.url"))
        .username(config.getProperty(prefix + ".username"))
        .password(config.getProperty(prefix + ".password"))
        .mqHost(config.getProperty(prefix + ".mq.host"))
        .mqPort(config.getIntProperty(prefix + ".mq.port", 1414))
        .mqQueueManager(config.getProperty(prefix + ".mq.queue.manager"))
        .mqChannel(config.getProperty(prefix + ".mq.channel", "SYSTEM.DEF.SVRCONN"))
        .mqTransportType(config.getProperty(prefix + ".mq.transport.type", "CLIENT"))
        .receiveTimeoutMs(config.getIntProperty(prefix + ".receive.timeout.ms", 5000))
        .build();
  }
}
