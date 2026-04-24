package com.smbc.raft.core.messaging.kafka;

import com.smbc.raft.core.config.ConfigurationManager;
import lombok.Builder;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Resolved Kafka connection settings. Values are read from
 * {@link ConfigurationManager} under either {@code kafka.*} (default client)
 * or {@code kafka.<name>.*} (named client). Arbitrary producer or consumer
 * properties may be passed through using {@code kafka.producer.*} /
 * {@code kafka.consumer.*}.
 */
@Data
@Builder
public class KafkaConfig {

    private final String clientName;
    private final String bootstrapServers;
    private final String groupId;
    private final String clientId;
    private final String securityProtocol;
    private final String saslMechanism;
    private final String saslJaasConfig;
    private final String keySerializer;
    private final String valueSerializer;
    private final String keyDeserializer;
    private final String valueDeserializer;
    private final String autoOffsetReset;
    private final Integer pollTimeoutMs;
    private final Map<String, String> extraProducerProps;
    private final Map<String, String> extraConsumerProps;

    public static KafkaConfig fromConfig(String clientName) {
        ConfigurationManager config = ConfigurationManager.getInstance();
        String prefix = (clientName == null || clientName.equals("default"))
                ? "kafka"
                : "kafka." + clientName;

        String bootstrap = config.getProperty(prefix + ".bootstrap.servers");
        if (bootstrap == null) {
            // fall back to default prefix if a named client was requested but
            // only a single kafka config is defined
            bootstrap = config.getProperty("kafka.bootstrap.servers");
        }

        return KafkaConfig.builder()
                .clientName(clientName == null ? "default" : clientName)
                .bootstrapServers(bootstrap)
                .groupId(config.getProperty(prefix + ".group.id",
                        "test-automation-" + System.currentTimeMillis()))
                .clientId(config.getProperty(prefix + ".client.id", "test-automation-client"))
                .securityProtocol(config.getProperty(prefix + ".security.protocol"))
                .saslMechanism(config.getProperty(prefix + ".sasl.mechanism"))
                .saslJaasConfig(config.getProperty(prefix + ".sasl.jaas.config"))
                .keySerializer(config.getProperty(prefix + ".key.serializer",
                        "org.apache.kafka.common.serialization.StringSerializer"))
                .valueSerializer(config.getProperty(prefix + ".value.serializer",
                        "org.apache.kafka.common.serialization.StringSerializer"))
                .keyDeserializer(config.getProperty(prefix + ".key.deserializer",
                        "org.apache.kafka.common.serialization.StringDeserializer"))
                .valueDeserializer(config.getProperty(prefix + ".value.deserializer",
                        "org.apache.kafka.common.serialization.StringDeserializer"))
                .autoOffsetReset(config.getProperty(prefix + ".auto.offset.reset", "earliest"))
                .pollTimeoutMs(config.getIntProperty(prefix + ".poll.timeout.ms", 5000))
                .extraProducerProps(new HashMap<>())
                .extraConsumerProps(new HashMap<>())
                .build();
    }

    public Properties producerProperties() {
        Properties props = new Properties();
        props.put("bootstrap.servers", bootstrapServers);
        props.put("client.id", clientId);
        props.put("key.serializer", keySerializer);
        props.put("value.serializer", valueSerializer);
        applySecurity(props);
        if (extraProducerProps != null) {
            props.putAll(extraProducerProps);
        }
        return props;
    }

    public Properties consumerProperties() {
        Properties props = new Properties();
        props.put("bootstrap.servers", bootstrapServers);
        props.put("client.id", clientId);
        props.put("group.id", groupId);
        props.put("key.deserializer", keyDeserializer);
        props.put("value.deserializer", valueDeserializer);
        props.put("auto.offset.reset", autoOffsetReset);
        props.put("enable.auto.commit", "true");
        applySecurity(props);
        if (extraConsumerProps != null) {
            props.putAll(extraConsumerProps);
        }
        return props;
    }

    private void applySecurity(Properties props) {
        if (securityProtocol != null) {
            props.put("security.protocol", securityProtocol);
        }
        if (saslMechanism != null) {
            props.put("sasl.mechanism", saslMechanism);
        }
        if (saslJaasConfig != null) {
            props.put("sasl.jaas.config", saslJaasConfig);
        }
    }
}
