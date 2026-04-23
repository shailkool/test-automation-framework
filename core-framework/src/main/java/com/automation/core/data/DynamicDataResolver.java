package com.automation.core.data;

import lombok.extern.log4j.Log4j2;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Centrally manages and resolves dynamic data placeholders (e.g., ${TC-001.UTI}).
 * Uses ThreadLocal to ensure thread safety during parallel execution.
 */
@Log4j2
public class DynamicDataResolver {
    
    private static final ThreadLocal<Map<String, Map<String, String>>> store = ThreadLocal.withInitial(HashMap::new);
    private static final Pattern VAR_PATTERN = Pattern.compile("\\$\\{([^\\.]+)\\.([^\\}]+)\\}");

    /**
     * Stores a mapping of field-value pairs for a specific identifier (e.g., Test Case ID).
     */
    public static void storeData(String id, Map<String, String> data) {
        if (id == null || data == null) return;
        store.get().put(id, data);
        log.debug("Stored dynamic data for ID: {}", id);
    }

    /**
     * Clears all stored data for the current thread.
     */
    public static void clear() {
        store.get().clear();
    }

    /**
     * Resolves all placeholders in the input string using the stored data.
     * Format: ${Identifier.FieldName}
     */
    public static String resolve(String input) {
        if (input == null || !input.contains("${")) {
            return input;
        }

        Matcher matcher = VAR_PATTERN.matcher(input);
        StringBuilder sb = new StringBuilder();
        int lastEnd = 0;

        while (matcher.find()) {
            sb.append(input, lastEnd, matcher.start());
            String id = matcher.group(1);
            String fieldName = matcher.group(2);
            
            Map<String, String> data = store.get().get(id);
            if (data != null && data.containsKey(fieldName)) {
                String value = data.get(fieldName);
                sb.append(value);
                log.trace("Resolved ${}.{} to '{}'", id, fieldName, value);
            } else {
                log.warn("Could not resolve variable: {}.{} - placeholder kept.", id, fieldName);
                sb.append(matcher.group(0));
            }
            lastEnd = matcher.end();
        }
        sb.append(input.substring(lastEnd));
        return sb.toString();
    }

    /**
     * Resolves all placeholders within a Cucumber DataTable (represented as a list of maps).
     */
    public static List<Map<String, String>> resolveTable(List<Map<String, String>> table) {
        if (table == null || table.isEmpty()) {
            return table;
        }
        
        log.debug("Resolving dynamic data in table with {} rows", table.size());
        List<Map<String, String>> resolved = new ArrayList<>();
        for (Map<String, String> row : table) {
            Map<String, String> resolvedRow = new LinkedHashMap<>();
            for (Map.Entry<String, String> entry : row.entrySet()) {
                resolvedRow.put(entry.getKey(), resolve(entry.getValue()));
            }
            resolved.add(resolvedRow);
        }
        return resolved;
    }
}
