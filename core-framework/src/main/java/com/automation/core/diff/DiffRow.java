package com.automation.core.diff;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Represents a row difference
 */
@Getter
public class DiffRow {
    
    private final String key;
    private final Map<String, String> leftRow;
    private final Map<String, String> rightRow;
    private final DiffType diffType;
    
    @Setter
    private List<FieldDiff> fieldDiffs = new ArrayList<>();
    
    public DiffRow(String key, Map<String, String> leftRow, 
                   Map<String, String> rightRow, DiffType diffType) {
        this.key = key;
        this.leftRow = leftRow;
        this.rightRow = rightRow;
        this.diffType = diffType;
    }
    
    /**
     * Check if row has field-level differences
     */
    public boolean hasFieldDiffs() {
        return !fieldDiffs.isEmpty();
    }
    
    /**
     * Get number of field differences
     */
    public int getFieldDiffCount() {
        return fieldDiffs.size();
    }
}
