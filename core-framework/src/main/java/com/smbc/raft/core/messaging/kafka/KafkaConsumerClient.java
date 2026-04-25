package com.smbc.raft.core.messaging.kafka;

import com.smbc.raft.core.exceptions.MessagingException;
import com.smbc.raft.core.messaging.Message;
import lombok.extern.log4j.Log4j2;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.header.Header;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Wrapper around {@link KafkaConsumer} that subscribes on demand and drains
 * records into framework {@link Message} objects.
 */
@Log4j2
public class KafkaConsumerClient implements AutoCloseable {

    private final KafkaConfig config;
    private final KafkaConsumer<String, String> consumer;
    private String subscribedTopic;

    public KafkaConsumerClient() {
        this(KafkaConfig.fromConfig("default"));
    }

    public KafkaConsumerClient(String clientName) {
        this(KafkaConfig.fromConfig(clientName));
    }

    public KafkaConsumerClient(KafkaConfig config) {
        this.config = config;
        try {
            this.consumer = new KafkaConsumer<>(config.consumerProperties());
            log.info("Kafka consumer [{}] connected to {} (group: {})",
                    config.getClientName(), config.getBootstrapServers(), config.getGroupId());
        } catch (Exception e) {
            throw new MessagingException("Failed to create Kafka consumer: " + config.getClientName(), e);
        }
    }

    /**
     * Subscribe to a topic. Re-subscribes if a different topic was previously active.
     */
    public KafkaConsumerClient subscribe(String topic) {
        if (topic == null) {
            throw new MessagingException("Topic must not be null");
        }
        if (!topic.equals(subscribedTopic)) {
            consumer.subscribe(Collections.singletonList(topic));
            subscribedTopic = topic;
            log.info("Kafka consumer [{}] subscribed to topic: {}", config.getClientName(), topic);
        }
        return this;
    }

    /**
     * Poll the configured topic until at least {@code maxMessages} have been
     * received or {@code timeout} elapses. Returns whatever was drained.
     */
    public List<Message> poll(String topic, int maxMessages, Duration timeout) {
        subscribe(topic);
        List<Message> received = new ArrayList<>();
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        Duration perPoll = Duration.ofMillis(Math.min(config.getPollTimeoutMs(), timeout.toMillis()));

        while (received.size() < maxMessages && System.currentTimeMillis() < deadline) {
            ConsumerRecords<String, String> records = consumer.poll(perPoll);
            for (ConsumerRecord<String, String> record : records) {
                received.add(toMessage(record));
                if (received.size() >= maxMessages) {
                    break;
                }
            }
        }

        log.debug("Kafka consumer [{}] polled {} records from {}",
                config.getClientName(), received.size(), topic);
        return received;
    }

    /**
     * Poll until a record matching {@code matcher} is found or the timeout elapses.
     */
    public Message pollMatching(String topic, Predicate<Message> matcher, Duration timeout) {
        subscribe(topic);
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        Duration perPoll = Duration.ofMillis(Math.min(config.getPollTimeoutMs(), timeout.toMillis()));

        while (System.currentTimeMillis() < deadline) {
            ConsumerRecords<String, String> records = consumer.poll(perPoll);
            for (ConsumerRecord<String, String> record : records) {
                Message msg = toMessage(record);
                if (matcher.test(msg)) {
                    return msg;
                }
            }
        }
        return null;
    }

    /**
     * Reset this consumer group to the earliest offset for the subscribed topic.
     * Useful to replay from the beginning during a test.
     */
    public void seekToBeginning() {
        if (subscribedTopic == null) {
            throw new MessagingException("Consumer is not subscribed to any topic");
        }
        consumer.poll(Duration.ofMillis(0)); // trigger partition assignment
        consumer.seekToBeginning(consumer.assignment());
    }

    private Message toMessage(ConsumerRecord<String, String> record) {
        Map<String, String> headers = new HashMap<>();
        for (Header header : record.headers()) {
            headers.put(header.key(),
                    header.value() == null ? null : new String(header.value(), StandardCharsets.UTF_8));
        }
        return Message.builder()
                .destination(record.topic())
                .key(record.key())
                .payload(record.value())
                .headers(headers)
                .partition(record.partition())
                .offset(record.offset())
                .timestamp(record.timestamp())
                .messageId(record.topic() + "-" + record.partition() + "@" + record.offset())
                .build();
    }

    @Override
    public void close() {
        try {
            consumer.close();
            log.info("Kafka consumer [{}] closed", config.getClientName());
        } catch (Exception e) {
            log.warn("Error closing Kafka consumer [{}]", config.getClientName(), e);
        }
    }
}
