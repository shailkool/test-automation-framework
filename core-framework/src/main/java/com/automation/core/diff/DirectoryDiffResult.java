package com.automation.core.diff;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

/**
 * Aggregate outcome of a {@link DirectoryDiff} run: one {@link FileDiffResult}
 * per matched file pair (or unmatched orphan), plus aggregated counts that
 * drive the summary report.
 */
@Getter
public class DirectoryDiffResult {

    private final String oldDir;
    private final String newDir;
    private final String pattern;
    private final List<String> keyFields;
    private final List<FileDiffResult> fileResults;

    public DirectoryDiffResult(String oldDir, String newDir, String pattern,
                               List<String> keyFields, List<FileDiffResult> fileResults) {
        this.oldDir = oldDir;
        this.newDir = newDir;
        this.pattern = pattern;
        this.keyFields = keyFields == null ? List.of() : List.copyOf(keyFields);
        this.fileResults = fileResults == null ? List.of() : List.copyOf(fileResults);
    }

    public int getTotalFiles()              { return fileResults.size(); }
    public int getIdenticalFiles()          { return countByStatus(FileStatus.IDENTICAL); }
    public int getFilesWithDifferences()    { return countByStatus(FileStatus.DIFFERENT); }
    public int getOnlyOldFiles()            { return countByStatus(FileStatus.ONLY_OLD); }
    public int getOnlyNewFiles()            { return countByStatus(FileStatus.ONLY_NEW); }

    public long getTotalChangedRows() {
        return fileResults.stream().mapToLong(FileDiffResult::getChangedRows).sum();
    }
    public long getTotalAddedRows() {
        return fileResults.stream().mapToLong(FileDiffResult::getAddedRows).sum();
    }
    public long getTotalRemovedRows() {
        return fileResults.stream().mapToLong(FileDiffResult::getRemovedRows).sum();
    }
    public long getTotalUnchangedRows() {
        return fileResults.stream().mapToLong(FileDiffResult::getUnchangedRows).sum();
    }

    /**
     * Overall match percentage across every compared file pair, weighted by
     * the row count on each side. Uncompared files (only-old/only-new) count
     * as zero match. Returns 100.0 when nothing was compared.
     */
    public double getOverallMatchPercentage() {
        long unchanged = getTotalUnchangedRows();
        long denom = fileResults.stream()
            .mapToLong(f -> Math.max(f.getOldRowCount(), f.getNewRowCount()))
            .sum();
        if (denom == 0) return 100.0;
        return (unchanged * 100.0) / denom;
    }

    public boolean hasDifferences() {
        return getFilesWithDifferences() + getOnlyOldFiles() + getOnlyNewFiles() > 0;
    }

    public List<FileDiffResult> filesByStatus(FileStatus status) {
        List<FileDiffResult> out = new ArrayList<>();
        for (FileDiffResult f : fileResults) {
            if (f.getStatus() == status) out.add(f);
        }
        return out;
    }

    private int countByStatus(FileStatus status) {
        int n = 0;
        for (FileDiffResult f : fileResults) {
            if (f.getStatus() == status) n++;
        }
        return n;
    }
}
