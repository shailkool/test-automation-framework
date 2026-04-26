package com.smbc.raft.core.data;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.extern.log4j.Log4j2;

/** Utility class for handling CSV file operations Supports reading and writing CSV files */
@Log4j2
public class CSVHandler {

  private String filePath;
  private List<String[]> data;
  private List<String> headers;

  /** Constructor to load existing CSV file */
  public CSVHandler(String filePath) {
    this.filePath = filePath;
    loadCSV();
  }

  /** Constructor to create new CSV file with headers */
  public CSVHandler(String filePath, List<String> headers) {
    this.filePath = filePath;
    this.headers = new ArrayList<>(headers);
    this.data = new ArrayList<>();
    log.info("Created new CSV handler with {} columns", headers.size());
  }

  /** Load CSV file */
  private void loadCSV() {
    try (CSVReader reader = new CSVReader(new FileReader(filePath))) {
      data = reader.readAll();
      if (!data.isEmpty()) {
        headers = Arrays.asList(data.get(0));
        data.remove(0); // Remove header row from data
      }
      log.info("Loaded CSV file with {} rows from: {}", data.size(), filePath);
    } catch (IOException | CsvException e) {
      log.error("Error loading CSV file: {}", filePath, e);
      throw new RuntimeException("Failed to load CSV file: " + filePath, e);
    }
  }

  /** Get all data as list of maps */
  public List<Map<String, String>> getAllData() {
    List<Map<String, String>> result = new ArrayList<>();

    for (String[] row : data) {
      Map<String, String> rowMap = new HashMap<>();
      for (int i = 0; i < headers.size() && i < row.length; i++) {
        rowMap.put(headers.get(i), row[i]);
      }
      result.add(rowMap);
    }

    log.debug("Retrieved {} rows from CSV", result.size());
    return result;
  }

  /** Get specific row as map */
  public Map<String, String> getRow(int rowIndex) {
    if (rowIndex < 0 || rowIndex >= data.size()) {
      log.warn("Row index {} out of bounds", rowIndex);
      return new HashMap<>();
    }

    Map<String, String> rowMap = new HashMap<>();
    String[] row = data.get(rowIndex);

    for (int i = 0; i < headers.size() && i < row.length; i++) {
      rowMap.put(headers.get(i), row[i]);
    }

    return rowMap;
  }

  /** Get cell value by row and column name */
  public String getCellValue(int rowIndex, String columnName) {
    int colIndex = headers.indexOf(columnName);
    if (colIndex == -1) {
      log.warn("Column '{}' not found", columnName);
      return "";
    }

    if (rowIndex < 0 || rowIndex >= data.size()) {
      log.warn("Row index {} out of bounds", rowIndex);
      return "";
    }

    String[] row = data.get(rowIndex);
    if (colIndex < row.length) {
      return row[colIndex];
    }

    return "";
  }

  /** Get cell value by row and column index */
  public String getCellValue(int rowIndex, int colIndex) {
    if (rowIndex < 0 || rowIndex >= data.size()) {
      log.warn("Row index {} out of bounds", rowIndex);
      return "";
    }

    String[] row = data.get(rowIndex);
    if (colIndex < 0 || colIndex >= row.length) {
      log.warn("Column index {} out of bounds", colIndex);
      return "";
    }

    return row[colIndex];
  }

  /** Get all values from a specific column */
  public List<String> getColumnData(String columnName) {
    int colIndex = headers.indexOf(columnName);
    if (colIndex == -1) {
      log.warn("Column '{}' not found", columnName);
      return new ArrayList<>();
    }

    List<String> columnData = new ArrayList<>();
    for (String[] row : data) {
      if (colIndex < row.length) {
        columnData.add(row[colIndex]);
      } else {
        columnData.add("");
      }
    }

    return columnData;
  }

  /** Get all values from a specific column by index */
  public List<String> getColumnData(int colIndex) {
    if (colIndex < 0 || colIndex >= headers.size()) {
      log.warn("Column index {} out of bounds", colIndex);
      return new ArrayList<>();
    }

    List<String> columnData = new ArrayList<>();
    for (String[] row : data) {
      if (colIndex < row.length) {
        columnData.add(row[colIndex]);
      } else {
        columnData.add("");
      }
    }

    return columnData;
  }

  /** Get row count (excluding header) */
  public int getRowCount() {
    return data.size();
  }

  /** Get column count */
  public int getColumnCount() {
    return headers.size();
  }

  /** Get headers */
  public List<String> getHeaders() {
    return new ArrayList<>(headers);
  }

  /** Add a new row */
  public CSVHandler addRow(Map<String, String> rowData) {
    String[] row = new String[headers.size()];

    for (int i = 0; i < headers.size(); i++) {
      String header = headers.get(i);
      row[i] = rowData.getOrDefault(header, "");
    }

    data.add(row);
    log.debug("Added new row to CSV");
    return this;
  }

  /** Add multiple rows */
  public CSVHandler addRows(List<Map<String, String>> rows) {
    for (Map<String, String> rowData : rows) {
      addRow(rowData);
    }
    log.info("Added {} rows to CSV", rows.size());
    return this;
  }

  /** Update cell value */
  public CSVHandler setCellValue(int rowIndex, String columnName, String value) {
    int colIndex = headers.indexOf(columnName);
    if (colIndex == -1) {
      log.warn("Column '{}' not found", columnName);
      return this;
    }

    if (rowIndex < 0 || rowIndex >= data.size()) {
      log.warn("Row index {} out of bounds", rowIndex);
      return this;
    }

    String[] row = data.get(rowIndex);
    if (colIndex < row.length) {
      row[colIndex] = value;
      log.debug("Updated cell at [{}, {}] to: {}", rowIndex, columnName, value);
    }

    return this;
  }

  /** Update cell value by indices */
  public CSVHandler setCellValue(int rowIndex, int colIndex, String value) {
    if (rowIndex < 0 || rowIndex >= data.size()) {
      log.warn("Row index {} out of bounds", rowIndex);
      return this;
    }

    String[] row = data.get(rowIndex);
    if (colIndex >= 0 && colIndex < row.length) {
      row[colIndex] = value;
      log.debug("Updated cell at [{}, {}] to: {}", rowIndex, colIndex, value);
    }

    return this;
  }

  /** Update entire row */
  public CSVHandler updateRow(int rowIndex, Map<String, String> rowData) {
    if (rowIndex < 0 || rowIndex >= data.size()) {
      log.warn("Row index {} out of bounds", rowIndex);
      return this;
    }

    String[] row = new String[headers.size()];
    for (int i = 0; i < headers.size(); i++) {
      String header = headers.get(i);
      row[i] = rowData.getOrDefault(header, "");
    }

    data.set(rowIndex, row);
    log.debug("Updated row at index: {}", rowIndex);
    return this;
  }

  /** Delete row */
  public CSVHandler deleteRow(int rowIndex) {
    if (rowIndex >= 0 && rowIndex < data.size()) {
      data.remove(rowIndex);
      log.debug("Deleted row at index: {}", rowIndex);
    } else {
      log.warn("Row index {} out of bounds", rowIndex);
    }
    return this;
  }

  /** Filter rows based on column value */
  public List<Map<String, String>> filterRows(String columnName, String value) {
    List<Map<String, String>> filtered = new ArrayList<>();

    int colIndex = headers.indexOf(columnName);
    if (colIndex == -1) {
      log.warn("Column '{}' not found", columnName);
      return filtered;
    }

    for (String[] row : data) {
      if (colIndex < row.length && row[colIndex].equals(value)) {
        Map<String, String> rowMap = new HashMap<>();
        for (int i = 0; i < headers.size() && i < row.length; i++) {
          rowMap.put(headers.get(i), row[i]);
        }
        filtered.add(rowMap);
      }
    }

    log.info("Filtered {} rows where {}={}", filtered.size(), columnName, value);
    return filtered;
  }

  /** Search rows containing a value in any column */
  public List<Map<String, String>> searchRows(String searchTerm) {
    List<Map<String, String>> results = new ArrayList<>();

    for (String[] row : data) {
      boolean found = false;
      for (String cell : row) {
        if (cell != null
            && cell.toLowerCase(java.util.Locale.ROOT)
                .contains(searchTerm.toLowerCase(java.util.Locale.ROOT))) {
          found = true;
          break;
        }
      }

      if (found) {
        Map<String, String> rowMap = new HashMap<>();
        for (int i = 0; i < headers.size() && i < row.length; i++) {
          rowMap.put(headers.get(i), row[i]);
        }
        results.add(rowMap);
      }
    }

    log.info("Found {} rows containing '{}'", results.size(), searchTerm);
    return results;
  }

  /** Get test data for specific test case */
  public Map<String, String> getTestData(String testCaseName) {
    int testCaseColIndex = headers.indexOf("TestCase");
    if (testCaseColIndex == -1) {
      log.warn("'TestCase' column not found");
      return new HashMap<>();
    }

    for (String[] row : data) {
      if (testCaseColIndex < row.length && row[testCaseColIndex].equalsIgnoreCase(testCaseName)) {

        Map<String, String> testData = new HashMap<>();
        for (int i = 0; i < headers.size() && i < row.length; i++) {
          testData.put(headers.get(i), row[i]);
        }
        return testData;
      }
    }

    log.warn("Test case '{}' not found in CSV", testCaseName);
    return new HashMap<>();
  }

  /** Save CSV to file */
  public void save() {
    try (CSVWriter writer = new CSVWriter(new FileWriter(filePath))) {
      // Write headers
      writer.writeNext(headers.toArray(new String[0]));

      // Write data
      writer.writeAll(data);

      log.info("Saved CSV file with {} rows to: {}", data.size(), filePath);
    } catch (IOException e) {
      log.error("Error saving CSV file: {}", filePath, e);
      throw new RuntimeException("Failed to save CSV file: " + filePath, e);
    }
  }

  /** Save CSV to different file */
  public void saveAs(String newFilePath) {
    try (CSVWriter writer = new CSVWriter(new FileWriter(newFilePath))) {
      // Write headers
      writer.writeNext(headers.toArray(new String[0]));

      // Write data
      writer.writeAll(data);

      this.filePath = newFilePath;
      log.info("Saved CSV file as: {}", newFilePath);
    } catch (IOException e) {
      log.error("Error saving CSV file: {}", newFilePath, e);
      throw new RuntimeException("Failed to save CSV file: " + newFilePath, e);
    }
  }

  /** Clear all data (keeps headers) */
  public CSVHandler clearData() {
    data.clear();
    log.info("Cleared all CSV data");
    return this;
  }

  /** Get unique values from a column */
  public Set<String> getUniqueValues(String columnName) {
    List<String> columnData = getColumnData(columnName);
    return new HashSet<>(columnData);
  }

  /** Sort data by column */
  public CSVHandler sortByColumn(String columnName, boolean ascending) {
    int colIndex = headers.indexOf(columnName);
    if (colIndex == -1) {
      log.warn("Column '{}' not found", columnName);
      return this;
    }

    final int sortIndex = colIndex;
    data.sort(
        (row1, row2) -> {
          String val1 = sortIndex < row1.length ? row1[sortIndex] : "";
          String val2 = sortIndex < row2.length ? row2[sortIndex] : "";
          return ascending ? val1.compareTo(val2) : val2.compareTo(val1);
        });

    log.info("Sorted data by column: {}", columnName);
    return this;
  }
}
