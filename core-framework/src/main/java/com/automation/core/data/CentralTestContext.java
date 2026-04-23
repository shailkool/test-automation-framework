package com.automation.core.data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Shared context for data-driven steps to allow state sharing between different
 * step definition classes within a single scenario.
 */
public class CentralTestContext {
    
    private static final ThreadLocal<List<Map<String, String>>> sourceRows = ThreadLocal.withInitial(ArrayList::new);
    private static final ThreadLocal<List<Map<String, String>>> filteredRows = ThreadLocal.withInitial(ArrayList::new);
    private static final ThreadLocal<String> baseFolder = new ThreadLocal<>();

    public static void setSourceRows(List<Map<String, String>> rows) {
        sourceRows.set(rows);
    }

    public static List<Map<String, String>> getSourceRows() {
        return sourceRows.get();
    }

    public static void setFilteredRows(List<Map<String, String>> rows) {
        filteredRows.set(rows);
    }

    public static List<Map<String, String>> getFilteredRows() {
        return filteredRows.get();
    }

    public static void setBaseFolder(String path) {
        baseFolder.set(path);
    }

    public static String getBaseFolder() {
        return baseFolder.get();
    }

    public static void clear() {
        sourceRows.remove();
        filteredRows.remove();
        baseFolder.remove();
    }
}
