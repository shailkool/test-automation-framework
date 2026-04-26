package com.smbc.raft.core.diff;

import lombok.Getter;

/** Represents a field-level difference */
@Getter
public class FieldDiff {

  private final String fieldName;
  private final String leftValue;
  private final String rightValue;

  public FieldDiff(String fieldName, String leftValue, String rightValue) {
    this.fieldName = fieldName;
    this.leftValue = leftValue;
    this.rightValue = rightValue;
  }

  @Override
  public String toString() {
    return String.format("%s: '%s' -> '%s'", fieldName, leftValue, rightValue);
  }
}
