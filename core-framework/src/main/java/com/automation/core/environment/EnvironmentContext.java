package com.automation.core.environment;

import lombok.extern.log4j.Log4j2;

import java.util.Collections;
import java.util.Map;

/**
 * Thread-safe singleton that caches the active {@link EnvironmentConfig} for
 * the current JVM.
 *
 * <p>The active environment name is resolved once, in order of precedence:
 * <ol>
 *   <li>System property {@code -Denv=...} (primary switch the test operator uses)</li>
 *   <li>System property {@code -Dtest.env=...}</li>
 *   <li>Environment variable {@code TEST_ENV}</li>
 *   <li>Default: {@code dev}</li>
 * </ol>
 *
 * <p>On first access the JSON for that environment is loaded from the
 * classpath (typically {@code environments/<env>.json}) and cached. Subsequent
 * calls are constant-time map lookups.
 *
 * <p>Example:
 * <pre>{@code
 * EnvironmentContext ctx = EnvironmentContext.getInstance();
 * WebsiteSettings bbc  = ctx.getWebsite("BBC");
 * UserCredential user  = ctx.getUser("BBC", "reader");
 * DatabaseSettings cms = ctx.getDatabase("contentDb");
 * }</pre>
 */
@Log4j2
public final class EnvironmentContext {

    public static final String ENV_SYSTEM_PROPERTY = "env";
    public static final String ENV_SYSTEM_PROPERTY_ALT = "test.env";
    public static final String ENV_ENVIRONMENT_VARIABLE = "TEST_ENV";
    public static final String DEFAULT_ENVIRONMENT = "dev";

    private static volatile EnvironmentContext instance;

    private final String environmentName;
    private final EnvironmentConfig config;

    private EnvironmentContext(String environmentName, EnvironmentConfig config) {
        this.environmentName = environmentName;
        this.config = config;
    }

    public static EnvironmentContext getInstance() {
        EnvironmentContext local = instance;
        if (local == null) {
            synchronized (EnvironmentContext.class) {
                local = instance;
                if (local == null) {
                    String env = resolveEnvironmentName();
                    EnvironmentConfig loaded = new EnvironmentConfigLoader().load(env);
                    local = new EnvironmentContext(env, loaded);
                    instance = local;
                    log.info("EnvironmentContext initialised for '{}' ({} databases, {} queues, {} websites)",
                        env,
                        loaded.getDatabases().size(),
                        loaded.getMessageQueues().size(),
                        loaded.getWebsites().size());
                }
            }
        }
        return local;
    }

    /**
     * Force a reload against a specific environment name. Primarily intended
     * for tests that need to exercise multiple environments within a single
     * JVM; normal test runs rely on {@link #getInstance()} alone.
     */
    public static synchronized EnvironmentContext reload(String environmentName) {
        EnvironmentConfig loaded = new EnvironmentConfigLoader().load(environmentName);
        instance = new EnvironmentContext(environmentName, loaded);
        log.info("EnvironmentContext reloaded for '{}'", environmentName);
        return instance;
    }

    /** Clears the cached singleton. Next {@link #getInstance()} call re-reads. */
    public static synchronized void reset() {
        instance = null;
    }

    private static String resolveEnvironmentName() {
        String env = System.getProperty(ENV_SYSTEM_PROPERTY);
        if (isBlank(env)) {
            env = System.getProperty(ENV_SYSTEM_PROPERTY_ALT);
        }
        if (isBlank(env)) {
            env = System.getenv(ENV_ENVIRONMENT_VARIABLE);
        }
        if (isBlank(env)) {
            env = DEFAULT_ENVIRONMENT;
        }
        return env.trim();
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public String getEnvironmentName() {
        return environmentName;
    }

    public EnvironmentConfig getConfig() {
        return config;
    }

    public DatabaseSettings getDatabase(String name) {
        DatabaseSettings db = config.getDatabases().get(name);
        if (db == null) {
            throw new IllegalArgumentException(String.format(
                "No database named '%s' defined for environment '%s'. Available: %s",
                name, environmentName, config.getDatabases().keySet()));
        }
        return db;
    }

    public Map<String, DatabaseSettings> getDatabases() {
        return Collections.unmodifiableMap(config.getDatabases());
    }

    public MessageQueueSettings getMessageQueue(String name) {
        MessageQueueSettings mq = config.getMessageQueues().get(name);
        if (mq == null) {
            throw new IllegalArgumentException(String.format(
                "No message queue named '%s' defined for environment '%s'. Available: %s",
                name, environmentName, config.getMessageQueues().keySet()));
        }
        return mq;
    }

    public Map<String, MessageQueueSettings> getMessageQueues() {
        return Collections.unmodifiableMap(config.getMessageQueues());
    }

    public WebsiteSettings getWebsite(String name) {
        WebsiteSettings site = config.getWebsites().get(name);
        if (site == null) {
            throw new IllegalArgumentException(String.format(
                "No website named '%s' defined for environment '%s'. Available: %s",
                name, environmentName, config.getWebsites().keySet()));
        }
        return site;
    }

    public Map<String, WebsiteSettings> getWebsites() {
        return Collections.unmodifiableMap(config.getWebsites());
    }

    public UserCredential getUser(String websiteName, String userKey) {
        WebsiteSettings site = getWebsite(websiteName);
        UserCredential user = site.getUsers().get(userKey);
        if (user == null) {
            throw new IllegalArgumentException(String.format(
                "No user '%s' on website '%s' for environment '%s'. Available users: %s",
                userKey, websiteName, environmentName, site.getUsers().keySet()));
        }
        return user;
    }
}
