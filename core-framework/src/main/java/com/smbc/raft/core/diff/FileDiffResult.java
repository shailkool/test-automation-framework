package com.smbc.raft.core.diff;

import lombok.Getter;

/**
 * Per-file outcome produced by {@link DirectoryDiff}: the file's status on both sides, the detailed
 * {@link DiffResult} (when both sides had a copy), and the filesystem path of the generated HTML
 * detail report (when one was rendered).
 */
@Getter
public class FileDiffResult {

  private final String fileName;
  private final FileStatus status;
  private final DiffResult diffResult;
  private final String oldPath;
  private final String newPath;
  private final String reportPath;
  private final long oldRowCount;
  private final long newRowCount;
  private final String errorMessage;

  private FileDiffResult(Builder b) {
    this.fileName = b.fileName;
    this.status = b.status;
    this.diffResult = b.diffResult;
    this.oldPath = b.oldPath;
    this.newPath = b.newPath;
    this.reportPath = b.reportPath;
    this.oldRowCount = b.oldRowCount;
    this.newRowCount = b.newRowCount;
    this.errorMessage = b.errorMessage;
  }

  public int getChangedRows() {
    return diffResult == null ? 0 : diffResult.getModifiedRows();
  }

  public int getAddedRows() {
    return diffResult == null ? 0 : diffResult.getAddedRows();
  }

  public int getRemovedRows() {
    return diffResult == null ? 0 : diffResult.getDeletedRows();
  }

  public int getUnchangedRows() {
    return diffResult == null ? 0 : diffResult.getMatchedRows();
  }

  public double getMatchPercentage() {
    if (diffResult != null) {
      return diffResult.getMatchPercentage();
    }
    return status == FileStatus.IDENTICAL ? 100.0 : 0.0;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private String fileName;
    private FileStatus status;
    private DiffResult diffResult;
    private String oldPath;
    private String newPath;
    private String reportPath;
    private long oldRowCount;
    private long newRowCount;
    private String errorMessage;

    public Builder fileName(String v) {
      this.fileName = v;
      return this;
    }

    public Builder status(FileStatus v) {
      this.status = v;
      return this;
    }

    public Builder diffResult(DiffResult v) {
      this.diffResult = v;
      return this;
    }

    public Builder oldPath(String v) {
      this.oldPath = v;
      return this;
    }

    public Builder newPath(String v) {
      this.newPath = v;
      return this;
    }

    public Builder reportPath(String v) {
      this.reportPath = v;
      return this;
    }

    public Builder oldRowCount(long v) {
      this.oldRowCount = v;
      return this;
    }

    public Builder newRowCount(long v) {
      this.newRowCount = v;
      return this;
    }

    public Builder errorMessage(String v) {
      this.errorMessage = v;
      return this;
    }

    public FileDiffResult build() {
      if (fileName == null) throw new IllegalStateException("fileName is required");
      if (status == null) throw new IllegalStateException("status is required");
      return new FileDiffResult(this);
    }
  }
}
