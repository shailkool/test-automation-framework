package com.smbc.raft.core.data;

import com.smbc.raft.core.filter.CsvFilterEngine;
import com.smbc.raft.core.filter.FilterRule;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.extern.log4j.Log4j2;

/**
 * Engine for performing relational joins on DataTables (List<Map<String, String>>). Supports
 * pre-join filtering and multi-key hash joins.
 */
@Log4j2
public class DataTableJoinEngine {

  /** Joins two tables based on conditions and optional filters. */
  public List<Map<String, String>> join(
      List<Map<String, String>> left,
      List<Map<String, String>> right,
      JoinType type,
      List<JoinCondition> joinConditions,
      List<FilterRule> leftFilters,
      List<FilterRule> rightFilters) {

    log.info("Starting join operation. Type: {}, Join Conditions: {}", type, joinConditions.size());

    // 1. Predicate Pushdown: Filter source tables before joining
    List<Map<String, String>> filteredLeft = applyFilters(left, leftFilters);
    List<Map<String, String>> filteredRight = applyFilters(right, rightFilters);

    log.info(
        "Join source sizes - Left: {} (after filter), Right: {} (after filter)",
        filteredLeft.size(),
        filteredRight.size());

    if (filteredLeft.isEmpty() && type != JoinType.RIGHT && type != JoinType.FULL) {
      return Collections.emptyList();
    }

    // 2. Perform Join using Hash Join algorithm
    switch (type) {
      case INNER:
        return performInnerJoin(filteredLeft, filteredRight, joinConditions);
      case LEFT:
        return performLeftJoin(filteredLeft, filteredRight, joinConditions);
      default:
        throw new UnsupportedOperationException("Join type " + type + " is not yet implemented.");
    }
  }

  private List<Map<String, String>> applyFilters(
      List<Map<String, String>> data, List<FilterRule> filters) {
    if (data == null || data.isEmpty() || filters == null || filters.isEmpty()) {
      return data == null ? Collections.emptyList() : data;
    }
    return new CsvFilterEngine(filters).filter(data);
  }

  private List<Map<String, String>> performInnerJoin(
      List<Map<String, String>> left,
      List<Map<String, String>> right,
      List<JoinCondition> conditions) {

    Map<String, List<Map<String, String>>> rightIndex = buildIndex(right, conditions, false);
    List<Map<String, String>> result = new ArrayList<>();

    for (Map<String, String> leftRow : left) {
      String key = createKey(leftRow, conditions, true);
      List<Map<String, String>> matches = rightIndex.get(key);

      if (matches != null) {
        for (Map<String, String> match : matches) {
          result.add(mergeRows(leftRow, match));
        }
      }
    }
    return result;
  }

  private List<Map<String, String>> performLeftJoin(
      List<Map<String, String>> left,
      List<Map<String, String>> right,
      List<JoinCondition> conditions) {

    Map<String, List<Map<String, String>>> rightIndex = buildIndex(right, conditions, false);
    List<Map<String, String>> result = new ArrayList<>();

    // Collect all columns from right side for padding in case of no match
    Set<String> rightCols = new LinkedHashSet<>();
    if (!right.isEmpty()) {
      rightCols.addAll(right.get(0).keySet());
    }

    for (Map<String, String> leftRow : left) {
      String key = createKey(leftRow, conditions, true);
      List<Map<String, String>> matches = rightIndex.get(key);

      if (matches != null) {
        for (Map<String, String> match : matches) {
          result.add(mergeRows(leftRow, match));
        }
      } else {
        Map<String, String> padded = new LinkedHashMap<>(leftRow);
        for (String col : rightCols) {
          padded.putIfAbsent(col, "");
        }
        result.add(padded);
      }
    }
    return result;
  }

  private Map<String, List<Map<String, String>>> buildIndex(
      List<Map<String, String>> data, List<JoinCondition> conditions, boolean isLeft) {

    Map<String, List<Map<String, String>>> index = new HashMap<>();
    for (Map<String, String> row : data) {
      String key = createKey(row, conditions, isLeft);
      index.computeIfAbsent(key, k -> new ArrayList<>()).add(row);
    }
    return index;
  }

  private String createKey(
      Map<String, String> row, List<JoinCondition> conditions, boolean isLeft) {
    StringBuilder sb = new StringBuilder();
    for (JoinCondition cond : conditions) {
      String col = isLeft ? cond.getLeftColumn() : cond.getRightColumn();
      sb.append(row.getOrDefault(col, "")).append("|");
    }
    return sb.toString();
  }

  private Map<String, String> mergeRows(Map<String, String> left, Map<String, String> right) {
    Map<String, String> merged = new LinkedHashMap<>(left);
    merged.putAll(right);
    return merged;
  }
}
