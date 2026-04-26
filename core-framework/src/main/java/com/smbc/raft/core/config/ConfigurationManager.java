package com.smbc.raft.core.config;

import lombok.extern.log4j.Log4j2;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Manages configuration properties for the framework
 */
@Log4j2
public class ConfigurationManager {
    
    private static volatile ConfigurationManager instance;
    private Properties properties;
    private String environment;
    
    private ConfigurationManager() {
        loadProperties();
    }
    
    public static ConfigurationManager getInstance() {
        if (instance == null) {
            synchronized (ConfigurationManager.class) {
                if (instance == null) {
                    instance = new ConfigurationManager();
                }
            }
        }
        return instance;
    }
    
    private void loadProperties() {
        properties = new Properties();
        environment = System.getProperty("env", "qa");
        
        try {
            // Load default properties
            loadPropertyFile("config/default.properties");
            
            // Load environment specific properties
            loadPropertyFile("config/" + environment + ".properties");
            
            // Override with system properties
            properties.putAll(System.getProperties());
            
            log.info("Configuration loaded for environment: {}", environment);
        } catch (IOException e) {
            log.error("Error loading configuration", e);
        }
    }
    
    private void loadPropertyFile(String filename) throws IOException {
        InputStream input = null;
        try {
            // Try loading from classpath
            input = getClass().getClassLoader().getResourceAsStream(filename);
            if (input != null) {
                properties.load(input);
                log.debug("Loaded properties from classpath: {}", filename);
            } else {
                // Try loading from file system
                input = new FileInputStream(filename);
                properties.load(input);
                log.debug("Loaded properties from file system: {}", filename);
            }
        } finally {
            if (input != null) {
                input.close();
            }
        }
    }
    
    public String getProperty(String key) {
        return resolvePlaceholders(properties.getProperty(key));
    }
    
    public String getProperty(String key, String defaultValue) {
        return resolvePlaceholders(properties.getProperty(key, defaultValue));
    }

    /**
     * Resolves placeholders in the form of ${VAR} or ${ENV:VAR} using environment variables.
     * Supports nested placeholders and falls back to system properties.
     */
    private String resolvePlaceholders(String value) {
        if (value == null || !value.contains("${") || !value.contains("}")) {
            return value;
        }

        StringBuilder result = new StringBuilder();
        int lastPos = 0;
        int start;

        while ((start = value.indexOf("${", lastPos)) != -1) {
            result.append(value, lastPos, start);
            int end = value.indexOf("}", start);
            if (end == -1) {
                break;
            }

            String placeholder = value.substring(start + 2, end);
            String lookupKey = placeholder;
            if (placeholder.startsWith("ENV:")) {
                lookupKey = placeholder.substring(4);
            }

            // Priority: Environment Variable -> System Property
            String resolvedValue = System.getenv(lookupKey);
            if (resolvedValue == null) {
                resolvedValue = System.getProperty(lookupKey);
            }

            if (resolvedValue != null) {
                result.append(resolvedValue);
            } else {
                // If not found, keep the placeholder as is
                result.append("${").append(placeholder).append("}");
                log.debug("Placeholder '{}' could not be resolved from ENV or System properties", placeholder);
            }

            lastPos = end + 1;
        }
        result.append(value.substring(lastPos));

        String finalValue = result.toString();
        // Handle recursive resolution if the resolved value itself contains placeholders
        if (finalValue.contains("${") && !finalValue.equals(value)) {
            return resolvePlaceholders(finalValue);
        }
        return finalValue;
    }
    
    public int getIntProperty(String key, int defaultValue) {
        String value = properties.getProperty(key);
        if (value != null) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                log.warn("Invalid integer value for key {}: {}", key, value);
            }
        }
        return defaultValue;
    }
    
    public boolean getBooleanProperty(String key, boolean defaultValue) {
        String value = properties.getProperty(key);
        if (value != null) {
            return Boolean.parseBoolean(value);
        }
        return defaultValue;
    }
    
    public String getEnvironment() {
        return environment;
    }
    
    // Browser configuration
    public String getBrowser() {
        return getProperty("browser", "chromium");
    }
    
    public boolean isHeadless() {
        return getBooleanProperty("headless", false);
    }
    
    public int getTimeout() {
        return getIntProperty("timeout", 30000);
    }
    
    // API configuration
    public String getApiBaseUrl() {
        return getProperty("api.base.url");
    }
    
    public int getApiTimeout() {
        return getIntProperty("api.timeout", 30000);
    }
    
    // Database configuration
    public String getDbType() {
        return getProperty("db.type");
    }
    
    public String getDbUrl() {
        return getProperty("db.url");
    }
    
    public String getDbUsername() {
        return getProperty("db.username");
    }
    
    public String getDbPassword() {
        return getProperty("db.password");
    }

    // Kafka configuration
    public String getKafkaBootstrapServers() {
        return getProperty("kafka.bootstrap.servers");
    }

    public String getKafkaGroupId() {
        return getProperty("kafka.group.id");
    }

    // JMS / MQ configuration
    public String getJmsProvider() {
        return getProperty("jms.provider");
    }

    public String getJmsBrokerUrl() {
        return getProperty("jms.broker.url");
    }

    // Reporting & Metrics
    public String getExtentReportDir() {
        return getProperty("report.extent.dir", "target/extent-reports/");
    }

    public String getMetricsDir() {
        return getProperty("report.metrics.dir", "target/metrics/");
    }

    public boolean isScreenshotOnFailure() {
        return getBooleanProperty("report.screenshots.on.failure", true);
    }

    // Video Recording
    public boolean isVideoEnabled() {
        return getBooleanProperty("playwright.video.enabled", true);
    }

    public String getVideoDir() {
        return getProperty("playwright.video.dir", "target/videos/");
    }
}
