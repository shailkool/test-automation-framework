package com.smbc.raft.core.data;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.log4j.Log4j2;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/** Utility class for handling Excel file operations Supports reading and writing .xlsx files */
@Log4j2
public class ExcelHandler {

  private Workbook workbook;
  private Sheet sheet;
  private String filePath;

  /** Constructor to load existing Excel file */
  public ExcelHandler(String filePath) {
    this.filePath = filePath;
    loadWorkbook();
  }

  /** Constructor to create new Excel file */
  public ExcelHandler(String filePath, String sheetName) {
    this.filePath = filePath;
    this.workbook = new XSSFWorkbook();
    this.sheet = workbook.createSheet(sheetName);
    log.info("Created new Excel workbook with sheet: {}", sheetName);
  }

  /** Load existing workbook */
  private void loadWorkbook() {
    try (FileInputStream fis = new FileInputStream(filePath)) {
      workbook = WorkbookFactory.create(fis);
      log.info("Loaded Excel workbook from: {}", filePath);
    } catch (IOException e) {
      log.error("Error loading Excel file: {}", filePath, e);
      throw new RuntimeException("Failed to load Excel file: " + filePath, e);
    }
  }

  /** Get sheet by name */
  public ExcelHandler selectSheet(String sheetName) {
    sheet = workbook.getSheet(sheetName);
    if (sheet == null) {
      log.warn("Sheet '{}' not found, creating new sheet", sheetName);
      sheet = workbook.createSheet(sheetName);
    }
    log.debug("Selected sheet: {}", sheetName);
    return this;
  }

  /** Get sheet by index */
  public ExcelHandler selectSheet(int sheetIndex) {
    sheet = workbook.getSheetAt(sheetIndex);
    log.debug("Selected sheet at index: {}", sheetIndex);
    return this;
  }

  /** Get cell value as string */
  public String getCellValue(int rowNum, int colNum) {
    Row row = sheet.getRow(rowNum);
    if (row == null) {
      return "";
    }

    Cell cell = row.getCell(colNum);
    return getCellValueAsString(cell);
  }

  /** Get cell value by column name (first row is header) */
  public String getCellValue(int rowNum, String columnName) {
    int colNum = getColumnIndex(columnName);
    if (colNum == -1) {
      log.warn("Column '{}' not found", columnName);
      return "";
    }
    return getCellValue(rowNum, colNum);
  }

  /** Get column index by name from header row */
  private int getColumnIndex(String columnName) {
    Row headerRow = sheet.getRow(0);
    if (headerRow == null) {
      return -1;
    }

    for (Cell cell : headerRow) {
      if (getCellValueAsString(cell).equalsIgnoreCase(columnName)) {
        return cell.getColumnIndex();
      }
    }
    return -1;
  }

  /** Convert cell value to string based on cell type */
  private String getCellValueAsString(Cell cell) {
    if (cell == null) {
      return "";
    }

    switch (cell.getCellType()) {
      case STRING:
        return cell.getStringCellValue();
      case NUMERIC:
        if (DateUtil.isCellDateFormatted(cell)) {
          return cell.getDateCellValue().toString();
        } else {
          return String.valueOf(cell.getNumericCellValue());
        }
      case BOOLEAN:
        return String.valueOf(cell.getBooleanCellValue());
      case FORMULA:
        return cell.getCellFormula();
      case BLANK:
        return "";
      default:
        return "";
    }
  }

  /** Set cell value */
  public ExcelHandler setCellValue(int rowNum, int colNum, String value) {
    Row row = sheet.getRow(rowNum);
    if (row == null) {
      row = sheet.createRow(rowNum);
    }

    Cell cell = row.getCell(colNum);
    if (cell == null) {
      cell = row.createCell(colNum);
    }

    cell.setCellValue(value);
    log.debug("Set cell value at [{}, {}]: {}", rowNum, colNum, value);
    return this;
  }

  /** Set cell value by column name */
  public ExcelHandler setCellValue(int rowNum, String columnName, String value) {
    int colNum = getColumnIndex(columnName);
    if (colNum == -1) {
      log.warn("Column '{}' not found", columnName);
      return this;
    }
    return setCellValue(rowNum, colNum, value);
  }

  /** Get all data from sheet as list of maps (first row as header) */
  public List<Map<String, String>> getAllData() {
    List<Map<String, String>> data = new ArrayList<>();

    Row headerRow = sheet.getRow(0);
    if (headerRow == null) {
      log.warn("No header row found");
      return data;
    }

    // Get headers
    List<String> headers = new ArrayList<>();
    for (Cell cell : headerRow) {
      headers.add(getCellValueAsString(cell));
    }

    // Get data rows
    for (int i = 1; i <= sheet.getLastRowNum(); i++) {
      Row row = sheet.getRow(i);
      if (row == null) {
        continue;
      }

      Map<String, String> rowData = new HashMap<>();
      for (int j = 0; j < headers.size(); j++) {
        String header = headers.get(j);
        String value = getCellValueAsString(row.getCell(j));
        rowData.put(header, value);
      }
      data.add(rowData);
    }

    log.info("Retrieved {} rows from sheet", data.size());
    return data;
  }

  /** Get specific row as map */
  public Map<String, String> getRowData(int rowNum) {
    Map<String, String> rowData = new HashMap<>();

    Row headerRow = sheet.getRow(0);
    Row dataRow = sheet.getRow(rowNum);

    if (headerRow == null || dataRow == null) {
      return rowData;
    }

    for (Cell headerCell : headerRow) {
      String header = getCellValueAsString(headerCell);
      String value = getCellValueAsString(dataRow.getCell(headerCell.getColumnIndex()));
      rowData.put(header, value);
    }

    return rowData;
  }

  /** Get row count (excluding header) */
  public int getRowCount() {
    return sheet.getLastRowNum();
  }

  /** Get column count */
  public int getColumnCount() {
    Row row = sheet.getRow(0);
    return row != null ? row.getLastCellNum() : 0;
  }

  /** Write data to Excel (creates new rows) */
  public ExcelHandler writeData(List<Map<String, String>> data) {
    if (data.isEmpty()) {
      return this;
    }

    // Get or create header row
    Row headerRow = sheet.getRow(0);
    List<String> headers;

    if (headerRow == null) {
      headerRow = sheet.createRow(0);
      headers = new ArrayList<>(data.get(0).keySet());

      // Write headers
      for (int i = 0; i < headers.size(); i++) {
        headerRow.createCell(i).setCellValue(headers.get(i));
      }
    } else {
      headers = new ArrayList<>();
      for (Cell cell : headerRow) {
        headers.add(getCellValueAsString(cell));
      }
    }

    // Write data rows
    int startRow = sheet.getLastRowNum() + 1;
    for (int i = 0; i < data.size(); i++) {
      Row row = sheet.createRow(startRow + i);
      Map<String, String> rowData = data.get(i);

      for (int j = 0; j < headers.size(); j++) {
        String value = rowData.getOrDefault(headers.get(j), "");
        row.createCell(j).setCellValue(value);
      }
    }

    log.info("Wrote {} rows to Excel", data.size());
    return this;
  }

  /** Write single row of data */
  public ExcelHandler writeRow(int rowNum, Map<String, String> rowData) {
    Row headerRow = sheet.getRow(0);
    if (headerRow == null) {
      log.error("Header row not found. Cannot write data.");
      return this;
    }

    Row row = sheet.getRow(rowNum);
    if (row == null) {
      row = sheet.createRow(rowNum);
    }

    for (Cell headerCell : headerRow) {
      String header = getCellValueAsString(headerCell);
      String value = rowData.getOrDefault(header, "");
      int colIndex = headerCell.getColumnIndex();

      Cell cell = row.getCell(colIndex);
      if (cell == null) {
        cell = row.createCell(colIndex);
      }
      cell.setCellValue(value);
    }

    return this;
  }

  /** Create new sheet */
  public ExcelHandler createSheet(String sheetName) {
    sheet = workbook.createSheet(sheetName);
    log.info("Created new sheet: {}", sheetName);
    return this;
  }

  /** Get all sheet names */
  public List<String> getSheetNames() {
    List<String> sheetNames = new ArrayList<>();
    for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
      sheetNames.add(workbook.getSheetName(i));
    }
    return sheetNames;
  }

  /** Auto-size all columns */
  public ExcelHandler autoSizeColumns() {
    if (sheet != null) {
      Row row = sheet.getRow(0);
      if (row != null) {
        for (int i = 0; i < row.getLastCellNum(); i++) {
          sheet.autoSizeColumn(i);
        }
      }
    }
    return this;
  }

  /** Save workbook to file */
  public void save() {
    try (FileOutputStream fos = new FileOutputStream(filePath)) {
      workbook.write(fos);
      log.info("Saved Excel file: {}", filePath);
    } catch (IOException e) {
      log.error("Error saving Excel file: {}", filePath, e);
      throw new RuntimeException("Failed to save Excel file: " + filePath, e);
    }
  }

  /** Save workbook to different file */
  public void saveAs(String newFilePath) {
    try (FileOutputStream fos = new FileOutputStream(newFilePath)) {
      workbook.write(fos);
      this.filePath = newFilePath;
      log.info("Saved Excel file as: {}", newFilePath);
    } catch (IOException e) {
      log.error("Error saving Excel file: {}", newFilePath, e);
      throw new RuntimeException("Failed to save Excel file: " + newFilePath, e);
    }
  }

  /** Close workbook */
  public void close() {
    try {
      if (workbook != null) {
        workbook.close();
        log.debug("Closed Excel workbook");
      }
    } catch (IOException e) {
      log.error("Error closing workbook", e);
    }
  }

  /** Get test data for specific test case */
  public Map<String, String> getTestData(String testCaseName) {
    for (int i = 1; i <= sheet.getLastRowNum(); i++) {
      String testName = getCellValue(i, "TestCase");
      if (testName != null && testName.equalsIgnoreCase(testCaseName)) {
        return getRowData(i);
      }
    }
    log.warn("Test case '{}' not found in Excel", testCaseName);
    return new HashMap<>();
  }

  /** Get all test data where a condition is met */
  public List<Map<String, String>> getTestDataWhere(String columnName, String value) {
    List<Map<String, String>> results = new ArrayList<>();

    for (int i = 1; i <= sheet.getLastRowNum(); i++) {
      String cellValue = getCellValue(i, columnName);
      if (cellValue != null && cellValue.equalsIgnoreCase(value)) {
        results.add(getRowData(i));
      }
    }

    log.info("Found {} rows matching condition", results.size());
    return results;
  }
}
