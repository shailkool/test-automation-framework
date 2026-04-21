package com.automation.core.filter;

import lombok.Getter;

/**
 * Supported comparison operators for {@link FilterRule}.
 *
 * <p>Operators fall into two families:
 * <ul>
 *   <li><b>Ordering</b> ({@code >}, {@code <}, {@code >=}, {@code <=}) &mdash;
 *       perform numeric comparison when both operands parse as numbers,
 *       otherwise fall back to lexicographic comparison.</li>
 *   <li><b>Equality / text</b> ({@code ==}, {@code !=}, {@code contains},
 *       {@code startsWith}, {@code endsWith}) &mdash; operate on string
 *       values.</li>
 * </ul>
 */
public enum FilterOperator {

    EQUALS("=="),
    NOT_EQUALS("!="),
    GREATER_THAN(">"),
    LESS_THAN("<"),
    GREATER_THAN_OR_EQUAL(">="),
    LESS_THAN_OR_EQUAL("<="),
    CONTAINS("contains"),
    STARTS_WITH("startsWith"),
    ENDS_WITH("endsWith");

    @Getter
    private final String symbol;

    FilterOperator(String symbol) {
        this.symbol = symbol;
    }

    /** Resolve an operator from its gherkin-friendly symbol. Case-insensitive. */
    public static FilterOperator fromSymbol(String symbol) {
        if (symbol == null) {
            throw new IllegalArgumentException("Operator symbol must not be null");
        }
        String normalised = symbol.trim();
        for (FilterOperator op : values()) {
            if (op.symbol.equalsIgnoreCase(normalised)) {
                return op;
            }
        }
        throw new IllegalArgumentException("Unsupported filter operator: '" + symbol + "'");
    }
}
