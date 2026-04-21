package com.automation.core.filter;

import com.automation.core.data.CSVHandler;
import lombok.extern.log4j.Log4j2;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Filters CSV data against an ordered list of {@link FilterRule}s.
 *
 * <p>All rules are combined with logical AND: a row survives only when every
 * rule matches its respective column. The engine is stateless across calls;
 * each invocation of {@link #filter} produces a new result list and leaves
 * the input untouched.
 *
 * <p>Rules may be supplied programmatically, from a Gherkin data table, or
 * from an external &quot;configuration&quot; CSV file whose header row is
 * {@code column,operator,value}.
 */
@Log4j2
public class CsvFilterEngine {

    private final List<FilterRule> rules;

    public CsvFilterEngine(List<FilterRule> rules) {
        this.rules = rules == null ? Collections.emptyList() : new ArrayList<>(rules);
    }

    public List<FilterRule> getRules() {
        return Collections.unmodifiableList(rules);
    }

    /**
     * Apply every configured rule to {@code rows} and return the surviving
     * records as a new list. The iteration order of input rows is preserved.
     */
    public List<Map<String, String>> filter(List<Map<String, String>> rows) {
        if (rows == null || rows.isEmpty()) {
            return new ArrayList<>();
        }
        List<Map<String, String>> result = new ArrayList<>();
        for (Map<String, String> row : rows) {
            if (matchesAll(row)) {
                result.add(new LinkedHashMap<>(row));
            }
        }
        log.info("Filter engine applied {} rules: {} in -> {} out",
            rules.size(), rows.size(), result.size());
        return result;
    }

    /**
     * Convenience overload that reads {@code csvPath} via {@link CSVHandler}
     * and filters the resulting rows.
     */
    public List<Map<String, String>> filterCsvFile(String csvPath) {
        CSVHandler handler = new CSVHandler(csvPath);
        return filter(handler.getAllData());
    }

    private boolean matchesAll(Map<String, String> row) {
        for (FilterRule rule : rules) {
            if (!row.containsKey(rule.getColumn())) {
                log.warn("Row missing column '{}'; rule {} evaluates to false",
                    rule.getColumn(), rule);
                return false;
            }
            if (!rule.matches(row.get(rule.getColumn()))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Load filter rules from an external configuration CSV with the header
     * {@code column,operator,value}. Header order is flexible; matching is
     * case-insensitive.
     */
    public static List<FilterRule> loadRulesFromCsv(String configPath) {
        Path path = Paths.get(configPath);
        List<String> lines;
        try {
            lines = Files.readAllLines(path);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read filter config: " + configPath, e);
        }
        if (lines.isEmpty()) {
            return new ArrayList<>();
        }

        List<String> headers = splitCsvLine(lines.get(0));
        int columnIdx = headerIndex(headers, "column");
        int operatorIdx = headerIndex(headers, "operator");
        int valueIdx = headerIndex(headers, "value");

        List<FilterRule> rules = new ArrayList<>();
        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line == null || line.isBlank()) {
                continue;
            }
            List<String> fields = splitCsvLine(line);
            if (fields.size() <= Math.max(columnIdx, Math.max(operatorIdx, valueIdx))) {
                log.warn("Skipping malformed config row: '{}'", line);
                continue;
            }
            rules.add(new FilterRule(
                fields.get(columnIdx).trim(),
                fields.get(operatorIdx).trim(),
                fields.get(valueIdx).trim()
            ));
        }
        return rules;
    }

    private static int headerIndex(List<String> headers, String name) {
        for (int i = 0; i < headers.size(); i++) {
            if (headers.get(i).trim().equalsIgnoreCase(name)) {
                return i;
            }
        }
        throw new IllegalArgumentException(
            "Filter config missing required header '" + name + "'. Found: " + headers
        );
    }

    private static List<String> splitCsvLine(String line) {
        return Arrays.asList(line.split(",", -1));
    }
}
