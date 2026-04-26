package com.smbc.raft.core.messaging.jms;

import com.smbc.raft.core.exceptions.MessagingException;
import javax.jms.ConnectionFactory;

/**
 * Resolves a {@link ConnectionFactory} for the configured provider. Provider-specific classes are
 * loaded reflectively so that a test run only needs the broker client it actually uses on the
 * classpath.
 */
final class JmsConnectionFactoryProvider {

  private JmsConnectionFactoryProvider() {}

  static ConnectionFactory create(JmsConfig config) {
    switch (config.getProvider()) {
      case ACTIVEMQ:
        return createActiveMq(config);
      case IBM_MQ:
        return createIbmMq(config);
      case CUSTOM:
      default:
        throw new MessagingException(
            "CUSTOM JMS provider requires a ConnectionFactory "
                + "to be passed directly to JmsMessageClient");
    }
  }

  private static ConnectionFactory createActiveMq(JmsConfig config) {
    if (config.getBrokerUrl() == null) {
      throw new MessagingException("ActiveMQ requires jms.broker.url to be set");
    }
    try {
      Class<?> factoryClass = Class.forName("org.apache.activemq.ActiveMQConnectionFactory");
      Object factory = factoryClass.getConstructor(String.class).newInstance(config.getBrokerUrl());
      if (config.getUsername() != null) {
        factoryClass.getMethod("setUserName", String.class).invoke(factory, config.getUsername());
      }
      if (config.getPassword() != null) {
        factoryClass.getMethod("setPassword", String.class).invoke(factory, config.getPassword());
      }
      return (ConnectionFactory) factory;
    } catch (ClassNotFoundException e) {
      throw new MessagingException(
          "ActiveMQ client not on classpath. Add "
              + "org.apache.activemq:activemq-client-jakarta to your dependencies.",
          e);
    } catch (ReflectiveOperationException e) {
      throw new MessagingException("Unable to initialise ActiveMQ connection factory", e);
    }
  }

  private static ConnectionFactory createIbmMq(JmsConfig config) {
    if (config.getMqHost() == null || config.getMqQueueManager() == null) {
      throw new MessagingException("IBM MQ requires jms.mq.host and jms.mq.queue.manager");
    }
    try {
      Class<?> factoryClass = Class.forName("com.ibm.mq.jms.MQConnectionFactory");
      Class<?> constantsClass = Class.forName("com.ibm.mq.jms.JmsConstants");
      Object factory = factoryClass.getDeclaredConstructor().newInstance();

      factoryClass.getMethod("setHostName", String.class).invoke(factory, config.getMqHost());
      factoryClass.getMethod("setPort", int.class).invoke(factory, config.getMqPort());
      factoryClass
          .getMethod("setQueueManager", String.class)
          .invoke(factory, config.getMqQueueManager());
      factoryClass.getMethod("setChannel", String.class).invoke(factory, config.getMqChannel());

      Object transportType =
          constantsClass.getField("WMQ_CM_" + config.getMqTransportType().toUpperCase()).get(null);
      factoryClass.getMethod("setTransportType", int.class).invoke(factory, transportType);

      return (ConnectionFactory) factory;
    } catch (ClassNotFoundException e) {
      throw new MessagingException(
          "IBM MQ client not on classpath. Add "
              + "com.ibm.mq:com.ibm.mq.allclient to your dependencies.",
          e);
    } catch (ReflectiveOperationException e) {
      throw new MessagingException("Unable to initialise IBM MQ connection factory", e);
    }
  }
}
