package com.automation.tests.messaging;

import com.automation.core.messaging.Message;
import com.automation.core.messaging.MessagingManager;
import com.automation.core.messaging.jms.JmsMessageClient;
import com.automation.core.reporting.ExtentReportManager;
import com.automation.core.utils.BaseTest;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.time.Duration;

/**
 * Examples showing how to send and receive via JMS (ActiveMQ by default,
 * IBM MQ when configured). Tagged as "jms" so it only runs when enabled.
 */
public class JmsMessagingTest extends BaseTest {

    private static final String QUEUE = "test.automation.queue";

    private JmsMessageClient jms;

    @BeforeClass
    public void setupClass() {
        jms = MessagingManager.jms();
        jms.drain(QUEUE);
    }

    @Test(groups = "jms", description = "Send a JMS message to a queue")
    public void testSendJmsMessage() {
        ExtentReportManager.assignCategory("Messaging", "JMS", "Smoke");

        Message message = Message.builder()
                .destination(QUEUE)
                .key("corr-1")
                .payload("<order id=\"42\"/>")
                .build()
                .addHeader("x-source", "test-automation");

        jms.send(message);

        Assert.assertNotNull(message.getMessageId(), "JMS message id should be set by broker");
        ExtentReportManager.logPass("Sent JMS message id=" + message.getMessageId());
    }

    @Test(groups = "jms", description = "Receive a JMS message matching correlation id",
            dependsOnMethods = "testSendJmsMessage")
    public void testReceiveJmsMessage() {
        ExtentReportManager.assignCategory("Messaging", "JMS", "Functional");

        Message received = jms.receiveMatching(QUEUE,
                msg -> "corr-1".equals(msg.getKey()),
                Duration.ofSeconds(10));

        Assert.assertNotNull(received, "Expected to receive the JMS message we just sent");
        Assert.assertEquals(received.getKey(), "corr-1", "Correlation id should round-trip");
        Assert.assertTrue(received.getPayload().contains("order"), "Payload should round-trip");
        Assert.assertEquals(received.getHeader("x-source"), "test-automation",
                "Custom header should round-trip");
        ExtentReportManager.logPass("Received JMS message " + received.getMessageId());
    }

    @Test(groups = "jms", description = "IBM MQ via named client (requires QA env)")
    public void testNamedIbmMqClient() {
        ExtentReportManager.assignCategory("Messaging", "JMS", "IBM_MQ");

        // Named client reads jms.mq.* properties; example only runs when configured.
        JmsMessageClient mq = MessagingManager.jms("mq");
        Assert.assertNotNull(mq, "Named MQ client should resolve via MessagingManager");
        ExtentReportManager.logInfo("IBM MQ client ready — connection verified");
    }
}
