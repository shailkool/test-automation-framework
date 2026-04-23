package com.automation.core.environment;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.log4j.Log4j2;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Locates and deserializes an {@link EnvironmentConfig} JSON document.
 *
 * <p>Resolution order for the JSON source:
 * <ol>
 *   <li>Explicit absolute / relative filesystem path passed to {@link #load(String, String)}</li>
 *   <li>Classpath resource at {@code <baseDir>/<env>.json} (default baseDir is {@code environments})</li>
 *   <li>Filesystem file at {@code <baseDir>/<env>.json}</li>
 * </ol>
 *
 * <p>The loader is stateless; {@link EnvironmentContext} layers singleton
 * caching on top.
 */
@Log4j2
public class EnvironmentConfigLoader {

    public static final String DEFAULT_BASE_DIR = "environments";

    private final ObjectMapper mapper;

    public EnvironmentConfigLoader() {
        this.mapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public EnvironmentConfig load(String environmentName) {
        return load(environmentName, DEFAULT_BASE_DIR);
    }

    public EnvironmentConfig load(String environmentName, String baseDir) {
        if (environmentName == null || environmentName.isBlank()) {
            throw new IllegalArgumentException("environmentName must be provided");
        }

        String resource = baseDir + "/" + environmentName + ".json";

        InputStream classpathStream = Thread.currentThread()
            .getContextClassLoader()
            .getResourceAsStream(resource);
        if (classpathStream != null) {
            try (InputStream in = classpathStream) {
                EnvironmentConfig config = mapper.readValue(in, EnvironmentConfig.class);
                applyDefaults(config, environmentName);
                log.info("Loaded environment '{}' from classpath resource '{}'",
                    environmentName, resource);
                return config;
            } catch (IOException e) {
                throw new IllegalStateException(
                    "Failed to parse environment config from classpath '" + resource + "'", e);
            }
        }

        Path fsPath = Paths.get(resource);
        if (Files.exists(fsPath)) {
            try {
                EnvironmentConfig config = mapper.readValue(fsPath.toFile(), EnvironmentConfig.class);
                applyDefaults(config, environmentName);
                log.info("Loaded environment '{}' from filesystem '{}'",
                    environmentName, fsPath.toAbsolutePath());
                return config;
            } catch (IOException e) {
                throw new IllegalStateException(
                    "Failed to parse environment config from file '" + fsPath.toAbsolutePath() + "'", e);
            }
        }

        throw new IllegalStateException(String.format(
            "Environment config '%s.json' not found on classpath ('%s') or filesystem ('%s'). "
                + "Create the file under src/test/resources/%s/ or pass an explicit path.",
            environmentName, resource, fsPath.toAbsolutePath(), baseDir));
    }

    public EnvironmentConfig loadFromPath(Path jsonFile) {
        try {
            EnvironmentConfig config = mapper.readValue(jsonFile.toFile(), EnvironmentConfig.class);
            applyDefaults(config, jsonFile.getFileName().toString().replace(".json", ""));
            log.info("Loaded environment config from '{}'", jsonFile.toAbsolutePath());
            return config;
        } catch (IOException e) {
            throw new IllegalStateException(
                "Failed to parse environment config from '" + jsonFile + "'", e);
        }
    }

    /**
     * Load {@code <environmentName>.json} from a specific filesystem directory.
     * Used when a run profile points the loader at an external location (e.g.
     * a secure volume mounted outside the repository).
     */
    public EnvironmentConfig loadFromDir(String environmentName, Path directory) {
        if (environmentName == null || environmentName.isBlank()) {
            throw new IllegalArgumentException("environmentName must be provided");
        }
        if (directory == null) {
            throw new IllegalArgumentException("directory must be provided");
        }
        Path jsonFile = directory.resolve(environmentName + ".json");
        if (!Files.exists(jsonFile)) {
            throw new IllegalStateException(String.format(
                "Environment config '%s.json' not found in external directory '%s'",
                environmentName, directory.toAbsolutePath()));
        }
        return loadFromPath(jsonFile);
    }

    private void applyDefaults(EnvironmentConfig config, String environmentName) {
        if (config.getName() == null || config.getName().isBlank()) {
            config.setName(environmentName);
        }
    }
}
