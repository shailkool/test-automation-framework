package com.automation.core.environment;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Generic database connection settings for a single named database.
 *
 * <p>This POJO is intentionally agnostic of any particular schema or
 * application. Environment-specific JSON files bind into this structure
 * via Jackson, and step definitions pull these values via
 * {@link EnvironmentContext#getDatabase(String)}.
 */
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class DatabaseSettings {

    private String type;
    private String url;
    private String username;
    private String password;
    private String schema;
    private Map<String, Object> properties = new LinkedHashMap<>();
}
