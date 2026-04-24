package com.smbc.raft.core.diff;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Shared CSV loader used by the diff utilities.
 *
 * <p>Always reads files as UTF-8 (not the platform default charset, which is
 * {@code cp1252} on Windows and produces garbage for UTF-8 content), and
 * strips a UTF-8 byte-order mark from the first header cell so the first
 * column name is matched correctly by {@code --keys}.
 *
 * <p>Also exposes {@link #ensureKeyColumnsPresent} which validates, before
 * the expensive diff runs, that every configured key column actually exists
 * in the loaded data &mdash; without this check a misspelt key silently
 * resolves to the empty string for every row and collapses the whole dataset
 * into a single comparison.
 */
final class CsvLoader {

    private static final char UTF8_BOM = '﻿';

    private CsvLoader() {
    }

    static List<Map<String, String>> load(Path file) {
        try (CSVReader reader = new CSVReader(new InputStreamReader(
                new FileInputStream(file.toFile()), StandardCharsets.UTF_8))) {
            List<String[]> all = reader.readAll();
            if (all.isEmpty()) return List.of();
            String[] headers = all.get(0).clone();
            if (headers.length > 0 && headers[0] != null
                && !headers[0].isEmpty()
                && headers[0].charAt(0) == UTF8_BOM) {
                headers[0] = headers[0].substring(1);
            }
            List<Map<String, String>> rows = new ArrayList<>(all.size() - 1);
            for (int i = 1; i < all.size(); i++) {
                String[] cells = all.get(i);
                Map<String, String> row = new LinkedHashMap<>();
                for (int c = 0; c < headers.length; c++) {
                    row.put(headers[c], c < cells.length ? cells[c] : "");
                }
                rows.add(row);
            }
            return rows;
        } catch (IOException | CsvException e) {
            throw new RuntimeException("Failed to load CSV " + file + ": " + e.getMessage(), e);
        }
    }

    /**
     * Verify that every key column is present in the loaded dataset's header
     * row. Throws {@link IllegalArgumentException} with the missing keys and
     * the actual columns when any key is absent. An empty dataset passes
     * silently (no rows to match).
     */
    static void ensureKeyColumnsPresent(Path file,
                                        List<String> keys,
                                        List<Map<String, String>> rows) {
        if (rows.isEmpty()) return;
        Map<String, String> firstRow = rows.get(0);
        List<String> missing = new ArrayList<>();
        for (String key : keys) {
            if (!firstRow.containsKey(key)) missing.add(key);
        }
        if (!missing.isEmpty()) {
            throw new IllegalArgumentException(String.format(
                "Key column(s) %s not found in %s. Available columns: %s",
                missing, file, firstRow.keySet()
            ));
        }
    }
}
