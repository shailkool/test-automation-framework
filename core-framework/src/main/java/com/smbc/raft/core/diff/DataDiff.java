package com.smbc.raft.core.diff;

import lombok.Getter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Compares two data tables and identifies added, deleted, modified, and
 * unchanged rows.
 *
 * <p>Supports two styles of usage:
 * <ul>
 *   <li><b>Immediate</b> &mdash; pass both datasets to the constructor and
 *       read the result lists directly (e.g. {@link #getAddedRows()}).</li>
 *   <li><b>Builder</b> &mdash; configure key fields, case sensitivity, and
 *       ignored fields via {@link #builder()}, then call
 *       {@link #compare(List, List)} with the datasets to obtain a
 *       {@link DiffResult}.</li>
 * </ul>
 */
@Getter
@lombok.extern.log4j.Log4j2
public class DataDiff {

    private final List<Map<String, String>> leftData;
    private final List<Map<String, String>> rightData;
    private final List<String> keyFields;
    private final boolean ignoreCase;
    private final Set<String> ignoredFields;
    private final boolean useLeftSchema;

    private final List<DiffRow> addedRows = new ArrayList<>();
    private final List<DiffRow> deletedRows = new ArrayList<>();
    private final List<DiffRow> modifiedRows = new ArrayList<>();
    private final List<DiffRow> unchangedRows = new ArrayList<>();

    private boolean hasDifferences = false;

    public DataDiff(List<Map<String, String>> leftData,
                    List<Map<String, String>> rightData,
                    String keyField) {
        this(leftData, rightData, Collections.singletonList(keyField), false, Collections.emptySet(), false);
    }

    public DataDiff(List<Map<String, String>> leftData,
                    List<Map<String, String>> rightData,
                    List<String> keyFields) {
        this(leftData, rightData, keyFields, false, Collections.emptySet(), false);
    }

    private DataDiff(List<Map<String, String>> leftData,
                     List<Map<String, String>> rightData,
                     List<String> keyFields,
                     boolean ignoreCase,
                     Set<String> ignoredFields,
                     boolean useLeftSchema) {
        this.leftData = leftData;
        this.rightData = rightData;
        this.keyFields = keyFields == null ? Collections.emptyList() : new ArrayList<>(keyFields);
        this.ignoreCase = ignoreCase;
        this.ignoredFields = ignoredFields == null ? Collections.emptySet() : new LinkedHashSet<>(ignoredFields);
        this.useLeftSchema = useLeftSchema;
        if (leftData != null && rightData != null) {
            performDiff();
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Run a fresh comparison against the supplied datasets using this
     * instance's configuration and return a {@link DiffResult} wrapping the
     * outcome.
     */
    public DiffResult compare(List<Map<String, String>> expected,
                              List<Map<String, String>> actual) {
        if (keyFields.isEmpty()) {
            throw new IllegalStateException(
                "At least one key field must be configured before calling compare()"
            );
        }
        DataDiff diff = new DataDiff(
            expected,
            actual,
            this.keyFields,
            this.ignoreCase,
            this.ignoredFields,
            this.useLeftSchema
        );
        return new DiffResult(diff);
    }

    private void performDiff() {
        Map<String, Map<String, String>> leftMap = createKeyMap(leftData);
        Map<String, Map<String, String>> rightMap = createKeyMap(rightData);

        Set<String> allKeys = new LinkedHashSet<>();
        allKeys.addAll(leftMap.keySet());
        allKeys.addAll(rightMap.keySet());

        for (String key : allKeys) {
            Map<String, String> leftRow = leftMap.get(key);
            Map<String, String> rightRow = rightMap.get(key);

            if (leftRow == null) {
                addedRows.add(new DiffRow(key, null, rightRow, DiffType.ADDED));
                hasDifferences = true;
            } else if (rightRow == null) {
                deletedRows.add(new DiffRow(key, leftRow, null, DiffType.DELETED));
                hasDifferences = true;
            } else {
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

    private Map<String, Map<String, String>> createKeyMap(List<Map<String, String>> data) {
        Map<String, Map<String, String>> map = new LinkedHashMap<>();
        for (Map<String, String> row : data) {
            map.put(createCompositeKey(row), row);
        }
        return map;
    }

    private String createCompositeKey(Map<String, String> row) {
        StringBuilder key = new StringBuilder();
        for (String keyField : keyFields) {
            if (key.length() > 0) {
                key.append("||");
            }
            String value = row.getOrDefault(keyField, "");
            key.append(ignoreCase ? value.toLowerCase(java.util.Locale.ROOT) : value);
        }
        return key.toString();
    }

    private List<FieldDiff> compareRows(Map<String, String> leftRow, Map<String, String> rightRow) {
        List<FieldDiff> diffs = new ArrayList<>();

        Set<String> fieldSelection;
        if (useLeftSchema) {
            // Match only columns present in the expected output (leftRow)
            // and in the order they appear there.
            fieldSelection = leftRow.keySet();
        } else {
            fieldSelection = new LinkedHashSet<>();
            fieldSelection.addAll(leftRow.keySet());
            fieldSelection.addAll(rightRow.keySet());
        }

        for (String field : fieldSelection) {
            if (keyFields.contains(field) || ignoredFields.contains(field)) {
                continue;
            }

            String leftValue = leftRow.getOrDefault(field, "");
            String rightValue = rightRow.getOrDefault(field, "");

            if (!valuesEqual(leftValue, rightValue)) {
                diffs.add(new FieldDiff(field, leftValue, rightValue));
            }
        }

        return diffs;
    }

    private boolean valuesEqual(String left, String right) {
        if (left == null) {
            return right == null;
        }
        return ignoreCase ? left.equalsIgnoreCase(right) : left.equals(right);
    }

    public int getTotalDifferences() {
        return addedRows.size() + deletedRows.size() + modifiedRows.size();
    }

    public DiffSummary getSummary() {
        return new DiffSummary(
            leftData == null ? 0 : leftData.size(),
            rightData == null ? 0 : rightData.size(),
            addedRows.size(),
            deletedRows.size(),
            modifiedRows.size(),
            unchangedRows.size()
        );
    }

    public List<DiffRow> getAllDifferences() {
        List<DiffRow> allDiffs = new ArrayList<>();
        allDiffs.addAll(addedRows);
        allDiffs.addAll(deletedRows);
        allDiffs.addAll(modifiedRows);
        return allDiffs;
    }

    /**
     * Returns every {@link DiffRow} (added, deleted, modified, and unchanged)
     * in a stable display order: left-side rows in their original input order
     * first, followed by right-only rows in right input order. Used by the
     * HTML report to render a single unified table.
     */
    public List<DiffRow> getAllRowsInOrder() {
        java.util.Map<String, DiffRow> byKey = new java.util.HashMap<>();
        for (DiffRow r : unchangedRows) byKey.put(r.getKey(), r);
        for (DiffRow r : modifiedRows)  byKey.put(r.getKey(), r);
        for (DiffRow r : deletedRows)   byKey.put(r.getKey(), r);
        for (DiffRow r : addedRows)     byKey.put(r.getKey(), r);

        List<DiffRow> ordered = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        if (leftData != null) {
            for (Map<String, String> row : leftData) {
                String key = createCompositeKey(row);
                if (seen.add(key)) {
                    DiffRow dr = byKey.get(key);
                    if (dr != null) ordered.add(dr);
                }
            }
        }
        if (rightData != null) {
            for (Map<String, String> row : rightData) {
                String key = createCompositeKey(row);
                if (seen.add(key)) {
                    DiffRow dr = byKey.get(key);
                    if (dr != null) ordered.add(dr);
                }
            }
        }
        return ordered;
    }

    public void printSummary() {
        DiffSummary summary = getSummary();
        log.info("=== Data Diff Summary ===");
        log.info("Left rows:       {}", summary.getLeftRowCount());
        log.info("Right rows:      {}", summary.getRightRowCount());
        log.info("Added:           {}", summary.getAddedCount());
        log.info("Deleted:         {}", summary.getDeletedCount());
        log.info("Modified:        {}", summary.getModifiedCount());
        log.info("Unchanged:       {}", summary.getUnchangedCount());
        log.info("Has differences: {}", hasDifferences);
    }

    /**
     * Fluent builder for configuring comparison options prior to calling
     * {@link DataDiff#compare(List, List)}.
     */
    public static class Builder {

        private final List<String> keyFields = new ArrayList<>();
        private boolean ignoreCase = false;
        private boolean useLeftSchema = false;
        private final Set<String> ignoredFields = new LinkedHashSet<>();

        public Builder keyField(String keyField) {
            if (keyField != null && !keyField.isEmpty()) {
                this.keyFields.add(keyField);
            }
            return this;
        }

        public Builder keyFields(String... keyFields) {
            if (keyFields != null) {
                this.keyFields.addAll(Arrays.asList(keyFields));
            }
            return this;
        }

        public Builder keyFields(List<String> keyFields) {
            if (keyFields != null) {
                this.keyFields.addAll(keyFields);
            }
            return this;
        }

        public Builder ignoreCase(boolean ignoreCase) {
            this.ignoreCase = ignoreCase;
            return this;
        }

        public Builder ignoreField(String field) {
            if (field != null && !field.isEmpty()) {
                this.ignoredFields.add(field);
            }
            return this;
        }

        public Builder ignoreFields(String... fields) {
            if (fields != null) {
                this.ignoredFields.addAll(Arrays.asList(fields));
            }
            return this;
        }

        public Builder useLeftSchema(boolean useLeftSchema) {
            this.useLeftSchema = useLeftSchema;
            return this;
        }

        public DataDiff build() {
            if (keyFields.isEmpty()) {
                throw new IllegalStateException(
                    "DataDiff.builder() requires at least one keyField"
                );
            }
            return new DataDiff(
                null,
                null,
                new ArrayList<>(keyFields),
                ignoreCase,
                new HashSet<>(ignoredFields),
                useLeftSchema
            );
        }
    }
}
