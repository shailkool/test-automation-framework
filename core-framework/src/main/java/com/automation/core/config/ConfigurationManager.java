package com.automation.core.config;

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
    
    private static ConfigurationManager instance;
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
        return properties.getProperty(key);
    }
    
    public String getProperty(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
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
}
