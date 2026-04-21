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

    /**
     * Render {@code result} to HTML and write it to {@code outputPath}.
     *
     * @param result the comparison result to render
     * @param title  human-readable report title used for headings
     * @param outputPath filesystem path to write the HTML report to
     */
    public void saveReport(DiffResult result, String title, String outputPath) {
        if (result == null) {
            throw new IllegalArgumentException("DiffResult must not be null");
        }
        if (outputPath == null || outputPath.isBlank()) {
            throw new IllegalArgumentException("Output path must not be blank");
        }

        ensureParentDirectory(outputPath);

        String html = DiffHtmlReportGenerator.generateHtml(
            result.getDataDiff(),
            title + " - Expected",
            title + " - Actual"
        );

        try (FileWriter writer = new FileWriter(outputPath)) {
            writer.write(html);
            log.info("Diff report written to {}", outputPath);
        } catch (IOException e) {
            log.error("Failed to write diff report to {}", outputPath, e);
            throw new RuntimeException("Failed to generate diff report", e);
        }
    }

    /**
     * Render {@code result} and return the HTML as a string without writing
     * to disk. Useful for embedding in email bodies or reporting dashboards.
     */
    public String generateHtml(DiffResult result, String title) {
        if (result == null) {
            throw new IllegalArgumentException("DiffResult must not be null");
        }
        return DiffHtmlReportGenerator.generateHtml(
            result.getDataDiff(),
            title + " - Expected",
            title + " - Actual"
        );
    }

    private void ensureParentDirectory(String outputPath) {
        File parent = new File(outputPath).getAbsoluteFile().getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            log.warn("Could not create parent directory for {}", outputPath);
        }
    }
}
