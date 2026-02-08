package com.automation.tests.api;

import com.automation.app.api.UserApiClient;
import com.automation.core.data.CSVHandler;
import com.automation.core.data.TestDataProvider;
import com.automation.core.reporting.ExtentReportManager;
import com.automation.core.utils.BaseTest;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Data-driven API tests using CSV test data
 */
public class DataDrivenUserApiTest extends BaseTest {
    
    private UserApiClient userApi;
    private List<Integer> createdUserIds;
    
    @BeforeClass
    public void setupClass() {
        userApi = new UserApiClient();
        createdUserIds = new ArrayList<>();
    }
    
    @Test(dataProvider = "csvDataProvider", dataProviderClass = TestDataProvider.class,
          description = "Data-driven user creation test from CSV")
    public void testCreateUserWithCSVData(Map<String, String> testData) {
        ExtentReportManager.assignCategory("API", "User", "Data-Driven");
        
        String testCase = testData.get("TestCase");
        String name = testData.get("Name");
        String email = testData.get("Email");
        String role = testData.get("Role");
        int expectedStatus = Integer.parseInt(testData.get("ExpectedStatus"));
        String description = testData.get("Description");
        
        ExtentReportManager.logInfo("Test Case: " + testCase);
        ExtentReportManager.logInfo("Description: " + description);
        
        // Make unique email for each run
        String uniqueEmail = System.currentTimeMillis() + "_" + email;
        
        // Send API request
        Response response = userApi.createUser(name, uniqueEmail, role);
        
        // Verify status code
        Assert.assertEquals(response.getStatusCode(), expectedStatus, 
            "Status code mismatch for: " + description);
        
        // Store user ID for cleanup if created successfully
        if (response.getStatusCode() == 201) {
            int userId = response.jsonPath().getInt("id");
            createdUserIds.add(userId);
            ExtentReportManager.logPass("User created successfully: " + userId);
        } else {
            ExtentReportManager.logPass("Request failed as expected with status: " + response.getStatusCode());
        }
    }
    
    @Test(description = "Test using filtered CSV data")
    public void testValidUserCreation() {
        ExtentReportManager.assignCategory("API", "User", "Filtered-Data");
        
        String dataFile = "src/test/resources/testdata/user_api_testdata.csv";
        
        // Get only test cases with expected status 201
        Object[][] testData = TestDataProvider.getCSVDataWhere(dataFile, "ExpectedStatus", "201");
        
        ExtentReportManager.logInfo("Testing " + testData.length + " valid user creation scenarios");
        
        for (Object[] data : testData) {
            @SuppressWarnings("unchecked")
            Map<String, String> row = (Map<String, String>) data[0];
            
            String email = System.currentTimeMillis() + "_" + row.get("Email");
            Response response = userApi.createUser(row.get("Name"), email, row.get("Role"));
            
            Assert.assertEquals(response.getStatusCode(), 201, 
                "User creation should succeed");
            
            if (response.getStatusCode() == 201) {
                createdUserIds.add(response.jsonPath().getInt("id"));
            }
        }
        
        ExtentReportManager.logPass("All valid user creation tests passed");
    }
    
    @Test(description = "Example of reading and updating CSV data")
    public void testCSVDataManipulation() {
        ExtentReportManager.assignCategory("Data", "CSV", "Manipulation");
        
        String dataFile = "src/test/resources/testdata/user_api_testdata.csv";
        CSVHandler csv = new CSVHandler(dataFile);
        
        // Read all data
        List<Map<String, String>> allData = csv.getAllData();
        ExtentReportManager.logInfo("Total test cases in CSV: " + allData.size());
        
        // Get specific row
        Map<String, String> firstRow = csv.getRow(0);
        ExtentReportManager.logInfo("First test case: " + firstRow.get("TestCase"));
        
        // Filter data
        List<Map<String, String>> adminUsers = csv.filterRows("Role", "admin");
        ExtentReportManager.logInfo("Admin user test cases: " + adminUsers.size());
        
        // Get column data
        List<String> allEmails = csv.getColumnData("Email");
        ExtentReportManager.logInfo("Emails in test data: " + allEmails.size());
        
        Assert.assertTrue(allData.size() > 0, "CSV should contain data");
        ExtentReportManager.logPass("CSV data manipulation test passed");
    }
    
    @AfterMethod
    public void cleanup() {
        // Clean up created users
        for (Integer userId : createdUserIds) {
            try {
                userApi.deleteUser(userId);
            } catch (Exception e) {
                // Log but don't fail test
                ExtentReportManager.logWarning("Could not delete user: " + userId);
            }
        }
        createdUserIds.clear();
    }
}
