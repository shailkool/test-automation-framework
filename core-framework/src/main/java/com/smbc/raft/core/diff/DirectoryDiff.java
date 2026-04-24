package com.smbc.raft.core.diff;

import lombok.extern.log4j.Log4j2;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

/**
 * Compares two directories of CSV files and produces a {@link DirectoryDiffResult}.
 *
 * <p>Files are paired by their filename (case-sensitive) after an optional
 * glob filter is applied. When both sides have the same filename the contents
 * are diffed with the configured key fields; when a file exists on only one
 * side it is reported as {@link FileStatus#ONLY_OLD} or
 * {@link FileStatus#ONLY_NEW}.
 *
 * <p>Optionally, per-file HTML detail reports can be rendered as a side
 * effect via {@link Builder#renderPerFileReports(Path)}; the resulting HTML
 * path is captured on each {@link FileDiffResult#getReportPath()} so the
 * summary report can link to it.
 */
@Log4j2
public final class DirectoryDiff {

    private final List<String> keyFields;
    private final boolean ignoreCase;
    private final List<String> ignoredFields;
    private final String glob;
    private final boolean recursive;
    private final Path perFileReportDir;
    private final String reportTitle;

    private DirectoryDiff(Builder b) {
        this.keyFields = List.copyOf(b.keyFields);
        this.ignoreCase = b.ignoreCase;
        this.ignoredFields = List.copyOf(b.ignoredFields);
        this.glob = b.glob;
        this.recursive = b.recursive;
        this.perFileReportDir = b.perFileReportDir;
        this.reportTitle = b.reportTitle;
    }

    public static Builder builder() { return new Builder(); }

    public DirectoryDiffResult compare(Path oldDir, Path newDir) {
        if (oldDir == null || newDir == null) {
            throw new IllegalArgumentException("oldDir and newDir must not be null");
        }
        if (keyFields.isEmpty()) {
            throw new IllegalStateException("At least one keyField must be configured");
        }

        Set<String> oldNames = listFiles(oldDir);
        Set<String> newNames = listFiles(newDir);

        Set<String> allNames = new TreeSet<>();
        allNames.addAll(oldNames);
        allNames.addAll(newNames);

        if (perFileReportDir != null) {
            try {
                Files.createDirectories(perFileReportDir);
            } catch (IOException e) {
                throw new RuntimeException("Cannot create report dir " + perFileReportDir, e);
            }
        }

        List<FileDiffResult> results = new ArrayList<>(allNames.size());
        for (String name : allNames) {
            boolean inOld = oldNames.contains(name);
            boolean inNew = newNames.contains(name);
            Path oldFile = inOld ? oldDir.resolve(name) : null;
            Path newFile = inNew ? newDir.resolve(name) : null;

            if (inOld && !inNew) {
                results.add(onlyOld(name, oldFile));
            } else if (!inOld) {
                results.add(onlyNew(name, newFile));
            } else {
                results.add(comparePair(name, oldFile, newFile));
            }
        }

        return new DirectoryDiffResult(
            oldDir.toString(),
            newDir.toString(),
            glob == null ? "*" : glob,
            keyFields,
            results
        );
    }

    private FileDiffResult comparePair(String name, Path oldFile, Path newFile) {
        try {
            List<Map<String, String>> oldData = CsvLoader.load(oldFile);
            List<Map<String, String>> newData = CsvLoader.load(newFile);
            CsvLoader.ensureKeyColumnsPresent(oldFile, keyFields, oldData);
            CsvLoader.ensureKeyColumnsPresent(newFile, keyFields, newData);

            DataDiff.Builder db = DataDiff.builder()
                .keyFields(keyFields)
                .ignoreCase(ignoreCase);
            for (String ignored : ignoredFields) {
                db.ignoreField(ignored);
            }
            DiffResult result = db.build().compare(oldData, newData);

            FileStatus status = result.isIdentical() ? FileStatus.IDENTICAL : FileStatus.DIFFERENT;

            String reportPath = null;
            if (perFileReportDir != null && status == FileStatus.DIFFERENT) {
                DiffSideInfo oldSide = DiffSideInfo.forFile(oldFile, oldData.size());
                DiffSideInfo newSide = DiffSideInfo.forFile(newFile, newData.size());
                reportPath = renderDetail(name, oldSide, newSide, result);
            }

            return FileDiffResult.builder()
                .fileName(name)
                .status(status)
                .diffResult(result)
                .oldPath(oldFile.toString())
                .newPath(newFile.toString())
                .oldRowCount(oldData.size())
                .newRowCount(newData.size())
                .reportPath(reportPath)
                .build();
        } catch (RuntimeException e) {
            log.error("Failed to diff '{}': {}", name, e.getMessage());
            return FileDiffResult.builder()
                .fileName(name)
                .status(FileStatus.DIFFERENT)
                .oldPath(oldFile == null ? null : oldFile.toString())
                .newPath(newFile == null ? null : newFile.toString())
                .errorMessage(e.getMessage())
                .build();
        }
    }

    private String renderDetail(String name, DiffSideInfo oldSide, DiffSideInfo newSide, DiffResult result) {
        String title = (reportTitle == null || reportTitle.isBlank()) ? name : reportTitle + " / " + name;
        String fileName = sanitize(name) + ".html";
        Path dest = perFileReportDir.resolve(fileName);
        new DiffReportGenerator().saveReport(result, title, oldSide, newSide, dest.toString());
        return dest.toString();
    }

    private FileDiffResult onlyOld(String name, Path oldFile) {
        long rows = safeRowCount(oldFile);
        return FileDiffResult.builder()
            .fileName(name)
            .status(FileStatus.ONLY_OLD)
            .oldPath(oldFile.toString())
            .oldRowCount(rows)
            .build();
    }

    private FileDiffResult onlyNew(String name, Path newFile) {
        long rows = safeRowCount(newFile);
        return FileDiffResult.builder()
            .fileName(name)
            .status(FileStatus.ONLY_NEW)
            .newPath(newFile.toString())
            .newRowCount(rows)
            .build();
    }

    private long safeRowCount(Path file) {
        try {
            return CsvLoader.load(file).size();
        } catch (RuntimeException e) {
            return 0;
        }
    }

    private Set<String> listFiles(Path dir) {
        if (dir == null || !Files.isDirectory(dir)) {
            return Collections.emptySet();
        }
        PathMatcher matcher = (glob == null || glob.isBlank())
            ? null
            : FileSystems.getDefault().getPathMatcher("glob:" + glob);

        Set<String> names = new TreeSet<>();
        try (Stream<Path> stream = recursive ? Files.walk(dir) : Files.list(dir)) {
            stream
                .filter(Files::isRegularFile)
                .forEach(p -> {
                    Path rel = dir.relativize(p);
                    if (matcher == null || matcher.matches(rel.getFileName())) {
                        names.add(rel.toString().replace('\\', '/'));
                    }
                });
        } catch (IOException e) {
            throw new RuntimeException("Failed to list " + dir, e);
        }
        return names;
    }

    static String sanitize(String value) {
        return value.replaceAll("[^A-Za-z0-9._-]+", "_");
    }

    public static class Builder {
        private final List<String> keyFields = new ArrayList<>();
        private final List<String> ignoredFields = new ArrayList<>();
        private boolean ignoreCase = false;
        private String glob = "*.csv";
        private boolean recursive = false;
        private Path perFileReportDir;
        private String reportTitle;

        public Builder keyField(String v) {
            if (v != null && !v.isBlank()) keyFields.add(v);
            return this;
        }
        public Builder keyFields(List<String> vs) {
            if (vs != null) vs.forEach(this::keyField);
            return this;
        }
        public Builder ignoreCase(boolean v) { this.ignoreCase = v; return this; }
        public Builder ignoreField(String v) {
            if (v != null && !v.isBlank()) ignoredFields.add(v);
            return this;
        }
        public Builder glob(String v)        { this.glob = v; return this; }
        public Builder recursive(boolean v)  { this.recursive = v; return this; }
        public Builder reportTitle(String v) { this.reportTitle = v; return this; }

        /**
         * Enable per-file detail report generation. Individual {@code .html}
         * files are written into {@code dir} (created if necessary) and each
         * {@link FileDiffResult#getReportPath()} will point at its detail
         * file.
         */
        public Builder renderPerFileReports(Path dir) {
            this.perFileReportDir = dir;
            return this;
        }

        public DirectoryDiff build() {
            if (keyFields.isEmpty()) {
                throw new IllegalStateException("At least one keyField is required");
            }
            return new DirectoryDiff(this);
        }
    }
}
