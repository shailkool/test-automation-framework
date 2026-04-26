package com.smbc.raft.core.messaging.kafka;

import com.smbc.raft.core.messaging.Message;
import com.smbc.raft.core.messaging.MessageClient;
import java.time.Duration;
import java.util.List;
import java.util.function.Predicate;
import lombok.extern.log4j.Log4j2;

/**
 * {@link MessageClient} facade that pairs a Kafka producer and a Kafka consumer, allowing tests to
 * hold a single handle for send + receive flows.
 */
@Log4j2
public class KafkaMessageClient implements MessageClient {

  private final KafkaProducerClient producer;
  private final KafkaConsumerClient consumer;

  public KafkaMessageClient() {
    this("default");
  }

  public KafkaMessageClient(String clientName) {
    KafkaConfig config = KafkaConfig.fromConfig(clientName);
    this.producer = new KafkaProducerClient(config);
    this.consumer = new KafkaConsumerClient(config);
  }

  public KafkaMessageClient(KafkaProducerClient producer, KafkaConsumerClient consumer) {
    this.producer = producer;
    this.consumer = consumer;
  }

  public KafkaProducerClient producer() {
    return producer;
  }

  public KafkaConsumerClient consumer() {
    return consumer;
  }

  @Override
  public void send(Message message) {
    producer.send(message);
  }

  @Override
  public List<Message> receive(String destination, int maxMessages, Duration timeout) {
    return consumer.poll(destination, maxMessages, timeout);
  }

  @Override
  public Message receiveMatching(String destination, Predicate<Message> matcher, Duration timeout) {
    return consumer.pollMatching(destination, matcher, timeout);
  }

  @Override
  public void close() {
    try {
      producer.close();
    } finally {
      consumer.close();
    }
  }
}
