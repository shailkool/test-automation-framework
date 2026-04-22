package com.automation.core.messaging;

import java.time.Duration;
import java.util.List;
import java.util.function.Predicate;

/**
 * Common abstraction over Kafka and JMS/MQ clients so tests can treat brokers
 * interchangeably when the test intent is transport-agnostic.
 */
public interface MessageClient extends AutoCloseable {

    /**
     * Send a message to the destination configured on the {@link Message}.
     */
    void send(Message message);

    /**
     * Poll for messages from the destination until either {@code maxMessages}
     * are received or {@code timeout} elapses.
     */
    List<Message> receive(String destination, int maxMessages, Duration timeout);

    /**
     * Receive the first message matching the predicate, or return null when
     * none arrives within the timeout.
     */
    Message receiveMatching(String destination, Predicate<Message> matcher, Duration timeout);

    @Override
    void close();
}
