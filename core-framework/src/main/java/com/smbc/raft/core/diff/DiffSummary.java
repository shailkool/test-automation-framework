package com.smbc.raft.core.diff;

import lombok.Getter;

/** Summary statistics for a data diff */
@Getter
public class DiffSummary {

  private final int leftRowCount;
  private final int rightRowCount;
  private final int addedCount;
  private final int deletedCount;
  private final int modifiedCount;
  private final int unchangedCount;

  public DiffSummary(
      int leftRowCount,
      int rightRowCount,
      int addedCount,
      int deletedCount,
      int modifiedCount,
      int unchangedCount) {
    this.leftRowCount = leftRowCount;
    this.rightRowCount = rightRowCount;
    this.addedCount = addedCount;
    this.deletedCount = deletedCount;
    this.modifiedCount = modifiedCount;
    this.unchangedCount = unchangedCount;
  }

  /** Get total number of differences */
  public int getTotalDifferences() {
    return addedCount + deletedCount + modifiedCount;
  }

  /** Check if there are any differences */
  public boolean hasDifferences() {
    return getTotalDifferences() > 0;
  }

  /** Get percentage of unchanged rows */
  public double getUnchangedPercentage() {
    int totalRows = Math.max(leftRowCount, rightRowCount);
    if (totalRows == 0) {
      return 100.0;
    }
    return (unchangedCount * 100.0) / totalRows;
  }

  public String toBusinessString() {
    return String.format(
        "Matched: %d, Added: %d, Deleted: %d, Modified: %d (%.2f%% Similarity)",
        unchangedCount, addedCount, deletedCount, modifiedCount, getUnchangedPercentage());
  }
}
