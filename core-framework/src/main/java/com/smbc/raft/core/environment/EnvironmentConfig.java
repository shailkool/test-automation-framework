package com.smbc.raft.core.environment;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Root POJO for a single environment's JSON configuration (e.g. dev.json, qa.json, uat2.json).
 * Groups named databases, message queues and websites so that a test targeting {@code dev} has
 * isolated access to exactly the connections provisioned for dev.
 *
 * <p>Deliberately agnostic of any specific application. Application-specific test projects author
 * the JSON files; the core only knows how to bind them.
 */
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class EnvironmentConfig {

  private String name;
  private String description;
  private Map<String, DatabaseSettings> databases = new LinkedHashMap<>();
  private Map<String, MessageQueueSettings> messageQueues = new LinkedHashMap<>();
  private Map<String, WebsiteSettings> websites = new LinkedHashMap<>();
  private Map<String, ServiceSettings> services = new LinkedHashMap<>();
  private Map<String, FilePathSettings> filePaths = new LinkedHashMap<>();
  private Map<String, AutosysJobSettings> autosys = new LinkedHashMap<>();
  private Map<String, String> customProperties = new LinkedHashMap<>();
  private Map<String, Object> properties = new LinkedHashMap<>();
}
