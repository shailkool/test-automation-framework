package com.smbc.raft.core.messaging.kafka;

import com.smbc.raft.core.exceptions.MessagingException;
import com.smbc.raft.core.messaging.Message;
import lombok.extern.log4j.Log4j2;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.internals.RecordHeader;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Thin wrapper around {@link KafkaProducer} that accepts {@link Message}
 * values and returns the broker-assigned metadata back onto the message.
 */
@Log4j2
public class KafkaProducerClient implements AutoCloseable {

    private final KafkaConfig config;
    private final KafkaProducer<String, String> producer;

    public KafkaProducerClient() {
        this(KafkaConfig.fromConfig("default"));
    }

    public KafkaProducerClient(String clientName) {
        this(KafkaConfig.fromConfig(clientName));
    }

    public KafkaProducerClient(KafkaConfig config) {
        this.config = config;
        try {
            this.producer = new KafkaProducer<>(config.producerProperties());
            log.info("Kafka producer [{}] connected to {}",
                    config.getClientName(), config.getBootstrapServers());
        } catch (Exception e) {
            throw new MessagingException("Failed to create Kafka producer: " + config.getClientName(), e);
        }
    }

    /**
     * Send a message synchronously and populate the broker-assigned fields
     * (partition, offset, timestamp) on the original {@link Message}.
     */
    public Message send(Message message) {
        return send(message, 30, TimeUnit.SECONDS);
    }

    public Message send(Message message, long timeout, TimeUnit unit) {
        if (message.getDestination() == null) {
            throw new MessagingException("Message destination (topic) must not be null");
        }

        List<Header> headers = new ArrayList<>();
        if (message.getHeaders() != null) {
            for (Map.Entry<String, String> entry : message.getHeaders().entrySet()) {
                headers.add(new RecordHeader(entry.getKey(),
                        entry.getValue().getBytes(StandardCharsets.UTF_8)));
            }
        }

        ProducerRecord<String, String> record = new ProducerRecord<>(
                message.getDestination(),
                null,
                message.getTimestamp(),
                message.getKey(),
                message.getPayload(),
                headers);

        try {
            Future<RecordMetadata> future = producer.send(record);
            RecordMetadata metadata = future.get(timeout, unit);
            message.setPartition(metadata.partition());
            message.setOffset(metadata.offset());
            message.setTimestamp(metadata.timestamp());
            log.debug("Sent Kafka record to {}-{}@{}",
                    metadata.topic(), metadata.partition(), metadata.offset());
            return message;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new MessagingException("Interrupted while sending Kafka message", e);
        } catch (ExecutionException | TimeoutException e) {
            throw new MessagingException("Failed to send Kafka message to "
                    + message.getDestination(), e);
        }
    }

    /**
     * Flush any buffered records.
     */
    public void flush() {
        producer.flush();
    }

    @Override
    public void close() {
        try {
            producer.flush();
            producer.close();
            log.info("Kafka producer [{}] closed", config.getClientName());
        } catch (Exception e) {
            log.warn("Error closing Kafka producer [{}]", config.getClientName(), e);
        }
    }
}
