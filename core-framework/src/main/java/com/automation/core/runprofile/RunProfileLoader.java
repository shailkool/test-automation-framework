package com.automation.core.runprofile;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.log4j.Log4j2;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Locates and deserializes a {@link RunProfile} JSON document.
 *
 * <p>Resolution order:
 * <ol>
 *   <li>Classpath resource {@code <baseDir>/<profile>.json}
 *       (default baseDir is {@code profiles})</li>
 *   <li>Filesystem file {@code <baseDir>/<profile>.json}</li>
 * </ol>
 */
@Log4j2
public class RunProfileLoader {

    public static final String DEFAULT_BASE_DIR = "profiles";

    private final ObjectMapper mapper;

    public RunProfileLoader() {
        this.mapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public RunProfile load(String profileName) {
        return load(profileName, DEFAULT_BASE_DIR);
    }

    public RunProfile load(String profileName, String baseDir) {
        if (profileName == null || profileName.isBlank()) {
            throw new IllegalArgumentException("profileName must be provided");
        }

        String resource = baseDir + "/" + profileName + ".json";

        InputStream classpathStream = Thread.currentThread()
            .getContextClassLoader()
            .getResourceAsStream(resource);
        if (classpathStream != null) {
            try (InputStream in = classpathStream) {
                RunProfile profile = mapper.readValue(in, RunProfile.class);
                applyDefaults(profile, profileName);
                log.info("Loaded run profile '{}' from classpath resource '{}'",
                    profileName, resource);
                return profile;
            } catch (IOException e) {
                throw new IllegalStateException(
                    "Failed to parse run profile from classpath '" + resource + "'", e);
            }
        }

        Path fsPath = Paths.get(resource);
        if (Files.exists(fsPath)) {
            try {
                RunProfile profile = mapper.readValue(fsPath.toFile(), RunProfile.class);
                applyDefaults(profile, profileName);
                log.info("Loaded run profile '{}' from filesystem '{}'",
                    profileName, fsPath.toAbsolutePath());
                return profile;
            } catch (IOException e) {
                throw new IllegalStateException(
                    "Failed to parse run profile from file '" + fsPath.toAbsolutePath() + "'", e);
            }
        }

        throw new IllegalStateException(String.format(
            "Run profile '%s.json' not found on classpath ('%s') or filesystem ('%s'). "
                + "Create the file under src/test/resources/%s/.",
            profileName, resource, fsPath.toAbsolutePath(), baseDir));
    }

    private void applyDefaults(RunProfile profile, String profileName) {
        if (profile.getName() == null || profile.getName().isBlank()) {
            profile.setName(profileName);
        }
    }
}
