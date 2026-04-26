package com.smbc.raft.tests.messaging;

import com.smbc.raft.core.messaging.Message;
import com.smbc.raft.core.messaging.MessagingManager;
import com.smbc.raft.core.messaging.kafka.KafkaMessageClient;
import com.smbc.raft.core.reporting.ExtentReportManager;
import com.smbc.raft.core.utils.BaseTest;
import java.time.Duration;
import java.util.List;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Examples showing how to produce and consume Kafka messages through the framework. Tagged as
 * "kafka" so runs against a live broker are opt-in.
 */
public class KafkaMessagingTest extends BaseTest {

  private static final String TOPIC = "test-automation.user.events";

  private KafkaMessageClient kafka;

  @BeforeClass
  public void setupClass() {
    kafka = MessagingManager.kafka();
  }

  @Test(
      groups = {"kafka", "nightly"},
      description = "Produce a single Kafka record and assert metadata")
  public void testProduceKafkaRecord() {
    ExtentReportManager.assignCategory("Messaging", "Kafka", "Smoke");

    Message message =
        Message.builder()
            .destination(TOPIC)
            .key("user-1")
            .payload("{\"id\":1,\"name\":\"Alice\"}")
            .build()
            .addHeader("eventType", "USER_CREATED");

    kafka.send(message);

    Assert.assertNotNull(message.getOffset(), "Broker should have assigned an offset");
    Assert.assertNotNull(message.getPartition(), "Broker should have assigned a partition");
    ExtentReportManager.logPass("Produced Kafka record offset=" + message.getOffset());
  }

  @Test(
      groups = {"kafka", "nightly"},
      description = "Produce then consume matches on the same topic",
      dependsOnMethods = "testProduceKafkaRecord")
  public void testConsumeKafkaRecord() {
    ExtentReportManager.assignCategory("Messaging", "Kafka", "Functional");

    String correlationKey = "consume-" + System.currentTimeMillis();
    kafka.send(
        Message.builder()
            .destination(TOPIC)
            .key(correlationKey)
            .payload("{\"status\":\"OK\"}")
            .build());

    Message received =
        kafka.receiveMatching(
            TOPIC, msg -> correlationKey.equals(msg.getKey()), Duration.ofSeconds(10));

    Assert.assertNotNull(received, "Expected to receive the Kafka record we just produced");
    Assert.assertEquals(received.getKey(), correlationKey, "Key should round-trip");
    Assert.assertTrue(received.getPayload().contains("OK"), "Payload should round-trip");
    ExtentReportManager.logPass("Consumed record at offset " + received.getOffset());
  }

  @Test(
      groups = {"kafka", "nightly"},
      description = "Drain multiple records up to a bound")
  public void testBatchPoll() {
    ExtentReportManager.assignCategory("Messaging", "Kafka", "Functional");

    for (int i = 0; i < 3; i++) {
      kafka.send(
          Message.builder()
              .destination(TOPIC)
              .key("batch-" + i)
              .payload("{\"batch\":" + i + "}")
              .build());
    }

    List<Message> batch = kafka.receive(TOPIC, 3, Duration.ofSeconds(10));
    Assert.assertTrue(batch.size() >= 1, "Should receive at least one record");
    ExtentReportManager.logPass("Received " + batch.size() + " records");
  }
}
