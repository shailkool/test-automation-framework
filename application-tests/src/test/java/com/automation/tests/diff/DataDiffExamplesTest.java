package com.automation.tests.diff;

import com.automation.core.data.CSVHandler;
import com.automation.core.data.ExcelHandler;
import com.automation.core.diff.DataDiff;
import com.automation.core.diff.DiffReportGenerator;
import com.automation.core.diff.DiffResult;
import com.automation.core.reporting.ExtentReportManager;
import com.automation.core.utils.BaseTest;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.*;

/**
 * Examples demonstrating DataDiff for comparing datasets
 */
public class DataDiffExamplesTest extends BaseTest {
    
    @Test(description = "Compare identical datasets")
    public void testIdenticalData() {
        ExtentReportManager.assignCategory("Diff", "Comparison", "Identical");
        
        // Create identical datasets
        List<Map<String, String>> expected = createSampleData();
        List<Map<String, String>> actual = createSampleData();
        
        // Compare
        DataDiff diff = DataDiff.builder()
                .keyField("ID")
                .build();
        
        DiffResult result = diff.compare(expected, actual);
        
        Assert.assertTrue(result.isIdentical(), "Datasets should be identical");
        Assert.assertEquals(result.getMatchedRows(), 3);
        Assert.assertEquals(result.getAddedRows(), 0);
        Assert.assertEquals(result.getDeletedRows(), 0);
        Assert.assertEquals(result.getModifiedRows(), 0);
        
        ExtentReportManager.logPass("Identical data detected correctly");
    }
    
    @Test(description = "Compare datasets with modifications")
    public void testModifiedData() {
        ExtentReportManager.assignCategory("Diff", "Comparison", "Modified");
        
        List<Map<String, String>> expected = createSampleData();
        List<Map<String, String>> actual = createSampleData();
        
        // Modify one record
        actual.get(1).put("Name", "Modified Name");
        actual.get(1).put("Email", "modified@example.com");
        
        // Compare
        DataDiff diff = DataDiff.builder()
                .keyField("ID")
                .build();
        
        DiffResult result = diff.compare(expected, actual);
        
        Assert.assertFalse(result.isIdentical(), "Datasets should be different");
        Assert.assertEquals(result.getModifiedRows(), 1);
        Assert.assertEquals(result.getMatchedRows(), 2);
        
        // Check field differences
        DiffResult.RowDifference rowDiff = result.getModifiedRecords().get(0);
        Assert.assertEquals(rowDiff.getFieldDifferences().size(), 2);
        
        ExtentReportManager.logPass("Modified data detected: " + 
            rowDiff.getFieldDifferences().size() + " fields changed");
    }
    
    @Test(description = "Compare datasets with added and deleted records")
    public void testAddedAndDeletedRecords() {
        ExtentReportManager.assignCategory("Diff", "Comparison", "Add/Delete");
        
        List<Map<String, String>> expected = createSampleData();
        List<Map<String, String>> actual = new ArrayList<>(createSampleData());
        
        // Remove one record (deleted)
        actual.remove(0);
        
        // Add new record (added)
        Map<String, String> newRecord = new HashMap<>();
        newRecord.put("ID", "4");
        newRecord.put("Name", "New User");
        newRecord.put("Email", "new@example.com");
        newRecord.put("Status", "Active");
        actual.add(newRecord);
        
        // Compare
        DataDiff diff = DataDiff.builder()
                .keyField("ID")
                .build();
        
        DiffResult result = diff.compare(expected, actual);
        
        Assert.assertEquals(result.getDeletedRows(), 1, "Should have 1 deleted record");
        Assert.assertEquals(result.getAddedRows(), 1, "Should have 1 added record");
        Assert.assertEquals(result.getMatchedRows(), 2);
        
        ExtentReportManager.logPass(String.format(
            "Changes detected: %d added, %d deleted", 
            result.getAddedRows(), result.getDeletedRows()
        ));
    }
    
    @Test(description = "Compare with case-insensitive matching")
    public void testCaseInsensitiveComparison() {
        ExtentReportManager.assignCategory("Diff", "Comparison", "Case-Insensitive");
        
        List<Map<String, String>> expected = createSampleData();
        List<Map<String, String>> actual = createSampleData();
        
        // Change case
        actual.get(0).put("Name", "JOHN DOE");
        actual.get(0).put("Email", "JOHN@EXAMPLE.COM");
        
        // Compare with case-insensitive option
        DataDiff diff = DataDiff.builder()
                .keyField("ID")
                .ignoreCase(true)
                .build();
        
        DiffResult result = diff.compare(expected, actual);
        
        Assert.assertTrue(result.isIdentical(), "Should match with case-insensitive comparison");
        
        ExtentReportManager.logPass("Case-insensitive comparison successful");
    }
    
    @Test(description = "Compare with ignored fields")
    public void testIgnoreFields() {
        ExtentReportManager.assignCategory("Diff", "Comparison", "Ignore Fields");
        
        List<Map<String, String>> expected = createSampleData();
        List<Map<String, String>> actual = createSampleData();
        
        // Modify status field
        actual.get(0).put("Status", "Inactive");
        actual.get(1).put("Status", "Pending");
        
        // Compare ignoring Status field
        DataDiff diff = DataDiff.builder()
                .keyField("ID")
                .ignoreField("Status")
                .build();
        
        DiffResult result = diff.compare(expected, actual);
        
        Assert.assertTrue(result.isIdentical(), "Should be identical when ignoring Status");
        
        ExtentReportManager.logPass("Field ignore functionality working correctly");
    }
    
    @Test(description = "Generate HTML report for differences")
    public void testGenerateHTMLReport() {
        ExtentReportManager.assignCategory("Diff", "Report", "HTML");
        
        List<Map<String, String>> expected = createSampleData();
        List<Map<String, String>> actual = createSampleData();
        
        // Make various changes
        actual.get(0).put("Name", "Modified Name");  // Modify
        actual.remove(1);  // Delete
        Map<String, String> newRecord = new HashMap<>();
        newRecord.put("ID", "4");
        newRecord.put("Name", "New User");
        newRecord.put("Email", "new@example.com");
        newRecord.put("Status", "Active");
        actual.add(newRecord);  // Add
        
        // Compare
        DataDiff diff = DataDiff.builder()
                .keyField("ID")
                .build();
        
        DiffResult result = diff.compare(expected, actual);
        
        // Generate HTML report
        DiffReportGenerator generator = new DiffReportGenerator();
        String reportPath = "test-output/diff-report-example.html";
        generator.saveReport(result, "Sample Data Comparison Report", reportPath);
        
        Assert.assertTrue(new java.io.File(reportPath).exists(), 
            "HTML report should be generated");
        
        ExtentReportManager.logPass("HTML diff report generated: " + reportPath);
    }
    
    @Test(description = "Compare CSV files")
    public void testCompareCSVFiles() {
        ExtentReportManager.assignCategory("Diff", "CSV", "Comparison");
        
        // Create two CSV files
        String expectedPath = "test-output/expected_users.csv";
        String actualPath = "test-output/actual_users.csv";
        
        List<String> headers = Arrays.asList("ID", "Name", "Email", "Status");
        
        // Expected CSV
        CSVHandler expectedCsv = new CSVHandler(expectedPath, headers);
        expectedCsv.addRows(createSampleData());
        expectedCsv.save();
        
        // Actual CSV (with differences)
        CSVHandler actualCsv = new CSVHandler(actualPath, headers);
        List<Map<String, String>> actualData = createSampleData();
        actualData.get(0).put("Status", "Inactive");  // Modify
        actualCsv.addRows(actualData);
        actualCsv.save();
        
        // Read and compare
        List<Map<String, String>> expected = new CSVHandler(expectedPath).getAllData();
        List<Map<String, String>> actual = new CSVHandler(actualPath).getAllData();
        
        DataDiff diff = DataDiff.builder()
                .keyField("ID")
                .build();
        
        DiffResult result = diff.compare(expected, actual);
        
        Assert.assertEquals(result.getModifiedRows(), 1);
        
        // Generate report
        DiffReportGenerator generator = new DiffReportGenerator();
        generator.saveReport(result, "CSV File Comparison", 
            "test-output/csv-diff-report.html");
        
        ExtentReportManager.logPass("CSV comparison completed with HTML report");
    }
    
    @Test(description = "Compare Excel files")
    public void testCompareExcelFiles() {
        ExtentReportManager.assignCategory("Diff", "Excel", "Comparison");
        
        // Create two Excel files
        String expectedPath = "test-output/expected_data.xlsx";
        String actualPath = "test-output/actual_data.xlsx";
        
        // Expected Excel
        ExcelHandler expectedExcel = new ExcelHandler(expectedPath, "Users");
        expectedExcel.writeData(createSampleData());
        expectedExcel.save();
        expectedExcel.close();
        
        // Actual Excel (with differences)
        ExcelHandler actualExcel = new ExcelHandler(actualPath, "Users");
        List<Map<String, String>> actualData = createSampleData();
        actualData.get(1).put("Email", "changed@example.com");  // Modify
        actualExcel.writeData(actualData);
        actualExcel.save();
        actualExcel.close();
        
        // Read and compare
        ExcelHandler expReader = new ExcelHandler(expectedPath);
        expReader.selectSheet("Users");
        List<Map<String, String>> expected = expReader.getAllData();
        expReader.close();
        
        ExcelHandler actReader = new ExcelHandler(actualPath);
        actReader.selectSheet("Users");
        List<Map<String, String>> actual = actReader.getAllData();
        actReader.close();
        
        DataDiff diff = DataDiff.builder()
                .keyField("ID")
                .build();
        
        DiffResult result = diff.compare(expected, actual);
        
        Assert.assertEquals(result.getModifiedRows(), 1);
        
        // Generate report
        DiffReportGenerator generator = new DiffReportGenerator();
        generator.saveReport(result, "Excel File Comparison", 
            "test-output/excel-diff-report.html");
        
        ExtentReportManager.logPass("Excel comparison completed with HTML report");
    }
    
    @Test(description = "Compare database query results")
    public void testCompareDatabaseResults() {
        ExtentReportManager.assignCategory("Diff", "Database", "Comparison");
        
        // Simulate database query results
        List<Map<String, String>> expectedResults = createSampleData();
        List<Map<String, String>> actualResults = createSampleData();
        
        // Simulate data change
        actualResults.get(0).put("Status", "Updated");
        
        DataDiff diff = DataDiff.builder()
                .keyField("ID")
                .build();
        
        DiffResult result = diff.compare(expectedResults, actualResults);
        
        Assert.assertFalse(result.isIdentical());
        Assert.assertEquals(result.getModifiedRows(), 1);
        
        // Generate detailed report
        DiffReportGenerator generator = new DiffReportGenerator();
        generator.saveReport(result, "Database Results Comparison", 
            "test-output/db-diff-report.html");
        
        ExtentReportManager.logPass("Database comparison: " + result.getSummaryString());
    }
    
    @Test(description = "Compare API responses")
    public void testCompareAPIResponses() {
        ExtentReportManager.assignCategory("Diff", "API", "Comparison");
        
        // Simulate API responses
        List<Map<String, String>> expectedResponse = createSampleData();
        List<Map<String, String>> actualResponse = createSampleData();
        
        // Remove one record (simulating API returning different data)
        actualResponse.remove(2);
        
        DataDiff diff = DataDiff.builder()
                .keyField("ID")
                .build();
        
        DiffResult result = diff.compare(expectedResponse, actualResponse);
        
        Assert.assertEquals(result.getDeletedRows(), 1);
        Assert.assertEquals(result.getMatchPercentage(), 66.6, 0.1);
        
        ExtentReportManager.logPass("API response comparison: " + result.getSummaryString());
    }
    
    /**
     * Helper method to create sample data
     */
    private List<Map<String, String>> createSampleData() {
        List<Map<String, String>> data = new ArrayList<>();
        
        Map<String, String> user1 = new HashMap<>();
        user1.put("ID", "1");
        user1.put("Name", "John Doe");
        user1.put("Email", "john@example.com");
        user1.put("Status", "Active");
        data.add(user1);
        
        Map<String, String> user2 = new HashMap<>();
        user2.put("ID", "2");
        user2.put("Name", "Jane Smith");
        user2.put("Email", "jane@example.com");
        user2.put("Status", "Active");
        data.add(user2);
        
        Map<String, String> user3 = new HashMap<>();
        user3.put("ID", "3");
        user3.put("Name", "Bob Johnson");
        user3.put("Email", "bob@example.com");
        user3.put("Status", "Inactive");
        data.add(user3);
        
        return data;
    }
}
