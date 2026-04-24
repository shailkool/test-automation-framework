package com.smbc.raft.core.messaging;

import com.smbc.raft.core.messaging.jms.JmsMessageClient;
import com.smbc.raft.core.messaging.kafka.KafkaMessageClient;
import lombok.extern.log4j.Log4j2;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Registry of reusable Kafka and JMS clients keyed by configuration name.
 * Mirrors {@link com.smbc.raft.core.database.DatabaseManager}: tests ask for
 * a client by name, the first call instantiates it, and
 * {@link #closeAll()} at the end of the suite releases resources.
 */
@Log4j2
public final class MessagingManager {

    private static final ConcurrentMap<String, KafkaMessageClient> kafkaClients = new ConcurrentHashMap<>();
    private static final ConcurrentMap<String, JmsMessageClient> jmsClients = new ConcurrentHashMap<>();

    private MessagingManager() {
    }

    public static KafkaMessageClient kafka() {
        return kafka("default");
    }

    public static KafkaMessageClient kafka(String clientName) {
        return kafkaClients.computeIfAbsent(clientName, KafkaMessageClient::new);
    }

    public static JmsMessageClient jms() {
        return jms("default");
    }

    public static JmsMessageClient jms(String clientName) {
        return jmsClients.computeIfAbsent(clientName, JmsMessageClient::new);
    }

    /**
     * Register an externally-built Kafka client (e.g. with an embedded broker
     * for unit tests). Replaces any existing client with the same name.
     */
    public static void registerKafka(String clientName, KafkaMessageClient client) {
        KafkaMessageClient previous = kafkaClients.put(clientName, client);
        if (previous != null) {
            previous.close();
        }
    }

    /**
     * Register an externally-built JMS client. Replaces any existing client.
     */
    public static void registerJms(String clientName, JmsMessageClient client) {
        JmsMessageClient previous = jmsClients.put(clientName, client);
        if (previous != null) {
            previous.close();
        }
    }

    public static void closeKafka(String clientName) {
        KafkaMessageClient client = kafkaClients.remove(clientName);
        if (client != null) {
            client.close();
        }
    }

    public static void closeJms(String clientName) {
        JmsMessageClient client = jmsClients.remove(clientName);
        if (client != null) {
            client.close();
        }
    }

    /**
     * Close every registered Kafka and JMS client.
     */
    public static void closeAll() {
        kafkaClients.values().forEach(client -> {
            try {
                client.close();
            } catch (Exception e) {
                log.warn("Error closing Kafka client", e);
            }
        });
        kafkaClients.clear();

        jmsClients.values().forEach(client -> {
            try {
                client.close();
            } catch (Exception e) {
                log.warn("Error closing JMS client", e);
            }
        });
        jmsClients.clear();
    }
}
