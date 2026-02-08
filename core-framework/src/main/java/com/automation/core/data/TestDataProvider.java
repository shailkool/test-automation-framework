package com.automation.core.data;

import lombok.extern.log4j.Log4j2;
import org.testng.annotations.DataProvider;

import java.lang.reflect.Method;
import java.util.*;

/**
 * Utility class for TestNG data providers
 * Supports data-driven testing from Excel and CSV files
 */
@Log4j2
public class TestDataProvider {
    
    /**
     * Data provider for Excel files
     * Usage: @Test(dataProvider = "excelDataProvider", dataProviderClass = TestDataProvider.class)
     */
    @DataProvider(name = "excelDataProvider")
    public static Object[][] excelDataProvider(Method method) {
        String testName = method.getName();
        String filePath = getDataFilePath(testName, "xlsx");
        
        if (filePath == null) {
            log.warn("No Excel data file found for test: {}", testName);
            return new Object[0][0];
        }
        
        return getExcelData(filePath, testName);
    }
    
    /**
     * Data provider for CSV files
     * Usage: @Test(dataProvider = "csvDataProvider", dataProviderClass = TestDataProvider.class)
     */
    @DataProvider(name = "csvDataProvider")
    public static Object[][] csvDataProvider(Method method) {
        String testName = method.getName();
        String filePath = getDataFilePath(testName, "csv");
        
        if (filePath == null) {
            log.warn("No CSV data file found for test: {}", testName);
            return new Object[0][0];
        }
        
        return getCSVData(filePath, testName);
    }
    
    /**
     * Get data from Excel file
     */
    public static Object[][] getExcelData(String filePath, String sheetName) {
        try {
            ExcelHandler excel = new ExcelHandler(filePath);
            excel.selectSheet(sheetName);
            
            List<Map<String, String>> data = excel.getAllData();
            excel.close();
            
            if (data.isEmpty()) {
                log.warn("No data found in Excel sheet: {}", sheetName);
                return new Object[0][0];
            }
            
            // Convert to Object[][]
            Object[][] testData = new Object[data.size()][1];
            for (int i = 0; i < data.size(); i++) {
                testData[i][0] = data.get(i);
            }
            
            log.info("Loaded {} rows from Excel file: {}", data.size(), filePath);
            return testData;
            
        } catch (Exception e) {
            log.error("Error reading Excel data from: {}", filePath, e);
            return new Object[0][0];
        }
    }
    
    /**
     * Get data from Excel file (first sheet)
     */
    public static Object[][] getExcelData(String filePath) {
        try {
            ExcelHandler excel = new ExcelHandler(filePath);
            excel.selectSheet(0);
            
            List<Map<String, String>> data = excel.getAllData();
            excel.close();
            
            if (data.isEmpty()) {
                return new Object[0][0];
            }
            
            Object[][] testData = new Object[data.size()][1];
            for (int i = 0; i < data.size(); i++) {
                testData[i][0] = data.get(i);
            }
            
            log.info("Loaded {} rows from Excel file: {}", data.size(), filePath);
            return testData;
            
        } catch (Exception e) {
            log.error("Error reading Excel data from: {}", filePath, e);
            return new Object[0][0];
        }
    }
    
    /**
     * Get data from CSV file
     */
    public static Object[][] getCSVData(String filePath) {
        try {
            CSVHandler csv = new CSVHandler(filePath);
            List<Map<String, String>> data = csv.getAllData();
            
            if (data.isEmpty()) {
                log.warn("No data found in CSV file: {}", filePath);
                return new Object[0][0];
            }
            
            Object[][] testData = new Object[data.size()][1];
            for (int i = 0; i < data.size(); i++) {
                testData[i][0] = data.get(i);
            }
            
            log.info("Loaded {} rows from CSV file: {}", data.size(), filePath);
            return testData;
            
        } catch (Exception e) {
            log.error("Error reading CSV data from: {}", filePath, e);
            return new Object[0][0];
        }
    }
    
    /**
     * Get filtered data from Excel
     */
    public static Object[][] getExcelDataWhere(String filePath, String sheetName, 
                                                String columnName, String value) {
        try {
            ExcelHandler excel = new ExcelHandler(filePath);
            excel.selectSheet(sheetName);
            
            List<Map<String, String>> data = excel.getTestDataWhere(columnName, value);
            excel.close();
            
            if (data.isEmpty()) {
                return new Object[0][0];
            }
            
            Object[][] testData = new Object[data.size()][1];
            for (int i = 0; i < data.size(); i++) {
                testData[i][0] = data.get(i);
            }
            
            log.info("Loaded {} filtered rows from Excel", data.size());
            return testData;
            
        } catch (Exception e) {
            log.error("Error reading filtered Excel data", e);
            return new Object[0][0];
        }
    }
    
    /**
     * Get filtered data from CSV
     */
    public static Object[][] getCSVDataWhere(String filePath, String columnName, String value) {
        try {
            CSVHandler csv = new CSVHandler(filePath);
            List<Map<String, String>> data = csv.filterRows(columnName, value);
            
            if (data.isEmpty()) {
                return new Object[0][0];
            }
            
            Object[][] testData = new Object[data.size()][1];
            for (int i = 0; i < data.size(); i++) {
                testData[i][0] = data.get(i);
            }
            
            log.info("Loaded {} filtered rows from CSV", data.size());
            return testData;
            
        } catch (Exception e) {
            log.error("Error reading filtered CSV data", e);
            return new Object[0][0];
        }
    }
    
    /**
     * Get specific test case data from Excel
     */
    public static Map<String, String> getExcelTestCase(String filePath, String sheetName, 
                                                         String testCaseName) {
        try {
            ExcelHandler excel = new ExcelHandler(filePath);
            excel.selectSheet(sheetName);
            
            Map<String, String> testData = excel.getTestData(testCaseName);
            excel.close();
            
            return testData;
            
        } catch (Exception e) {
            log.error("Error reading test case from Excel", e);
            return new HashMap<>();
        }
    }
    
    /**
     * Get specific test case data from CSV
     */
    public static Map<String, String> getCSVTestCase(String filePath, String testCaseName) {
        try {
            CSVHandler csv = new CSVHandler(filePath);
            return csv.getTestData(testCaseName);
            
        } catch (Exception e) {
            log.error("Error reading test case from CSV", e);
            return new HashMap<>();
        }
    }
    
    /**
     * Helper method to get data file path
     */
    private static String getDataFilePath(String testName, String extension) {
        // Try common locations
        String[] locations = {
            "src/test/resources/testdata/" + testName + "." + extension,
            "testdata/" + testName + "." + extension,
            "src/test/resources/data/" + testName + "." + extension,
            "data/" + testName + "." + extension
        };
        
        for (String location : locations) {
            if (new java.io.File(location).exists()) {
                return location;
            }
        }
        
        return null;
    }
    
    /**
     * Convert Excel data to 2D array with separate parameters
     * Useful when you want each column as a separate parameter
     */
    public static Object[][] getExcelDataAsParameters(String filePath, String sheetName) {
        try {
            ExcelHandler excel = new ExcelHandler(filePath);
            excel.selectSheet(sheetName);
            
            List<Map<String, String>> data = excel.getAllData();
            excel.close();
            
            if (data.isEmpty()) {
                return new Object[0][0];
            }
            
            // Get headers to maintain column order
            List<String> headers = new ArrayList<>(data.get(0).keySet());
            
            Object[][] testData = new Object[data.size()][headers.size()];
            for (int i = 0; i < data.size(); i++) {
                Map<String, String> row = data.get(i);
                for (int j = 0; j < headers.size(); j++) {
                    testData[i][j] = row.get(headers.get(j));
                }
            }
            
            log.info("Loaded {} rows with {} parameters from Excel", data.size(), headers.size());
            return testData;
            
        } catch (Exception e) {
            log.error("Error reading Excel data as parameters", e);
            return new Object[0][0];
        }
    }
    
    /**
     * Convert CSV data to 2D array with separate parameters
     */
    public static Object[][] getCSVDataAsParameters(String filePath) {
        try {
            CSVHandler csv = new CSVHandler(filePath);
            List<Map<String, String>> data = csv.getAllData();
            
            if (data.isEmpty()) {
                return new Object[0][0];
            }
            
            List<String> headers = csv.getHeaders();
            
            Object[][] testData = new Object[data.size()][headers.size()];
            for (int i = 0; i < data.size(); i++) {
                Map<String, String> row = data.get(i);
                for (int j = 0; j < headers.size(); j++) {
                    testData[i][j] = row.get(headers.get(j));
                }
            }
            
            log.info("Loaded {} rows with {} parameters from CSV", data.size(), headers.size());
            return testData;
            
        } catch (Exception e) {
            log.error("Error reading CSV data as parameters", e);
            return new Object[0][0];
        }
    }
}
