package com.automation.core.diff;

import lombok.extern.log4j.Log4j2;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * High-level, instance-based report generator for {@link DiffResult} objects.
 *
 * <p>Delegates HTML rendering to {@link DiffHtmlReportGenerator} while giving
 * tests a stable, title-aware entry point. Ensures parent directories are
 * created so callers can drop reports into arbitrary output paths.
 */
@Log4j2
public class DiffReportGenerator {

    private static final String DEFAULT_OLD_LABEL = "Expected";
    private static final String DEFAULT_NEW_LABEL = "Actual";

    public void saveReport(DiffResult result, String title, String outputPath) {
        saveReport(result, title, DEFAULT_OLD_LABEL, DEFAULT_NEW_LABEL, outputPath);
    }

    /**
     * Render {@code result} to HTML and write it to {@code outputPath} using
     * the supplied labels for the report's old/new file metadata strip.
     */
    public void saveReport(DiffResult result,
                           String title,
                           String oldLabel,
                           String newLabel,
                           String outputPath) {
        saveReport(result, title,
            DiffSideInfo.label(oldLabel),
            DiffSideInfo.label(newLabel),
            outputPath);
    }

    /**
     * Render {@code result} using rich per-side metadata and write the
     * resulting HTML to {@code outputPath}.
     */
    public void saveReport(DiffResult result,
                           String title,
                           DiffSideInfo oldSide,
                           DiffSideInfo newSide,
                           String outputPath) {
        if (result == null) {
            throw new IllegalArgumentException("DiffResult must not be null");
        }
        if (outputPath == null || outputPath.isBlank()) {
            throw new IllegalArgumentException("Output path must not be blank");
        }

        ensureParentDirectory(outputPath);

        String html = DiffHtmlReportGenerator.generateHtml(
            result.getDataDiff(),
            title,
            oldSide,
            newSide
        );

        try (FileWriter writer = new FileWriter(outputPath)) {
            writer.write(html);
            log.info("Diff report written to {}", outputPath);
        } catch (IOException e) {
            log.error("Failed to write diff report to {}", outputPath, e);
            throw new RuntimeException("Failed to generate diff report", e);
        }
    }

    public String generateHtml(DiffResult result, String title) {
        return generateHtml(result, title, DEFAULT_OLD_LABEL, DEFAULT_NEW_LABEL);
    }

    /**
     * Render {@code result} and return the HTML as a string without writing
     * to disk. Useful for embedding in email bodies or reporting dashboards.
     */
    public String generateHtml(DiffResult result,
                               String title,
                               String oldLabel,
                               String newLabel) {
        if (result == null) {
            throw new IllegalArgumentException("DiffResult must not be null");
        }
        return DiffHtmlReportGenerator.generateHtml(
            result.getDataDiff(),
            title,
            oldLabel,
            newLabel
        );
    }

    private void ensureParentDirectory(String outputPath) {
        File parent = new File(outputPath).getAbsoluteFile().getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            log.warn("Could not create parent directory for {}", outputPath);
        }
    }
}
