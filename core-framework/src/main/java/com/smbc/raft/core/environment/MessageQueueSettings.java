package com.smbc.raft.core.environment;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Generic message-queue connection settings for a single named queue/topic.
 *
 * <p>Supports any provider (Kafka, ActiveMQ, IBM MQ, RabbitMQ, etc.) via the {@code provider} field
 * and a free-form {@code properties} map for provider-specific knobs.
 */
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class MessageQueueSettings {

  private String provider;
  private String brokerUrl;
  private String destination;
  private String destinationType;
  private String username;
  private String password;
  private Map<String, Object> properties = new LinkedHashMap<>();
}
