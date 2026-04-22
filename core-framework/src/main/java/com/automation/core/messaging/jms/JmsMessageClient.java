package com.automation.core.messaging.jms;

import com.automation.core.exceptions.MessagingException;
import com.automation.core.messaging.Message;
import com.automation.core.messaging.MessageClient;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.jms.Topic;
import lombok.extern.log4j.Log4j2;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

/**
 * {@link MessageClient} backed by JMS — works with ActiveMQ, IBM MQ and any
 * provider you hand in a {@link ConnectionFactory} for. A single JMS session
 * is kept open per client, and producers/consumers are cached per destination.
 */
@Log4j2
public class JmsMessageClient implements MessageClient {

    private final JmsConfig config;
    private final Connection connection;
    private final Session session;
    private final Map<String, MessageProducer> producers = new ConcurrentHashMap<>();
    private final Map<String, MessageConsumer> consumers = new ConcurrentHashMap<>();

    public JmsMessageClient() {
        this(JmsConfig.fromConfig("default"));
    }

    public JmsMessageClient(String clientName) {
        this(JmsConfig.fromConfig(clientName));
    }

    public JmsMessageClient(JmsConfig config) {
        this(config, JmsConnectionFactoryProvider.create(config));
    }

    public JmsMessageClient(JmsConfig config, ConnectionFactory factory) {
        this.config = config;
        try {
            this.connection = (config.getUsername() != null)
                    ? factory.createConnection(config.getUsername(), config.getPassword())
                    : factory.createConnection();
            this.connection.start();
            this.session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            log.info("JMS client [{}] connected ({} / {})",
                    config.getClientName(), config.getProvider(), config.getDestinationType());
        } catch (JMSException e) {
            throw new MessagingException("Failed to create JMS connection: " + config.getClientName(), e);
        }
    }

    @Override
    public void send(Message message) {
        if (message.getDestination() == null) {
            throw new MessagingException("Message destination must not be null");
        }
        try {
            MessageProducer producer = producerFor(message.getDestination());
            TextMessage jmsMessage = session.createTextMessage(message.getPayload());
            if (message.getKey() != null) {
                jmsMessage.setJMSCorrelationID(message.getKey());
            }
            if (message.getHeaders() != null) {
                for (Map.Entry<String, String> entry : message.getHeaders().entrySet()) {
                    jmsMessage.setStringProperty(entry.getKey(), entry.getValue());
                }
            }
            producer.send(jmsMessage);
            message.setMessageId(jmsMessage.getJMSMessageID());
            message.setTimestamp(jmsMessage.getJMSTimestamp());
            log.debug("Sent JMS message to {} (id={})",
                    message.getDestination(), jmsMessage.getJMSMessageID());
        } catch (JMSException e) {
            throw new MessagingException("Failed to send JMS message to "
                    + message.getDestination(), e);
        }
    }

    @Override
    public List<Message> receive(String destination, int maxMessages, Duration timeout) {
        List<Message> received = new ArrayList<>();
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        try {
            MessageConsumer consumer = consumerFor(destination);
            while (received.size() < maxMessages && System.currentTimeMillis() < deadline) {
                long remaining = Math.max(1, deadline - System.currentTimeMillis());
                javax.jms.Message jms = consumer.receive(Math.min(remaining, config.getReceiveTimeoutMs()));
                if (jms == null) {
                    break;
                }
                received.add(toMessage(jms, destination));
            }
            return received;
        } catch (JMSException e) {
            throw new MessagingException("Failed to receive from " + destination, e);
        }
    }

    @Override
    public Message receiveMatching(String destination, Predicate<Message> matcher, Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        try {
            MessageConsumer consumer = consumerFor(destination);
            while (System.currentTimeMillis() < deadline) {
                long remaining = Math.max(1, deadline - System.currentTimeMillis());
                javax.jms.Message jms = consumer.receive(Math.min(remaining, config.getReceiveTimeoutMs()));
                if (jms == null) {
                    continue;
                }
                Message mapped = toMessage(jms, destination);
                if (matcher.test(mapped)) {
                    return mapped;
                }
            }
            return null;
        } catch (JMSException e) {
            throw new MessagingException("Failed to receive from " + destination, e);
        }
    }

    /**
     * Remove any pending messages from the destination. Useful as a test-setup step.
     */
    public int drain(String destination) {
        int drained = 0;
        try {
            MessageConsumer consumer = consumerFor(destination);
            while (consumer.receiveNoWait() != null) {
                drained++;
            }
            log.debug("Drained {} messages from {}", drained, destination);
            return drained;
        } catch (JMSException e) {
            throw new MessagingException("Failed to drain " + destination, e);
        }
    }

    private MessageProducer producerFor(String destinationName) throws JMSException {
        MessageProducer cached = producers.get(destinationName);
        if (cached != null) {
            return cached;
        }
        Destination destination = resolveDestination(destinationName);
        MessageProducer producer = session.createProducer(destination);
        producers.put(destinationName, producer);
        return producer;
    }

    private MessageConsumer consumerFor(String destinationName) throws JMSException {
        MessageConsumer cached = consumers.get(destinationName);
        if (cached != null) {
            return cached;
        }
        Destination destination = resolveDestination(destinationName);
        MessageConsumer consumer = session.createConsumer(destination);
        consumers.put(destinationName, consumer);
        return consumer;
    }

    private Destination resolveDestination(String name) throws JMSException {
        if (config.getDestinationType() == JmsDestinationType.TOPIC) {
            Topic topic = session.createTopic(name);
            return topic;
        }
        Queue queue = session.createQueue(name);
        return queue;
    }

    private Message toMessage(javax.jms.Message jms, String destination) throws JMSException {
        String payload = null;
        if (jms instanceof TextMessage) {
            payload = ((TextMessage) jms).getText();
        }
        Map<String, String> headers = new HashMap<>();
        Enumeration<?> names = jms.getPropertyNames();
        while (names.hasMoreElements()) {
            String propertyName = (String) names.nextElement();
            Object value = jms.getObjectProperty(propertyName);
            headers.put(propertyName, value == null ? null : value.toString());
        }
        return Message.builder()
                .destination(destination)
                .key(jms.getJMSCorrelationID())
                .payload(payload)
                .headers(headers)
                .messageId(jms.getJMSMessageID())
                .timestamp(jms.getJMSTimestamp())
                .build();
    }

    @Override
    public void close() {
        for (MessageProducer producer : producers.values()) {
            try {
                producer.close();
            } catch (JMSException e) {
                log.warn("Error closing JMS producer", e);
            }
        }
        producers.clear();
        for (MessageConsumer consumer : consumers.values()) {
            try {
                consumer.close();
            } catch (JMSException e) {
                log.warn("Error closing JMS consumer", e);
            }
        }
        consumers.clear();
        try {
            session.close();
        } catch (JMSException e) {
            log.warn("Error closing JMS session", e);
        }
        try {
            connection.close();
            log.info("JMS client [{}] closed", config.getClientName());
        } catch (JMSException e) {
            log.warn("Error closing JMS connection [{}]", config.getClientName(), e);
        }
    }
}
