package com.smbc.raft.core.filter;

import lombok.Getter;

import java.util.Objects;

/**
 * A single filter predicate applied to one CSV column.
 *
 * <p>Rules are composed by {@link CsvFilterEngine} using logical AND &mdash;
 * a row is retained only if it satisfies every configured rule.
 */
@Getter
public class FilterRule {

    private final String column;
    private final FilterOperator operator;
    private final String value;

    public FilterRule(String column, FilterOperator operator, String value) {
        this.column = Objects.requireNonNull(column, "column");
        this.operator = Objects.requireNonNull(operator, "operator");
        this.value = value == null ? "" : value;
    }

    public FilterRule(String column, String operatorSymbol, String value) {
        this(column, FilterOperator.fromSymbol(operatorSymbol), value);
    }

    /** Evaluate the rule against a single row's column value. */
    public boolean matches(String cellValue) {
        String cell = cellValue == null ? "" : cellValue;
        switch (operator) {
            case EQUALS:
                return cell.equals(value);
            case NOT_EQUALS:
                return !cell.equals(value);
            case CONTAINS:
                return cell.contains(value);
            case STARTS_WITH:
                return cell.startsWith(value);
            case ENDS_WITH:
                return cell.endsWith(value);
            case GREATER_THAN:
                return compare(cell, value) > 0;
            case LESS_THAN:
                return compare(cell, value) < 0;
            case GREATER_THAN_OR_EQUAL:
                return compare(cell, value) >= 0;
            case LESS_THAN_OR_EQUAL:
                return compare(cell, value) <= 0;
            default:
                throw new IllegalStateException("Unhandled operator: " + operator);
        }
    }

    private int compare(String left, String right) {
        try {
            double l = Double.parseDouble(left);
            double r = Double.parseDouble(right);
            return Double.compare(l, r);
        } catch (NumberFormatException ignored) {
            return left.compareTo(right);
        }
    }

    @Override
    public String toString() {
        return String.format("%s %s %s", column, operator.getSymbol(), value);
    }
}
