package com.smbc.raft.core.data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Shared context for data-driven steps to allow state sharing between different
 * step definition classes within a single scenario.
 */
public class CentralTestContext {
    
    private static final ThreadLocal<List<Map<String, String>>> SOURCE_ROWS = ThreadLocal.withInitial(ArrayList::new);
    private static final ThreadLocal<List<Map<String, String>>> FILTERED_ROWS = ThreadLocal.withInitial(ArrayList::new);
    private static final ThreadLocal<java.util.Map<String, List<Map<String, String>>>> NAMED_TABLES = 
        ThreadLocal.withInitial(java.util.HashMap::new);
    private static final ThreadLocal<String> BASE_FOLDER = new ThreadLocal<>();

    public static void setSourceRows(List<Map<String, String>> rows) {
        SOURCE_ROWS.set(rows);
    }

    public static List<Map<String, String>> getSourceRows() {
        return SOURCE_ROWS.get();
    }

    public static void setFilteredRows(List<Map<String, String>> rows) {
        FILTERED_ROWS.set(rows);
    }

    public static List<Map<String, String>> getFilteredRows() {
        return FILTERED_ROWS.get();
    }

    public static void saveTable(String name, List<Map<String, String>> rows) {
        NAMED_TABLES.get().put(name, rows);
    }

    public static List<Map<String, String>> getTable(String name) {
        return NAMED_TABLES.get().get(name);
    }

    public static void setBaseFolder(String path) {
        BASE_FOLDER.set(path);
    }

    public static String getBaseFolder() {
        return BASE_FOLDER.get();
    }

    public static void clear() {
        SOURCE_ROWS.remove();
        FILTERED_ROWS.remove();
        NAMED_TABLES.remove();
        BASE_FOLDER.remove();
    }
}
