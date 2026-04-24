package com.smbc.raft.app.messaging;

import com.smbc.raft.core.messaging.Message;
import com.smbc.raft.core.messaging.MessagingManager;
import com.smbc.raft.core.messaging.kafka.KafkaMessageClient;
import com.smbc.raft.core.reporting.ExtentReportManager;
import lombok.extern.log4j.Log4j2;

import java.time.Duration;
import java.util.List;

/**
 * Application-specific helper demonstrating how to publish and verify
 * user-domain events on a Kafka topic via the core messaging layer.
 */
@Log4j2
public class UserEventPublisher {

    public static final String DEFAULT_TOPIC = "user.events";

    private final KafkaMessageClient client;
    private final String topic;

    public UserEventPublisher() {
        this(DEFAULT_TOPIC);
    }

    public UserEventPublisher(String topic) {
        this.client = MessagingManager.kafka();
        this.topic = topic;
    }

    public UserEventPublisher(String clientName, String topic) {
        this.client = MessagingManager.kafka(clientName);
        this.topic = topic;
    }

    /**
     * Publish a user-created event.
     */
    public Message publishUserCreated(int userId, String email) {
        Message message = Message.builder()
                .destination(topic)
                .key(String.valueOf(userId))
                .payload(String.format("{\"type\":\"USER_CREATED\",\"id\":%d,\"email\":\"%s\"}",
                        userId, email))
                .build()
                .addHeader("eventType", "USER_CREATED")
                .addHeader("source", "test-automation");

        client.send(message);
        ExtentReportManager.logInfo("Published USER_CREATED event for " + email);
        log.info("Published USER_CREATED event (userId={}, offset={})",
                userId, message.getOffset());
        return message;
    }

    /**
     * Drain recent events matching the provided user id.
     */
    public List<Message> readEvents(int maxMessages, Duration timeout) {
        return client.receive(topic, maxMessages, timeout);
    }

    /**
     * Block until an event for this userId is observed or the timeout elapses.
     */
    public Message awaitEventForUser(int userId, Duration timeout) {
        return client.receiveMatching(topic,
                msg -> String.valueOf(userId).equals(msg.getKey()),
                timeout);
    }
}
