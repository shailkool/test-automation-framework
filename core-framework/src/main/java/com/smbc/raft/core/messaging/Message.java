package com.smbc.raft.core.messaging;

import java.util.HashMap;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Transport-neutral message representation that flows through both Kafka and JMS/MQ clients, so
 * tests can assert against a single shape regardless of which broker produced the payload.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Message {

  /** Destination (topic, queue, or topic name). */
  private String destination;

  /** Optional correlation / routing key (Kafka key or JMS correlationId). */
  private String key;

  /** Payload body, typically JSON or plain text. */
  private String payload;

  /** Headers / record metadata carried alongside the payload. */
  @Builder.Default private Map<String, String> headers = new HashMap<>();

  /** Broker-specific identifier (Kafka offset, JMS message ID). */
  private String messageId;

  /** Kafka partition (null for JMS). */
  private Integer partition;

  /** Kafka offset (null for JMS). */
  private Long offset;

  /** Broker timestamp, millis since epoch. */
  private Long timestamp;

  public Message addHeader(String name, String value) {
    if (headers == null) {
      headers = new HashMap<>();
    }
    headers.put(name, value);
    return this;
  }

  public String getHeader(String name) {
    return headers == null ? null : headers.get(name);
  }
}
