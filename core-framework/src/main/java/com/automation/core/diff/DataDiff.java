package com.automation.core.diff;

import lombok.Getter;
import java.util.*;

/**
 * Compares two data tables and identifies differences
 * Similar to csv-diff functionality
 */
@Getter
public class DataDiff {
    
    private final List<Map<String, String>> leftData;
    private final List<Map<String, String>> rightData;
    private final List<String> keyFields;
    
    private List<DiffRow> addedRows = new ArrayList<>();
    private List<DiffRow> deletedRows = new ArrayList<>();
    private List<DiffRow> modifiedRows = new ArrayList<>();
    private List<DiffRow> unchangedRows = new ArrayList<>();
    
    private boolean hasDifferences = false;
    
    /**
     * Create diff with single key field
     */
    public DataDiff(List<Map<String, String>> leftData, 
                    List<Map<String, String>> rightData,
                    String keyField) {
        this(leftData, rightData, Arrays.asList(keyField));
    }
    
    /**
     * Create diff with multiple key fields
     */
    public DataDiff(List<Map<String, String>> leftData,
                    List<Map<String, String>> rightData,
                    List<String> keyFields) {
        this.leftData = leftData;
        this.rightData = rightData;
        this.keyFields = keyFields;
        performDiff();
    }
    
    /**
     * Perform the diff comparison
     */
    private void performDiff() {
        // Create maps keyed by composite key
        Map<String, Map<String, String>> leftMap = createKeyMap(leftData);
        Map<String, Map<String, String>> rightMap = createKeyMap(rightData);
        
        Set<String> allKeys = new HashSet<>();
        allKeys.addAll(leftMap.keySet());
        allKeys.addAll(rightMap.keySet());
        
        for (String key : allKeys) {
            Map<String, String> leftRow = leftMap.get(key);
            Map<String, String> rightRow = rightMap.get(key);
            
            if (leftRow == null) {
                // Row added in right
                addedRows.add(new DiffRow(key, null, rightRow, DiffType.ADDED));
                hasDifferences = true;
            } else if (rightRow == null) {
                // Row deleted from left
                deletedRows.add(new DiffRow(key, leftRow, null, DiffType.DELETED));
                hasDifferences = true;
            } else {
                // Row exists in both, check for modifications
                List<FieldDiff> fieldDiffs = compareRows(leftRow, rightRow);
                if (fieldDiffs.isEmpty()) {
                    unchangedRows.add(new DiffRow(key, leftRow, rightRow, DiffType.UNCHANGED));
                } else {
                    DiffRow diffRow = new DiffRow(key, leftRow, rightRow, DiffType.MODIFIED);
                    diffRow.setFieldDiffs(fieldDiffs);
                    modifiedRows.add(diffRow);
                    hasDifferences = true;
                }
            }
        }
    }
    
    /**
     * Create map with composite key
     */
    private Map<String, Map<String, String>> createKeyMap(List<Map<String, String>> data) {
        Map<String, Map<String, String>> map = new LinkedHashMap<>();
        
        for (Map<String, String> row : data) {
            String key = createCompositeKey(row);
            map.put(key, row);
        }
        
        return map;
    }
    
    /**
     * Create composite key from key fields
     */
    private String createCompositeKey(Map<String, String> row) {
        StringBuilder key = new StringBuilder();
        for (String keyField : keyFields) {
            if (key.length() > 0) {
                key.append("||");
            }
            String value = row.getOrDefault(keyField, "");
            key.append(value);
        }
        return key.toString();
    }
    
    /**
     * Compare two rows and identify field differences
     */
    private List<FieldDiff> compareRows(Map<String, String> leftRow, Map<String, String> rightRow) {
        List<FieldDiff> diffs = new ArrayList<>();
        
        Set<String> allFields = new HashSet<>();
        allFields.addAll(leftRow.keySet());
        allFields.addAll(rightRow.keySet());
        
        for (String field : allFields) {
            // Skip key fields in comparison
            if (keyFields.contains(field)) {
                continue;
            }
            
            String leftValue = leftRow.getOrDefault(field, "");
            String rightValue = rightRow.getOrDefault(field, "");
            
            if (!leftValue.equals(rightValue)) {
                diffs.add(new FieldDiff(field, leftValue, rightValue));
            }
        }
        
        return diffs;
    }
    
    /**
     * Get total number of differences
     */
    public int getTotalDifferences() {
        return addedRows.size() + deletedRows.size() + modifiedRows.size();
    }
    
    /**
     * Get summary statistics
     */
    public DiffSummary getSummary() {
        return new DiffSummary(
            leftData.size(),
            rightData.size(),
            addedRows.size(),
            deletedRows.size(),
            modifiedRows.size(),
            unchangedRows.size()
        );
    }
    
    /**
     * Get all differences (added, deleted, modified)
     */
    public List<DiffRow> getAllDifferences() {
        List<DiffRow> allDiffs = new ArrayList<>();
        allDiffs.addAll(addedRows);
        allDiffs.addAll(deletedRows);
        allDiffs.addAll(modifiedRows);
        return allDiffs;
    }
    
    /**
     * Print summary to console
     */
    public void printSummary() {
        DiffSummary summary = getSummary();
        System.out.println("=== Data Diff Summary ===");
        System.out.println("Left rows:      " + summary.getLeftRowCount());
        System.out.println("Right rows:     " + summary.getRightRowCount());
        System.out.println("Added:          " + summary.getAddedCount());
        System.out.println("Deleted:        " + summary.getDeletedCount());
        System.out.println("Modified:       " + summary.getModifiedCount());
        System.out.println("Unchanged:      " + summary.getUnchangedCount());
        System.out.println("Has differences: " + hasDifferences);
    }
}
