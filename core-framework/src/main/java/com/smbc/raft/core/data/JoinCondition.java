package com.smbc.raft.core.data;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a single condition for joining two data tables.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class JoinCondition {
    private String leftColumn;
    private String rightColumn;
    private String operator = "=="; // Default to equality
}
