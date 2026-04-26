package com.smbc.raft.core.messaging;

import com.smbc.raft.core.messaging.jms.JmsMessageClient;
import com.smbc.raft.core.messaging.kafka.KafkaMessageClient;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.log4j.Log4j2;

/**
 * Registry of reusable Kafka and JMS clients keyed by configuration name. Mirrors {@link
 * com.smbc.raft.core.database.DatabaseManager}: tests ask for a client by name, the first call
 * instantiates it, and {@link #closeAll()} at the end of the suite releases resources.
 */
@Log4j2
public final class MessagingManager {

  private static final Map<String, KafkaMessageClient> KAFKA_CLIENTS = new ConcurrentHashMap<>();
  private static final Map<String, JmsMessageClient> JMS_CLIENTS = new ConcurrentHashMap<>();

  private MessagingManager() {}

  public static KafkaMessageClient kafka() {
    return kafka("default");
  }

  public static KafkaMessageClient kafka(String clientName) {
    return KAFKA_CLIENTS.computeIfAbsent(clientName, KafkaMessageClient::new);
  }

  public static JmsMessageClient jms() {
    return jms("default");
  }

  public static JmsMessageClient jms(String clientName) {
    return JMS_CLIENTS.computeIfAbsent(clientName, JmsMessageClient::new);
  }

  /**
   * Register an externally-built Kafka client (e.g. with an embedded broker for unit tests).
   * Replaces any existing client with the same name.
   */
  public static void registerKafka(String clientName, KafkaMessageClient client) {
    KafkaMessageClient previous = KAFKA_CLIENTS.put(clientName, client);
    if (previous != null) {
      previous.close();
    }
  }

  /** Register an externally-built JMS client. Replaces any existing client. */
  public static void registerJms(String clientName, JmsMessageClient client) {
    JmsMessageClient previous = JMS_CLIENTS.put(clientName, client);
    if (previous != null) {
      previous.close();
    }
  }

  public static void closeKafka(String clientName) {
    KafkaMessageClient client = KAFKA_CLIENTS.remove(clientName);
    if (client != null) {
      client.close();
    }
  }

  public static void closeJms(String clientName) {
    JmsMessageClient client = JMS_CLIENTS.remove(clientName);
    if (client != null) {
      client.close();
    }
  }

  /** Close every registered Kafka and JMS client. */
  public static void closeAll() {
    KAFKA_CLIENTS
        .values()
        .forEach(
            client -> {
              try {
                client.close();
              } catch (Exception e) {
                log.warn("Error closing Kafka client", e);
              }
            });
    KAFKA_CLIENTS.clear();

    JMS_CLIENTS
        .values()
        .forEach(
            client -> {
              try {
                client.close();
              } catch (Exception e) {
                log.warn("Error closing JMS client", e);
              }
            });
    JMS_CLIENTS.clear();
  }
}
