package com.smbc.raft.core.health;

import lombok.Builder;
import lombok.extern.log4j.Log4j2;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Checks a file-system path for presence, file format, freshness, and
 * minimum file count. Works with local paths, UNC paths, and mapped drives.
 *
 * <p>Usage:
 * <pre>
 *   HealthCheck check = FilePresenceCheck.builder()
 *       .name("tradeOutput")
 *       .path("C:/tmp/trade-output")
 *       .expectedFormats(Set.of("csv", "xlsx"))
 *       .maxAgeMinutes(60)
 *       .minFileCount(1)
 *       .build();
 *
 *   HealthCheckResult result = check.run();
 * </pre>
 */
@Log4j2
@Builder
public class FilePresenceCheck implements HealthCheck {

    private final String name;
    private final String path;

    /** File extensions to accept, without dots. Null or empty = accept all. */
    @Builder.Default
    private final Set<String> expectedFormats = Set.of();

    /**
     * Maximum age in minutes for files to be considered fresh.
     * 0 means no age check is performed.
     */
    @Builder.Default
    private final int maxAgeMinutes = 0;

    /**
     * Minimum number of matching files that must exist.
     * 0 means the check passes even if the folder is empty.
     */
    @Builder.Default
    private final int minFileCount = 0;

    @Override
    public HealthCheckResult run() {
        long start = System.currentTimeMillis();
        Path dir = Paths.get(path);

        if (!Files.exists(dir)) {
            return result(HealthCheckResult.Status.DOWN, "Path does not exist: " + path,
                System.currentTimeMillis() - start);
        }
        if (!Files.isDirectory(dir)) {
            return result(HealthCheckResult.Status.DOWN, "Path is not a directory: " + path,
                System.currentTimeMillis() - start);
        }

        return performScan(dir, start);
    }

    private HealthCheckResult performScan(Path dir, long start) {
        List<String> issues = new ArrayList<>();
        int total = 0;
        int matching = 0;
        int stale = 0;

        Instant cutoff = maxAgeMinutes > 0
            ? Instant.now().minus(maxAgeMinutes, ChronoUnit.MINUTES)
            : null;

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            for (Path file : stream) {
                if (!Files.isRegularFile(file)) {
                    continue;
                }
                total++;

                if (isMatchingFormat(file)) {
                    matching++;
                    if (isStale(file, cutoff)) {
                        stale++;
                        issues.add("Stale (>" + maxAgeMinutes + "min): " + file.getFileName());
                    }
                } else {
                    issues.add("Unexpected format: " + file.getFileName());
                }
            }
        } catch (IOException e) {
            return result(HealthCheckResult.Status.DOWN, "Cannot scan directory: " + e.getMessage(),
                System.currentTimeMillis() - start);
        }

        if (matching < minFileCount) {
            issues.add(String.format("Need at least %d matching file(s), found %d", minFileCount, matching));
        }

        long elapsed = System.currentTimeMillis() - start;
        String detail = String.format("%d total, %d matching, %d stale — path: %s",
            total, matching, stale, path);

        if (!issues.isEmpty()) {
            log.warn("FilePresenceCheck [{}] issues: {}", name, issues);
            return result(HealthCheckResult.Status.DEGRADED,
                detail + " | issues: " + String.join("; ", issues), elapsed);
        }

        log.debug("FilePresenceCheck [{}]: {}", name, detail);
        return result(HealthCheckResult.Status.UP, detail, elapsed);
    }

    private boolean isMatchingFormat(Path file) {
        String fileName = file.getFileName().toString().toLowerCase();
        return expectedFormats == null || expectedFormats.isEmpty()
            || expectedFormats.stream().anyMatch(fmt -> fileName.endsWith("." + fmt.toLowerCase()));
    }

    private boolean isStale(Path file, Instant cutoff) {
        if (cutoff == null) {
            return false;
        }
        try {
            BasicFileAttributes attrs = Files.readAttributes(file, BasicFileAttributes.class);
            return attrs.lastModifiedTime().toInstant().isBefore(cutoff);
        } catch (IOException e) {
            log.warn("Cannot read attributes for {}: {}", file, e.getMessage());
            return false;
        }
    }

    private HealthCheckResult result(HealthCheckResult.Status status,
                                     String detail, long ms) {
        return HealthCheckResult.builder()
            .name(name).category("FileSystem")
            .status(status).detail(detail)
            .durationMillis(ms).checkedAt(Instant.now()).build();
    }
}
