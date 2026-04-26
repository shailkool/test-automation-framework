package com.smbc.raft.core.runprofile;

import java.nio.file.Path;
import java.nio.file.Paths;
import lombok.extern.log4j.Log4j2;

/**
 * Thread-safe singleton that caches the active {@link RunProfile} for the current JVM.
 *
 * <p>The profile name is resolved once, in order of precedence:
 *
 * <ol>
 *   <li>System property {@code -Dprofile=...}
 *   <li>System property {@code -Drun.profile=...}
 *   <li>Environment variable {@code RUN_PROFILE}
 *   <li>Default: {@code local}
 * </ol>
 *
 * <p>Unlike {@link com.smbc.raft.core.environment.EnvironmentContext}, the run profile is
 * intentionally optional: if no {@code profiles/local.json} is present on the classpath the context
 * falls back to an in-memory default profile so tests that don't care about profiles still work.
 */
@Log4j2
public final class RunProfileContext {

  public static final String PROFILE_SYSTEM_PROPERTY = "profile";
  public static final String PROFILE_SYSTEM_PROPERTY_ALT = "run.profile";
  public static final String PROFILE_ENVIRONMENT_VARIABLE = "RUN_PROFILE";
  public static final String DEFAULT_PROFILE = "local";

  private static volatile RunProfileContext instance;

  private final String profileName;
  private final RunProfile profile;

  private RunProfileContext(String profileName, RunProfile profile) {
    this.profileName = profileName;
    this.profile = profile;
  }

  public static RunProfileContext getInstance() {
    RunProfileContext local = instance;
    if (local == null) {
      synchronized (RunProfileContext.class) {
        local = instance;
        if (local == null) {
          String name = resolveProfileName();
          RunProfile loaded = tryLoad(name);
          local = new RunProfileContext(name, loaded);
          instance = local;
          log.info(
              "RunProfileContext initialised for '{}' "
                  + "(browser={}, headless={}, envDir={}, reportsDir={})",
              name,
              loaded.getBrowser(),
              loaded.isHeadless(),
              loaded.getEnvironmentConfigDir(),
              loaded.getOutputDirs() == null ? null : loaded.getOutputDirs().getReports());
        }
      }
    }
    return local;
  }

  public static synchronized RunProfileContext reload(String profileName) {
    RunProfile loaded = new RunProfileLoader().load(profileName);
    instance = new RunProfileContext(profileName, loaded);
    log.info("RunProfileContext reloaded for '{}'", profileName);
    return instance;
  }

  public static synchronized void reset() {
    instance = null;
  }

  private static String resolveProfileName() {
    String name = System.getProperty(PROFILE_SYSTEM_PROPERTY);
    if (isBlank(name)) {
      name = System.getProperty(PROFILE_SYSTEM_PROPERTY_ALT);
    }
    if (isBlank(name)) {
      name = System.getenv(PROFILE_ENVIRONMENT_VARIABLE);
    }
    if (isBlank(name)) {
      name = DEFAULT_PROFILE;
    }
    return name.trim();
  }

  private static RunProfile tryLoad(String name) {
    try {
      return new RunProfileLoader().load(name);
    } catch (IllegalStateException notFound) {
      log.warn(
          "No run profile '{}' found - falling back to built-in defaults. ({})",
          name,
          notFound.getMessage());
      RunProfile fallback = new RunProfile();
      fallback.setName(name);
      fallback.setBrowser("chromium");
      fallback.setHeadless(true);
      fallback.setTimeout(30_000);
      return fallback;
    }
  }

  private static boolean isBlank(String value) {
    return value == null || value.isBlank();
  }

  public String getProfileName() {
    return profileName;
  }

  public RunProfile getProfile() {
    return profile;
  }

  /**
   * Absolute path to an external directory holding environment JSON files, or {@code null} when the
   * active profile doesn't override the default classpath location.
   */
  public Path getEnvironmentConfigDir() {
    String dir = profile.getEnvironmentConfigDir();
    return isBlank(dir) ? null : Paths.get(dir);
  }

  public Path getReportsDir() {
    return outputDir(d -> d.getReports(), "test-output/reports");
  }

  public Path getLogsDir() {
    return outputDir(d -> d.getLogs(), "test-output/logs");
  }

  public Path getScreenshotsDir() {
    return outputDir(d -> d.getScreenshots(), "test-output/screenshots");
  }

  public Path getArtifactsDir() {
    return outputDir(d -> d.getArtifacts(), "test-output/artifacts");
  }

  private Path outputDir(
      java.util.function.Function<OutputDirs, String> extractor, String fallback) {
    OutputDirs dirs = profile.getOutputDirs();
    String configured = dirs == null ? null : extractor.apply(dirs);
    return Paths.get(isBlank(configured) ? fallback : configured);
  }
}
