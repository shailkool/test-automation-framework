package com.smbc.raft.core.data;

import com.smbc.raft.core.reporting.ExtentReportManager;
import com.smbc.raft.core.utils.BaseTest;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Examples demonstrating CSV data handling capabilities
 */
public class CSVDataHandlingTest extends BaseTest {
    
    @Test(description = "Create new CSV file and write data")
    public void testCreateCSVFile() {
        ExtentReportManager.assignCategory("Data", "CSV", "Write");
        
        String filePath = "test-output/sample_products.csv";
        List<String> headers = Arrays.asList("ProductID", "ProductName", "Price", "Stock");
        
        // Create new CSV file
        CSVHandler csv = new CSVHandler(filePath, headers);
        
        // Prepare test data
        Map<String, String> product1 = new HashMap<>();
        product1.put("ProductID", "P001");
        product1.put("ProductName", "Laptop");
        product1.put("Price", "999.99");
        product1.put("Stock", "50");
        
        Map<String, String> product2 = new HashMap<>();
        product2.put("ProductID", "P002");
        product2.put("ProductName", "Mouse");
        product2.put("Price", "25.50");
        product2.put("Stock", "200");
        
        Map<String, String> product3 = new HashMap<>();
        product3.put("ProductID", "P003");
        product3.put("ProductName", "Keyboard");
        product3.put("Price", "75.00");
        product3.put("Stock", "150");
        
        // Add rows
        csv.addRow(product1);
        csv.addRow(product2);
        csv.addRow(product3);
        
        // Save CSV
        csv.save();
        
        ExtentReportManager.logPass("CSV file created with 3 products");
        
        // Verify file was created
        Assert.assertTrue(new java.io.File(filePath).exists(), "CSV file should be created");
    }
    
    @Test(description = "Read data from CSV file", dependsOnMethods = "testCreateCSVFile")
    public void testReadCSVFile() {
        ExtentReportManager.assignCategory("Data", "CSV", "Read");
        
        String filePath = "test-output/sample_products.csv";
        
        CSVHandler csv = new CSVHandler(filePath);
        
        // Read all data
        List<Map<String, String>> allData = csv.getAllData();
        ExtentReportManager.logInfo("Total rows read: " + allData.size());
        
        Assert.assertEquals(allData.size(), 3, "Should have 3 product records");
        
        // Read specific cell
        String productName = csv.getCellValue(0, "ProductName");
        ExtentReportManager.logInfo("First product: " + productName);
        Assert.assertEquals(productName, "Laptop", "First product should be Laptop");
        
        // Read specific row
        Map<String, String> secondProduct = csv.getRow(1);
        ExtentReportManager.logInfo("Second product price: " + secondProduct.get("Price"));
        Assert.assertEquals(secondProduct.get("Price"), "25.50", "Price should match");
        
        // Get column data
        List<String> allPrices = csv.getColumnData("Price");
        ExtentReportManager.logInfo("All prices: " + allPrices);
        Assert.assertEquals(allPrices.size(), 3, "Should have 3 prices");
        
        // Get headers
        List<String> headers = csv.getHeaders();
        ExtentReportManager.logInfo("Headers: " + headers);
        
        ExtentReportManager.logPass("CSV file read successfully");
    }
    
    @Test(description = "Update CSV file data", dependsOnMethods = "testReadCSVFile")
    public void testUpdateCSVFile() {
        ExtentReportManager.assignCategory("Data", "CSV", "Update");
        
        String filePath = "test-output/sample_products.csv";
        
        CSVHandler csv = new CSVHandler(filePath);
        
        // Update a cell value
        csv.setCellValue(0, "Price", "899.99");
        csv.setCellValue(0, "Stock", "45");
        
        // Add a new row
        Map<String, String> newProduct = new HashMap<>();
        newProduct.put("ProductID", "P004");
        newProduct.put("ProductName", "Monitor");
        newProduct.put("Price", "299.99");
        newProduct.put("Stock", "75");
        
        csv.addRow(newProduct);
        
        // Save changes
        csv.save();
        
        // Verify updates
        CSVHandler updatedCsv = new CSVHandler(filePath);
        String updatedPrice = updatedCsv.getCellValue(0, "Price");
        Assert.assertEquals(updatedPrice, "899.99", "Price should be updated");
        
        int rowCount = updatedCsv.getRowCount();
        Assert.assertEquals(rowCount, 4, "Should have 4 products after adding one");
        
        ExtentReportManager.logPass("CSV file updated successfully");
    }
    
    @Test(description = "Filter and search CSV data", dependsOnMethods = "testUpdateCSVFile")
    public void testFilterCSVData() {
        ExtentReportManager.assignCategory("Data", "CSV", "Filter");
        
        String filePath = "test-output/sample_products.csv";
        
        CSVHandler csv = new CSVHandler(filePath);
        
        // Filter by product ID
        List<Map<String, String>> filteredProducts = csv.filterRows("ProductID", "P001");
        ExtentReportManager.logInfo("Products with ID P001: " + filteredProducts.size());
        Assert.assertEquals(filteredProducts.size(), 1, "Should find 1 product");
        
        // Search for products
        List<Map<String, String>> searchResults = csv.searchRows("Mouse");
        ExtentReportManager.logInfo("Search results for 'Mouse': " + searchResults.size());
        Assert.assertTrue(searchResults.size() > 0, "Should find products containing 'Mouse'");
        
        // Get unique product names
        Set<String> uniqueNames = csv.getUniqueValues("ProductName");
        ExtentReportManager.logInfo("Unique product names: " + uniqueNames.size());
        
        ExtentReportManager.logPass("CSV data filtering successful");
    }
    
    @Test(description = "Sort CSV data")
    public void testSortCSVData() {
        ExtentReportManager.assignCategory("Data", "CSV", "Sort");
        
        String filePath = "test-output/sample_products.csv";
        
        CSVHandler csv = new CSVHandler(filePath);
        
        // Sort by product name ascending
        csv.sortByColumn("ProductName", true);
        
        String firstProduct = csv.getCellValue(0, "ProductName");
        ExtentReportManager.logInfo("First product after sort: " + firstProduct);
        
        // Sort by price descending
        csv.sortByColumn("Price", false);
        
        String mostExpensive = csv.getCellValue(0, "ProductName");
        ExtentReportManager.logInfo("Most expensive product: " + mostExpensive);
        
        ExtentReportManager.logPass("CSV sorting successful");
    }
    
    @Test(description = "Delete rows from CSV")
    public void testDeleteCSVRows() {
        ExtentReportManager.assignCategory("Data", "CSV", "Delete");
        
        String filePath = "test-output/sample_products.csv";
        
        CSVHandler csv = new CSVHandler(filePath);
        
        int initialCount = csv.getRowCount();
        ExtentReportManager.logInfo("Initial row count: " + initialCount);
        
        // Delete a row
        csv.deleteRow(0);
        
        int afterDeleteCount = csv.getRowCount();
        ExtentReportManager.logInfo("Row count after delete: " + afterDeleteCount);
        
        Assert.assertEquals(afterDeleteCount, initialCount - 1, 
            "Row count should decrease by 1");
        
        csv.save();
        
        ExtentReportManager.logPass("CSV row deletion successful");
    }
    
    @Test(description = "Work with CSV test data")
    public void testCSVTestData() {
        ExtentReportManager.assignCategory("Data", "CSV", "TestData");
        
        String filePath = "src/test/resources/testdata/user_api_testdata.csv";
        
        CSVHandler csv = new CSVHandler(filePath);
        
        // Get all test data
        List<Map<String, String>> allTests = csv.getAllData();
        ExtentReportManager.logInfo("Total test cases: " + allTests.size());
        
        // Get specific test case
        Map<String, String> testCase = csv.getTestData("TC_API_001");
        if (!testCase.isEmpty()) {
            ExtentReportManager.logInfo("Test case TC_API_001 found");
            ExtentReportManager.logInfo("Name: " + testCase.get("Name"));
            ExtentReportManager.logInfo("Email: " + testCase.get("Email"));
        }
        
        // Filter test cases by expected status
        List<Map<String, String>> successTests = csv.filterRows("ExpectedStatus", "201");
        ExtentReportManager.logInfo("Test cases expecting success: " + successTests.size());
        
        ExtentReportManager.logPass("CSV test data handling successful");
    }
    
    @Test(description = "Export data to CSV")
    public void testExportDataToCSV() {
        ExtentReportManager.assignCategory("Data", "CSV", "Export");
        
        String filePath = "test-output/test_execution_log.csv";
        
        List<String> headers = Arrays.asList("Timestamp", "TestName", "Status", "Duration");
        CSVHandler csv = new CSVHandler(filePath, headers);
        
        // Simulate test execution log
        Map<String, String> log1 = new HashMap<>();
        log1.put("Timestamp", new Date().toString());
        log1.put("TestName", "LoginTest");
        log1.put("Status", "PASS");
        log1.put("Duration", "2.5s");
        
        Map<String, String> log2 = new HashMap<>();
        log2.put("Timestamp", new Date().toString());
        log2.put("TestName", "APITest");
        log2.put("Status", "PASS");
        log2.put("Duration", "1.8s");
        
        csv.addRow(log1);
        csv.addRow(log2);
        csv.save();
        
        ExtentReportManager.logPass("Data exported to CSV successfully");
        
        Assert.assertTrue(new java.io.File(filePath).exists(), 
            "CSV export file should be created");
    }
}
