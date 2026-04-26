package com.smbc.raft.core.environment;

import com.smbc.raft.core.runprofile.RunProfileContext;
import java.nio.file.Path;
import lombok.extern.log4j.Log4j2;

/**
 * Minimalist singleton for accessing environment-specific configurations. Loads the JSON for the
 * active environment (resolved via 'env' property or 'TEST_ENV' variable) and provides direct
 * access to the {@link EnvironmentConfig} structure.
 */
@Log4j2
public final class EnvironmentContext {

  private static volatile EnvironmentConfig config;

  private EnvironmentContext() {}

  /**
   * Retrieves the active environment configuration. Uses double-checked locking for thread-safe
   * initialization.
   */
  public static EnvironmentConfig get() {
    if (config == null) {
      synchronized (EnvironmentContext.class) {
        if (config == null) {
          config = loadConfig();
        }
      }
    }
    return config;
  }

  private static EnvironmentConfig loadConfig() {
    String env =
        System.getProperty(
                "env",
                System.getProperty("test.env", System.getenv().getOrDefault("TEST_ENV", "dev")))
            .trim();
    EnvironmentConfigLoader loader = new EnvironmentConfigLoader();
    Path externalDir = null;

    try {
      externalDir = RunProfileContext.getInstance().getEnvironmentConfigDir();
    } catch (Exception e) {
      log.debug(
          "RunProfileContext unavailable, falling back to classpath for environment: {}", env);
    }

    EnvironmentConfig loaded =
        (externalDir != null) ? loader.loadFromDir(env, externalDir) : loader.load(env);

    log.info(
        "Initialised Environment: '{}' ({} databases, {} websites)",
        env,
        loaded.getDatabases().size(),
        loaded.getWebsites().size());
    return loaded;
  }

  /** Clears the cache, forcing a reload on the next access. */
  public static synchronized void reset() {
    config = null;
  }
}
