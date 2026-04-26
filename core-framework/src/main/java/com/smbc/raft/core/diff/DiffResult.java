package com.smbc.raft.core.diff;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;

/**
 * Result of a data comparison operation, produced by {@link DataDiff#compare}.
 *
 * <p>Exposes aggregate counts and helper accessors over the underlying {@link DataDiff} so callers
 * can assert on match percentages, record-level modifications, and generate reports without
 * depending on internal classes.
 */
@Getter
public class DiffResult {

  private final DataDiff dataDiff;

  public DiffResult(DataDiff dataDiff) {
    this.dataDiff = dataDiff;
  }

  public boolean isIdentical() {
    return !dataDiff.isHasDifferences();
  }

  public int getMatchedRows() {
    return dataDiff.getUnchangedRows().size();
  }

  public int getAddedRows() {
    return dataDiff.getAddedRows().size();
  }

  public int getDeletedRows() {
    return dataDiff.getDeletedRows().size();
  }

  public int getModifiedRows() {
    return dataDiff.getModifiedRows().size();
  }

  public int getTotalDifferences() {
    return dataDiff.getTotalDifferences();
  }

  public List<RowDifference> getModifiedRecords() {
    List<RowDifference> records = new ArrayList<>();
    for (DiffRow row : dataDiff.getModifiedRows()) {
      records.add(new RowDifference(row));
    }
    return records;
  }

  public List<RowDifference> getAddedRecords() {
    List<RowDifference> records = new ArrayList<>();
    for (DiffRow row : dataDiff.getAddedRows()) {
      records.add(new RowDifference(row));
    }
    return records;
  }

  public List<RowDifference> getDeletedRecords() {
    List<RowDifference> records = new ArrayList<>();
    for (DiffRow row : dataDiff.getDeletedRows()) {
      records.add(new RowDifference(row));
    }
    return records;
  }

  /**
   * Percentage of left-side rows that matched a right-side row without modification. Uses the
   * larger of left/right as denominator to avoid inflating the score when one side is shorter.
   */
  public double getMatchPercentage() {
    DiffSummary summary = dataDiff.getSummary();
    int denominator = Math.max(summary.getLeftRowCount(), summary.getRightRowCount());
    if (denominator == 0) {
      return 100.0;
    }
    return (summary.getUnchangedCount() * 100.0) / denominator;
  }

  public String getSummaryString() {
    DiffSummary s = dataDiff.getSummary();
    return String.format(
        "Matched: %d, Added: %d, Deleted: %d, Modified: %d, Match %%: %.2f",
        s.getUnchangedCount(),
        s.getAddedCount(),
        s.getDeletedCount(),
        s.getModifiedCount(),
        getMatchPercentage());
  }

  /**
   * Lightweight view over a modified {@link DiffRow} that surfaces field-level changes to test
   * code.
   */
  @Getter
  public static class RowDifference {

    private final DiffRow diffRow;

    public RowDifference(DiffRow diffRow) {
      this.diffRow = diffRow;
    }

    public String getKey() {
      return diffRow.getKey();
    }

    public DiffType getDiffType() {
      return diffRow.getDiffType();
    }

    public List<FieldDiff> getFieldDifferences() {
      return diffRow.getFieldDiffs();
    }
  }
}
