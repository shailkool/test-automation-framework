package com.automation.tests.data;

import com.automation.core.data.ExcelHandler;
import com.automation.core.reporting.ExtentReportManager;
import com.automation.core.utils.BaseTest;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.*;

/**
 * Examples demonstrating Excel data handling capabilities
 */
public class ExcelDataHandlingTest extends BaseTest {
    
    @Test(description = "Create new Excel file and write data")
    public void testCreateExcelFile() {
        ExtentReportManager.assignCategory("Data", "Excel", "Write");
        
        String filePath = "test-output/sample_users.xlsx";
        
        // Create new Excel file
        ExcelHandler excel = new ExcelHandler(filePath, "Users");
        
        // Prepare test data
        List<Map<String, String>> users = new ArrayList<>();
        
        Map<String, String> user1 = new HashMap<>();
        user1.put("UserID", "1");
        user1.put("Name", "John Doe");
        user1.put("Email", "john.doe@example.com");
        user1.put("Role", "Admin");
        users.add(user1);
        
        Map<String, String> user2 = new HashMap<>();
        user2.put("UserID", "2");
        user2.put("Name", "Jane Smith");
        user2.put("Email", "jane.smith@example.com");
        user2.put("Role", "User");
        users.add(user2);
        
        Map<String, String> user3 = new HashMap<>();
        user3.put("UserID", "3");
        user3.put("Name", "Bob Johnson");
        user3.put("Email", "bob.johnson@example.com");
        user3.put("Role", "User");
        users.add(user3);
        
        // Write data to Excel
        excel.writeData(users);
        excel.autoSizeColumns();
        excel.save();
        excel.close();
        
        ExtentReportManager.logPass("Excel file created with " + users.size() + " users");
        
        // Verify file was created
        Assert.assertTrue(new java.io.File(filePath).exists(), "Excel file should be created");
    }
    
    @Test(description = "Read data from Excel file", dependsOnMethods = "testCreateExcelFile")
    public void testReadExcelFile() {
        ExtentReportManager.assignCategory("Data", "Excel", "Read");
        
        String filePath = "test-output/sample_users.xlsx";
        
        ExcelHandler excel = new ExcelHandler(filePath);
        excel.selectSheet("Users");
        
        // Read all data
        List<Map<String, String>> allData = excel.getAllData();
        ExtentReportManager.logInfo("Total rows read: " + allData.size());
        
        Assert.assertEquals(allData.size(), 3, "Should have 3 user records");
        
        // Read specific cell
        String firstUserName = excel.getCellValue(1, "Name");
        ExtentReportManager.logInfo("First user name: " + firstUserName);
        Assert.assertEquals(firstUserName, "John Doe", "First user name should match");
        
        // Read specific row
        Map<String, String> secondUser = excel.getRowData(2);
        ExtentReportManager.logInfo("Second user email: " + secondUser.get("Email"));
        Assert.assertEquals(secondUser.get("Email"), "jane.smith@example.com", 
            "Second user email should match");
        
        // Get row and column count
        int rowCount = excel.getRowCount();
        int colCount = excel.getColumnCount();
        ExtentReportManager.logInfo("Rows: " + rowCount + ", Columns: " + colCount);
        
        excel.close();
        
        ExtentReportManager.logPass("Excel file read successfully");
    }
    
    @Test(description = "Update Excel file data", dependsOnMethods = "testReadExcelFile")
    public void testUpdateExcelFile() {
        ExtentReportManager.assignCategory("Data", "Excel", "Update");
        
        String filePath = "test-output/sample_users.xlsx";
        
        ExcelHandler excel = new ExcelHandler(filePath);
        excel.selectSheet("Users");
        
        // Update a cell value
        excel.setCellValue(1, "Role", "SuperAdmin");
        
        // Add a new row
        Map<String, String> newUser = new HashMap<>();
        newUser.put("UserID", "4");
        newUser.put("Name", "Alice Williams");
        newUser.put("Email", "alice.williams@example.com");
        newUser.put("Role", "Manager");
        
        List<Map<String, String>> newUsers = Arrays.asList(newUser);
        excel.writeData(newUsers);
        
        excel.save();
        
        // Verify update
        String updatedRole = excel.getCellValue(1, "Role");
        Assert.assertEquals(updatedRole, "SuperAdmin", "Role should be updated");
        
        int rowCount = excel.getRowCount();
        Assert.assertEquals(rowCount, 4, "Should have 4 users after adding one");
        
        excel.close();
        
        ExtentReportManager.logPass("Excel file updated successfully");
    }
    
    @Test(description = "Work with multiple sheets in Excel")
    public void testMultipleSheets() {
        ExtentReportManager.assignCategory("Data", "Excel", "Multi-Sheet");
        
        String filePath = "test-output/multi_sheet_data.xlsx";
        
        // Create Excel with first sheet
        ExcelHandler excel = new ExcelHandler(filePath, "Employees");
        
        // Add data to first sheet
        List<Map<String, String>> employees = new ArrayList<>();
        Map<String, String> emp1 = new HashMap<>();
        emp1.put("EmpID", "E001");
        emp1.put("Name", "Employee One");
        emp1.put("Department", "IT");
        employees.add(emp1);
        
        excel.writeData(employees);
        
        // Create second sheet
        excel.createSheet("Departments");
        
        List<Map<String, String>> departments = new ArrayList<>();
        Map<String, String> dept1 = new HashMap<>();
        dept1.put("DeptID", "D001");
        dept1.put("DeptName", "IT");
        dept1.put("Location", "Building A");
        departments.add(dept1);
        
        excel.writeData(departments);
        
        excel.save();
        
        // Verify sheets
        List<String> sheetNames = excel.getSheetNames();
        ExtentReportManager.logInfo("Sheets in workbook: " + sheetNames);
        
        Assert.assertTrue(sheetNames.contains("Employees"), "Should have Employees sheet");
        Assert.assertTrue(sheetNames.contains("Departments"), "Should have Departments sheet");
        
        excel.close();
        
        ExtentReportManager.logPass("Multiple sheets handled successfully");
    }
    
    @Test(description = "Filter and search Excel data", dependsOnMethods = "testUpdateExcelFile")
    public void testFilterExcelData() {
        ExtentReportManager.assignCategory("Data", "Excel", "Filter");
        
        String filePath = "test-output/sample_users.xlsx";
        
        ExcelHandler excel = new ExcelHandler(filePath);
        excel.selectSheet("Users");
        
        // Get users with specific role
        List<Map<String, String>> regularUsers = excel.getTestDataWhere("Role", "User");
        ExtentReportManager.logInfo("Users with 'User' role: " + regularUsers.size());
        
        Assert.assertEquals(regularUsers.size(), 2, "Should have 2 regular users");
        
        // Get specific test data
        Map<String, String> adminUser = excel.getTestDataWhere("Role", "SuperAdmin").get(0);
        ExtentReportManager.logInfo("Admin user: " + adminUser.get("Name"));
        
        excel.close();
        
        ExtentReportManager.logPass("Excel data filtering successful");
    }
    
    @Test(description = "Export test results to Excel")
    public void testExportResultsToExcel() {
        ExtentReportManager.assignCategory("Data", "Excel", "Export");
        
        String filePath = "test-output/test_results.xlsx";
        
        ExcelHandler excel = new ExcelHandler(filePath, "TestResults");
        
        // Simulate test results
        List<Map<String, String>> results = new ArrayList<>();
        
        Map<String, String> result1 = new HashMap<>();
        result1.put("TestCase", "TC_001");
        result1.put("TestName", "Login Test");
        result1.put("Status", "Passed");
        result1.put("Duration", "2.5s");
        results.add(result1);
        
        Map<String, String> result2 = new HashMap<>();
        result2.put("TestCase", "TC_002");
        result2.put("TestName", "API Test");
        result2.put("Status", "Passed");
        result2.put("Duration", "1.2s");
        results.add(result2);
        
        Map<String, String> result3 = new HashMap<>();
        result3.put("TestCase", "TC_003");
        result3.put("TestName", "Database Test");
        result3.put("Status", "Failed");
        result3.put("Duration", "3.1s");
        results.add(result3);
        
        excel.writeData(results);
        excel.autoSizeColumns();
        excel.save();
        excel.close();
        
        ExtentReportManager.logPass("Test results exported to Excel successfully");
    }
}
