package com.smbc.raft.tests.diff;

import com.smbc.raft.core.data.CSVHandler;
import com.smbc.raft.core.diff.DataDiff;
import com.smbc.raft.core.diff.DiffHtmlReportGenerator;
import com.smbc.raft.core.diff.DiffResult;
import com.smbc.raft.core.diff.DiffSummary;
import com.smbc.raft.core.reporting.ExtentReportManager;
import com.smbc.raft.core.utils.BaseTest;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Examples demonstrating data diff and HTML report generation
 */
public class DataDiffTest extends BaseTest {
    
    @Test(description = "Compare two data sets and identify differences")
    public void testBasicDataDiff() {
        ExtentReportManager.assignCategory("DataDiff", "Basic", "Comparison");
        
        // Create expected data
        List<Map<String, String>> expected = new ArrayList<>();
        expected.add(createRow("1", "John Doe", "john@example.com", "Active"));
        expected.add(createRow("2", "Jane Smith", "jane@example.com", "Active"));
        expected.add(createRow("3", "Bob Johnson", "bob@example.com", "Inactive"));
        
        // Create actual data (with differences)
        List<Map<String, String>> actual = new ArrayList<>();
        actual.add(createRow("1", "John Doe", "john@example.com", "Active"));  // Unchanged
        actual.add(createRow("2", "Jane Smith", "jane.smith@example.com", "Active"));  // Modified email
        actual.add(createRow("4", "Alice Williams", "alice@example.com", "Active"));  // Added
        // Bob Johnson (ID 3) is deleted
        
        // Perform diff
        DataDiff diff = new DataDiff(expected, actual, "ID");
        
        // Print summary
        diff.printSummary();
        
        // Verify differences
        Assert.assertTrue(diff.isHasDifferences(), "Should have differences");
        Assert.assertEquals(diff.getAddedRows().size(), 1, "Should have 1 added row");
        Assert.assertEquals(diff.getDeletedRows().size(), 1, "Should have 1 deleted row");
        Assert.assertEquals(diff.getModifiedRows().size(), 1, "Should have 1 modified row");
        Assert.assertEquals(diff.getUnchangedRows().size(), 1, "Should have 1 unchanged row");
        
        ExtentReportManager.logPass("Data diff completed successfully");
        ExtentReportManager.logInfo("Total differences: " + diff.getTotalDifferences());
    }
    
    @Test(description = "Generate HTML diff report")
    public void testGenerateHtmlReport() {
        ExtentReportManager.assignCategory("DataDiff", "Report", "HTML");
        
        // Create test data with differences
        List<Map<String, String>> expected = new ArrayList<>();
        expected.add(createProductRow("P001", "Laptop", "999.99", "50"));
        expected.add(createProductRow("P002", "Mouse", "25.50", "200"));
        expected.add(createProductRow("P003", "Keyboard", "75.00", "150"));
        expected.add(createProductRow("P004", "Monitor", "299.99", "75"));
        
        List<Map<String, String>> actual = new ArrayList<>();
        actual.add(createProductRow("P001", "Laptop", "899.99", "45"));  // Price and stock changed
        actual.add(createProductRow("P002", "Mouse", "25.50", "200"));  // Unchanged
        actual.add(createProductRow("P005", "Headphones", "99.99", "100"));  // Added
        // P003 Keyboard deleted
        // P004 Monitor deleted
        
        // Perform diff
        DataDiff diff = new DataDiff(expected, actual, "ProductID");
        
        // Generate HTML report
        String reportPath = "test-output/data-diff-report.html";
        DiffHtmlReportGenerator.generateReport(
            diff, 
            reportPath,
            "Expected Products",
            "Actual Products"
        );
        
        // Verify report was created
        Assert.assertTrue(new java.io.File(reportPath).exists(), 
            "HTML report should be created");
        
        ExtentReportManager.logPass("HTML diff report generated: " + reportPath);
        
        // Log summary
        DiffSummary summary = diff.getSummary();
        ExtentReportManager.logInfo(String.format(
            "Summary: %d added, %d deleted, %d modified, %d unchanged",
            summary.getAddedCount(),
            summary.getDeletedCount(),
            summary.getModifiedCount(),
            summary.getUnchangedCount()
        ));
    }
    
    @Test(description = "Compare CSV files and generate diff report")
    public void testCsvFileDiff() {
        ExtentReportManager.assignCategory("DataDiff", "CSV", "FileComparison");
        
        // Create expected CSV
        String expectedCsv = "test-output/expected_users.csv";
        createTestCsv(expectedCsv, Arrays.asList(
            createRow("1", "User One", "user1@example.com", "admin"),
            createRow("2", "User Two", "user2@example.com", "user"),
            createRow("3", "User Three", "user3@example.com", "user")
        ));
        
        // Create actual CSV
        String actualCsv = "test-output/actual_users.csv";
        createTestCsv(actualCsv, Arrays.asList(
            createRow("1", "User One", "user1@example.com", "admin"),  // Unchanged
            createRow("2", "User Two", "user2.new@example.com", "admin"),  // Email and role changed
            createRow("4", "User Four", "user4@example.com", "user")  // Added
            // User Three deleted
        ));
        
        // Read CSV files
        CSVHandler expectedHandler = new CSVHandler(expectedCsv);
        CSVHandler actualHandler = new CSVHandler(actualCsv);
        
        List<Map<String, String>> expectedData = expectedHandler.getAllData();
        List<Map<String, String>> actualData = actualHandler.getAllData();
        
        // Perform diff
        DataDiff diff = new DataDiff(expectedData, actualData, "ID");
        
        // Generate report
        String reportPath = "test-output/csv-diff-report.html";
        DiffHtmlReportGenerator.generateReport(
            diff,
            reportPath,
            "Expected CSV: " + expectedCsv,
            "Actual CSV: " + actualCsv
        );
        
        Assert.assertTrue(new java.io.File(reportPath).exists());
        Assert.assertTrue(diff.isHasDifferences());
        
        ExtentReportManager.logPass("CSV diff report generated: " + reportPath);
    }
    
    @Test(description = "Compare database results with expected data")
    public void testDatabaseResultDiff() {
        ExtentReportManager.assignCategory("DataDiff", "Database", "ResultComparison");
        
        // Expected data
        List<Map<String, String>> expected = new ArrayList<>();
        expected.add(createOrderRow("ORD001", "COMPLETED", "500.00"));
        expected.add(createOrderRow("ORD002", "PENDING", "250.00"));
        expected.add(createOrderRow("ORD003", "SHIPPED", "750.00"));
        
        // Simulate database results (actual data)
        List<Map<String, String>> actual = new ArrayList<>();
        actual.add(createOrderRow("ORD001", "COMPLETED", "500.00"));  // Unchanged
        actual.add(createOrderRow("ORD002", "COMPLETED", "250.00"));  // Status changed to COMPLETED
        actual.add(createOrderRow("ORD003", "DELIVERED", "750.00"));  // Status changed to DELIVERED
        actual.add(createOrderRow("ORD004", "PENDING", "100.00"));  // New order
        
        // Perform diff
        DataDiff diff = new DataDiff(expected, actual, "OrderID");
        
        // Generate report
        String reportPath = "test-output/database-diff-report.html";
        DiffHtmlReportGenerator.generateReport(
            diff,
            reportPath,
            "Expected Order Status",
            "Actual Database Results"
        );
        
        // Verify differences
        Assert.assertEquals(diff.getModifiedRows().size(), 2, 
            "Two orders should have status changes");
        Assert.assertEquals(diff.getAddedRows().size(), 1, 
            "One new order should be added");
        
        ExtentReportManager.logPass("Database diff report generated");
    }
    
    @Test(description = "Compare API responses")
    public void testApiResponseDiff() {
        ExtentReportManager.assignCategory("DataDiff", "API", "ResponseComparison");
        
        // Expected API response
        List<Map<String, String>> expectedResponse = new ArrayList<>();
        expectedResponse.add(createApiResponse("1", "Success", "User created"));
        expectedResponse.add(createApiResponse("2", "Success", "User updated"));
        
        // Actual API response
        List<Map<String, String>> actualResponse = new ArrayList<>();
        actualResponse.add(createApiResponse("1", "Success", "User created"));  // Same
        actualResponse.add(createApiResponse("2", "Error", "User not found"));  // Different status and message
        actualResponse.add(createApiResponse("3", "Success", "User deleted"));  // New response
        
        // Perform diff
        DataDiff diff = new DataDiff(expectedResponse, actualResponse, "RequestID");
        
        // Generate report
        String reportPath = "test-output/api-diff-report.html";
        DiffHtmlReportGenerator.generateReport(
            diff,
            reportPath,
            "Expected API Response",
            "Actual API Response"
        );
        
        Assert.assertTrue(diff.isHasDifferences());
        ExtentReportManager.logPass("API response diff completed");
    }
    
    @Test(description = "Test with composite key (multiple key fields)")
    public void testCompositeKeyDiff() {
        ExtentReportManager.assignCategory("DataDiff", "CompositeKey", "MultiField");
        
        // Data with composite key (FirstName + LastName)
        List<Map<String, String>> expected = new ArrayList<>();
        expected.add(createPersonRow("John", "Doe", "30", "USA"));
        expected.add(createPersonRow("Jane", "Smith", "25", "UK"));
        
        List<Map<String, String>> actual = new ArrayList<>();
        actual.add(createPersonRow("John", "Doe", "31", "USA"));  // Age changed
        actual.add(createPersonRow("Jane", "Smith", "25", "Canada"));  // Country changed
        
        // Diff with composite key
        DataDiff diff = new DataDiff(
            expected, 
            actual, 
            Arrays.asList("FirstName", "LastName")  // Composite key
        );
        
        Assert.assertEquals(diff.getModifiedRows().size(), 2);
        ExtentReportManager.logPass("Composite key diff completed");
    }

    @Test(description = "Builder-style composite key with ignoreCase and ignoreField")
    public void testCompositeKeyViaBuilder() {
        ExtentReportManager.assignCategory("DataDiff", "CompositeKey", "Builder");

        List<Map<String, String>> expected = new ArrayList<>();
        expected.add(createPersonRow("John",  "Doe",   "30", "USA"));
        expected.add(createPersonRow("Jane",  "Smith", "25", "UK"));
        expected.add(createPersonRow("Alice", "Jones", "40", "IE"));

        List<Map<String, String>> actual = new ArrayList<>();
        // same composite-key person but mixed case in second key part - ignoreCase should match
        actual.add(createPersonRow("John", "DOE",   "30", "USA"));
        // same person with different Country - should be MODIFIED...
        actual.add(createPersonRow("Jane", "Smith", "25", "Canada"));
        // ...unless Country is in ignoreFields, in which case should be UNCHANGED
        actual.add(createPersonRow("Bob",  "Brown", "55", "AU"));
        // Alice missing -> DELETED, Bob new -> ADDED

        DiffResult result = DataDiff.builder()
            .keyFields("FirstName", "LastName")
            .ignoreCase(true)
            .ignoreField("Country")
            .build()
            .compare(expected, actual);

        Assert.assertTrue(result.isIdentical() == false, "Composite diff should report differences");
        Assert.assertEquals(result.getMatchedRows(),  2, "John and Jane should be unchanged (case + ignored field)");
        Assert.assertEquals(result.getAddedRows(),    1, "Bob should be ADDED");
        Assert.assertEquals(result.getDeletedRows(),  1, "Alice should be DELETED");
        Assert.assertEquals(result.getModifiedRows(), 0, "Nothing modified once ignoreCase + ignoreField applied");

        ExtentReportManager.logPass("Composite-key builder diff completed: " + result.getSummaryString());
    }

    @Test(description = "Test with no differences")
    public void testNoDifferences() {
        ExtentReportManager.assignCategory("DataDiff", "NoDiff", "Identical");
        
        List<Map<String, String>> data1 = new ArrayList<>();
        data1.add(createRow("1", "A", "B", "C"));
        data1.add(createRow("2", "D", "E", "F"));
        
        List<Map<String, String>> data2 = new ArrayList<>();
        data2.add(createRow("1", "A", "B", "C"));
        data2.add(createRow("2", "D", "E", "F"));
        
        DataDiff diff = new DataDiff(data1, data2, "ID");
        
        Assert.assertFalse(diff.isHasDifferences(), "Should have no differences");
        Assert.assertEquals(diff.getUnchangedRows().size(), 2);
        Assert.assertEquals(diff.getTotalDifferences(), 0);
        
        ExtentReportManager.logPass("Verified no differences detected");
    }
    
    // Helper methods
    private Map<String, String> createRow(String id, String name, String email, String status) {
        Map<String, String> row = new LinkedHashMap<>();
        row.put("ID", id);
        row.put("Name", name);
        row.put("Email", email);
        row.put("Status", status);
        return row;
    }
    
    private Map<String, String> createProductRow(String id, String name, String price, String stock) {
        Map<String, String> row = new LinkedHashMap<>();
        row.put("ProductID", id);
        row.put("ProductName", name);
        row.put("Price", price);
        row.put("Stock", stock);
        return row;
    }
    
    private Map<String, String> createOrderRow(String orderId, String status, String amount) {
        Map<String, String> row = new LinkedHashMap<>();
        row.put("OrderID", orderId);
        row.put("Status", status);
        row.put("Amount", amount);
        return row;
    }
    
    private Map<String, String> createApiResponse(String requestId, String status, String message) {
        Map<String, String> row = new LinkedHashMap<>();
        row.put("RequestID", requestId);
        row.put("Status", status);
        row.put("Message", message);
        return row;
    }
    
    private Map<String, String> createPersonRow(String firstName, String lastName, String age, String country) {
        Map<String, String> row = new LinkedHashMap<>();
        row.put("FirstName", firstName);
        row.put("LastName", lastName);
        row.put("Age", age);
        row.put("Country", country);
        return row;
    }
    
    private void createTestCsv(String filePath, List<Map<String, String>> data) {
        if (data.isEmpty()) return;
        
        List<String> headers = new ArrayList<>(data.get(0).keySet());
        CSVHandler csv = new CSVHandler(filePath, headers);
        csv.addRows(data);
        csv.save();
    }
}
