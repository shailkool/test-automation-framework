package com.smbc.raft.core.diff;

import lombok.Getter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Metadata about one side (old or new) of a diff, surfaced in the report's
 * metadata strip.
 *
 * <p>When only {@link #getLabel()} is populated the report renders a minimal
 * single-line description (used by callers such as Cucumber step defs where
 * there is no real file on disk). When the builder is populated with a
 * {@code path} via {@link #forFile(Path, long)} the generator shows the full
 * path on its own line together with creation time, byte size, and row count.
 */
@Getter
public final class DiffSideInfo {

    private final String label;
    private final String path;
    private final Long sizeBytes;
    private final String createdAt;
    private final Long rowCount;

    private DiffSideInfo(Builder b) {
        this.label = b.label;
        this.path = b.path;
        this.sizeBytes = b.sizeBytes;
        this.createdAt = b.createdAt;
        this.rowCount = b.rowCount;
    }

    public boolean hasPath()      { return path != null && !path.isBlank(); }
    public boolean hasSize()      { return sizeBytes != null; }
    public boolean hasCreated()   { return createdAt != null && !createdAt.isBlank(); }
    public boolean hasRowCount()  { return rowCount != null; }
    public boolean hasDetails()   { return hasPath() || hasSize() || hasCreated() || hasRowCount(); }

    /** Short, label-only side (no filesystem metadata). */
    public static DiffSideInfo label(String label) {
        return builder().label(label).build();
    }

    /**
     * Build a {@link DiffSideInfo} from a real CSV file on disk and a row
     * count that the caller has already determined. Creation time and byte
     * size are read from the filesystem; if the attributes can't be read,
     * those fields are simply omitted.
     */
    public static DiffSideInfo forFile(Path file, long rowCount) {
        Builder b = builder()
            .label(file.getFileName().toString())
            .path(file.toAbsolutePath().toString())
            .rowCount(rowCount);
        try {
            BasicFileAttributes attrs = Files.readAttributes(file, BasicFileAttributes.class);
            b.sizeBytes(attrs.size());
            b.createdAt(LocalDateTime.ofInstant(
                attrs.creationTime().toInstant(), ZoneId.systemDefault()
            ).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        } catch (IOException ignored) {
            // leave size/createdAt blank
        }
        return b.build();
    }

    public static Builder builder() { return new Builder(); }

    /** Human-friendly byte size, e.g. {@code "12.3 KB"}. */
    public String getHumanSize() {
        if (sizeBytes == null) return "";
        long b = sizeBytes;
        if (b < 1024) return b + " B";
        if (b < 1024L * 1024L) return String.format("%.1f KB", b / 1024.0);
        if (b < 1024L * 1024L * 1024L) return String.format("%.1f MB", b / (1024.0 * 1024.0));
        return String.format("%.1f GB", b / (1024.0 * 1024.0 * 1024.0));
    }

    public static class Builder {
        private String label;
        private String path;
        private Long sizeBytes;
        private String createdAt;
        private Long rowCount;

        public Builder label(String v)     { this.label = v; return this; }
        public Builder path(String v)      { this.path = v; return this; }
        public Builder sizeBytes(Long v)   { this.sizeBytes = v; return this; }
        public Builder createdAt(String v) { this.createdAt = v; return this; }
        public Builder rowCount(Long v)    { this.rowCount = v; return this; }

        public DiffSideInfo build() {
            if (label == null || label.isBlank()) label = "";
            return new DiffSideInfo(this);
        }
    }
}
