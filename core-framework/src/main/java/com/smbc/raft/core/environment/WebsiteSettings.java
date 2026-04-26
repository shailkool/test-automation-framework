package com.smbc.raft.core.environment;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Generic website settings for one named site under test.
 *
 * <p>Each site carries its own user base ({@link #users}) so tests can authenticate as a specific
 * role against a specific site without leaking credentials across sites.
 */
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class WebsiteSettings {

  private String baseUrl;
  private String description;
  private Map<String, UserCredential> users = new LinkedHashMap<>();
  private Map<String, Object> properties = new LinkedHashMap<>();
}
