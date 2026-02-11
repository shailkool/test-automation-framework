package com.automation.core.diff;

/**
 * Types of differences
 */
public enum DiffType {
    ADDED,      // Row exists in right but not in left
    DELETED,    // Row exists in left but not in right
    MODIFIED,   // Row exists in both but has different values
    UNCHANGED   // Row exists in both with same values
}
