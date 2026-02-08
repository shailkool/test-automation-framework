# Data Handling Guide

## Overview

The framework provides comprehensive Excel and CSV handling capabilities for data-driven testing, test data management, and result reporting.

## Features

### Excel Handling (ExcelHandler)
- Read and write .xlsx files
- Multiple sheet support
- Cell-level operations
- Data filtering and searching
- Auto-sizing columns
- Test data management

### CSV Handling (CSVHandler)
- Read and write CSV files
- Row and column operations
- Data filtering and searching
- Sorting capabilities
- Test data management

### TestNG Data Providers
- Automated data-driven testing
- Excel data provider
- CSV data provider
- Filtered data providers

## Excel Operations

### Creating a New Excel File

```java
// Create new Excel file with a sheet
ExcelHandler excel = new ExcelHandler("path/to/file.xlsx", "SheetName");

// Prepare data
List<Map<String, String>> data = new ArrayList<>();
Map<String, String> row1 = new HashMap<>();
row1.put("Name", "John Doe");
row1.put("Email", "john@example.com");
data.add(row1);

// Write data
excel.writeData(data);
excel.autoSizeColumns();
excel.save();
excel.close();
```

### Reading Excel Files

```java
// Load existing Excel file
ExcelHandler excel = new ExcelHandler("path/to/file.xlsx");
excel.selectSheet("SheetName");

// Read all data
List<Map<String, String>> allData = excel.getAllData();

// Read specific cell
String value = excel.getCellValue(rowNum, "ColumnName");

// Read specific row
Map<String, String> rowData = excel.getRowData(rowNum);

excel.close();
```

### Updating Excel Files

```java
ExcelHandler excel = new ExcelHandler("path/to/file.xlsx");
excel.selectSheet("SheetName");

// Update single cell
excel.setCellValue(rowNum, "ColumnName", "New Value");

// Update entire row
Map<String, String> updatedRow = new HashMap<>();
updatedRow.put("Name", "Updated Name");
updatedRow.put("Email", "updated@example.com");
excel.writeRow(rowNum, updatedRow);

excel.save();
excel.close();
```

### Working with Multiple Sheets

```java
ExcelHandler excel = new ExcelHandler("path/to/file.xlsx");

// Create new sheet
excel.createSheet("NewSheet");

// Switch between sheets
excel.selectSheet("Sheet1");
// ... perform operations

excel.selectSheet("Sheet2");
// ... perform operations

// Get all sheet names
List<String> sheets = excel.getSheetNames();

excel.close();
```

### Filtering Excel Data

```java
ExcelHandler excel = new ExcelHandler("path/to/file.xlsx");
excel.selectSheet("TestData");

// Get all rows where column equals value
List<Map<String, String>> filtered = excel.getTestDataWhere("Status", "Active");

// Get specific test case
Map<String, String> testCase = excel.getTestData("TC_001");

excel.close();
```

## CSV Operations

### Creating a New CSV File

```java
// Define headers
List<String> headers = Arrays.asList("ID", "Name", "Email", "Status");

// Create new CSV
CSVHandler csv = new CSVHandler("path/to/file.csv", headers);

// Add rows
Map<String, String> row1 = new HashMap<>();
row1.put("ID", "1");
row1.put("Name", "John Doe");
row1.put("Email", "john@example.com");
row1.put("Status", "Active");

csv.addRow(row1);
csv.save();
```

### Reading CSV Files

```java
// Load existing CSV
CSVHandler csv = new CSVHandler("path/to/file.csv");

// Read all data
List<Map<String, String>> allData = csv.getAllData();

// Read specific cell
String value = csv.getCellValue(rowIndex, "ColumnName");

// Read specific row
Map<String, String> row = csv.getRow(rowIndex);

// Get all values from a column
List<String> columnData = csv.getColumnData("ColumnName");

// Get headers
List<String> headers = csv.getHeaders();
```

### Updating CSV Files

```java
CSVHandler csv = new CSVHandler("path/to/file.csv");

// Update single cell
csv.setCellValue(rowIndex, "ColumnName", "New Value");

// Update entire row
Map<String, String> updatedRow = new HashMap<>();
updatedRow.put("Name", "Updated Name");
updatedRow.put("Status", "Inactive");
csv.updateRow(rowIndex, updatedRow);

// Add new row
Map<String, String> newRow = new HashMap<>();
newRow.put("ID", "10");
newRow.put("Name", "New User");
csv.addRow(newRow);

csv.save();
```

### Filtering and Searching CSV Data

```java
CSVHandler csv = new CSVHandler("path/to/file.csv");

// Filter rows by exact match
List<Map<String, String>> active = csv.filterRows("Status", "Active");

// Search for text anywhere in rows
List<Map<String, String>> results = csv.searchRows("john");

// Get unique values from a column
Set<String> uniqueStatuses = csv.getUniqueValues("Status");

// Get specific test case
Map<String, String> testCase = csv.getTestData("TC_001");
```

### Sorting CSV Data

```java
CSVHandler csv = new CSVHandler("path/to/file.csv");

// Sort ascending
csv.sortByColumn("Name", true);

// Sort descending
csv.sortByColumn("Date", false);

csv.save();
```

### Deleting CSV Rows

```java
CSVHandler csv = new CSVHandler("path/to/file.csv");

// Delete specific row
csv.deleteRow(rowIndex);

// Clear all data (keeps headers)
csv.clearData();

csv.save();
```

## Data-Driven Testing

### Using Excel Data Provider

```java
@Test(dataProvider = "excelDataProvider", dataProviderClass = TestDataProvider.class)
public void testWithExcelData(Map<String, String> testData) {
    String username = testData.get("Username");
    String password = testData.get("Password");
    String expected = testData.get("ExpectedResult");
    
    // Perform test with data
    loginPage.login(username, password);
    
    if ("Success".equals(expected)) {
        Assert.assertTrue(homePage.isLoaded());
    }
}
```

**Excel File Structure:**
```
| TestCase    | Username              | Password    | ExpectedResult |
|-------------|----------------------|-------------|----------------|
| TC_001      | user@example.com     | Pass123     | Success        |
| TC_002      | invalid@example.com  | Wrong       | Failure        |
```

### Using CSV Data Provider

```java
@Test(dataProvider = "csvDataProvider", dataProviderClass = TestDataProvider.class)
public void testWithCSVData(Map<String, String> testData) {
    String name = testData.get("Name");
    String email = testData.get("Email");
    
    // Perform test with data
    Response response = userApi.createUser(name, email, "user");
    
    Assert.assertEquals(response.getStatusCode(), 201);
}
```

**CSV File Structure:**
```csv
TestCase,Name,Email,ExpectedStatus
TC_001,John Doe,john@example.com,201
TC_002,Jane Smith,jane@example.com,201
```

### Custom Data Providers

```java
// Get data from specific Excel file and sheet
@DataProvider(name = "loginData")
public Object[][] getLoginData() {
    return TestDataProvider.getExcelData(
        "src/test/resources/testdata/login.xlsx", 
        "LoginTests"
    );
}

// Get filtered data
@DataProvider(name = "validUsers")
public Object[][] getValidUsers() {
    return TestDataProvider.getExcelDataWhere(
        "src/test/resources/testdata/users.xlsx",
        "Users",
        "Status",
        "Active"
    );
}

// Use in test
@Test(dataProvider = "loginData")
public void testLogin(Map<String, String> data) {
    // Test implementation
}
```

### Getting Specific Test Case Data

```java
// From Excel
Map<String, String> testData = TestDataProvider.getExcelTestCase(
    "path/to/file.xlsx",
    "SheetName",
    "TC_001"
);

// From CSV
Map<String, String> testData = TestDataProvider.getCSVTestCase(
    "path/to/file.csv",
    "TC_001"
);

// Use the data
String username = testData.get("Username");
String password = testData.get("Password");
```

## Best Practices

### 1. Test Data File Organization

```
src/test/resources/testdata/
├── login_testdata.csv
├── user_api_testdata.csv
├── product_data.xlsx
└── test_scenarios.xlsx
```

### 2. Test Data Structure

**For UI Tests:**
```csv
TestCase,Username,Password,ExpectedResult,Description
TC_LOGIN_001,user@example.com,Pass123,Success,Valid login
TC_LOGIN_002,invalid@example.com,Wrong,Failure,Invalid credentials
```

**For API Tests:**
```csv
TestCase,Endpoint,Method,Payload,ExpectedStatus
TC_API_001,/api/users,POST,{"name":"John"},201
TC_API_002,/api/users/1,GET,,200
```

**For Database Tests:**
```csv
TestCase,Query,ExpectedRows,Description
TC_DB_001,SELECT * FROM users WHERE status='active',5,Active users count
```

### 3. Data-Driven Test Template

```java
@Test(dataProvider = "csvDataProvider", dataProviderClass = TestDataProvider.class)
public void dataTemplateTest(Map<String, String> testData) {
    // 1. Get test data
    String testCase = testData.get("TestCase");
    String description = testData.get("Description");
    
    // 2. Log test info
    ExtentReportManager.logInfo("Test Case: " + testCase);
    ExtentReportManager.logInfo("Description: " + description);
    
    // 3. Perform test steps
    // ... your test logic here
    
    // 4. Verify results
    String expected = testData.get("ExpectedResult");
    // ... assertions
    
    // 5. Log result
    ExtentReportManager.logPass("Test completed");
}
```

### 4. Handling Large Data Sets

```java
// Read data in batches
ExcelHandler excel = new ExcelHandler("large_file.xlsx");
excel.selectSheet("Data");

int batchSize = 100;
int totalRows = excel.getRowCount();

for (int i = 1; i <= totalRows; i += batchSize) {
    List<Map<String, String>> batch = new ArrayList<>();
    for (int j = i; j < i + batchSize && j <= totalRows; j++) {
        batch.add(excel.getRowData(j));
    }
    // Process batch
    processBatch(batch);
}

excel.close();
```

### 5. Dynamic Test Data

```java
// Create unique test data for each test run
Map<String, String> testData = csv.getRow(0);
String uniqueEmail = System.currentTimeMillis() + "_" + testData.get("Email");
testData.put("Email", uniqueEmail);

// Use unique data in test
userApi.createUser(testData.get("Name"), uniqueEmail, testData.get("Role"));
```

### 6. Test Result Reporting

```java
// Export test results to Excel
@AfterClass
public void exportResults() {
    ExcelHandler results = new ExcelHandler("test-output/results.xlsx", "TestResults");
    
    List<Map<String, String>> testResults = new ArrayList<>();
    // ... populate with test results
    
    results.writeData(testResults);
    results.autoSizeColumns();
    results.save();
    results.close();
}

// Export test execution log to CSV
public void logTestExecution(String testName, String status, String duration) {
    CSVHandler log = new CSVHandler("test-output/execution_log.csv");
    
    Map<String, String> entry = new HashMap<>();
    entry.put("Timestamp", new Date().toString());
    entry.put("TestName", testName);
    entry.put("Status", status);
    entry.put("Duration", duration);
    
    log.addRow(entry);
    log.save();
}
```

## Common Use Cases

### 1. Parameter Comparison Testing

```java
// Excel: Browser compatibility test data
| TestCase | Browser  | URL                    | ExpectedTitle |
|----------|----------|------------------------|---------------|
| TC_001   | Chrome   | https://example.com    | Example Site  |
| TC_002   | Firefox  | https://example.com    | Example Site  |
| TC_003   | Safari   | https://example.com    | Example Site  |

@Test(dataProvider = "excelDataProvider", dataProviderClass = TestDataProvider.class)
public void testBrowserCompatibility(Map<String, String> data) {
    String browser = data.get("Browser");
    PlaywrightManager.initializeBrowser(BrowserType.valueOf(browser.toUpperCase()));
    
    page.navigate(data.get("URL"));
    Assert.assertEquals(page.title(), data.get("ExpectedTitle"));
}
```

### 2. Database Verification

```java
// CSV: Database validation test data
TestCase,Table,Column,ExpectedValue
TC_001,users,status,active
TC_002,orders,payment_status,completed

@Test(dataProvider = "csvDataProvider", dataProviderClass = TestDataProvider.class)
public void testDatabaseValues(Map<String, String> data) {
    String query = "SELECT " + data.get("Column") + " FROM " + data.get("Table");
    Object result = dbManager.executeScalar(query);
    
    Assert.assertEquals(result.toString(), data.get("ExpectedValue"));
}
```

### 3. API Contract Testing

```java
// Excel: API endpoint validation
| Endpoint      | Method | ExpectedStatus | ResponseField | ExpectedValue |
|---------------|--------|----------------|---------------|---------------|
| /api/users/1  | GET    | 200            | name          | John Doe      |
| /api/products | POST   | 201            | id            | not_null      |

@Test(dataProvider = "excelDataProvider", dataProviderClass = TestDataProvider.class)
public void testAPIContract(Map<String, String> data) {
    Response response = apiClient.request(
        data.get("Method"),
        data.get("Endpoint")
    );
    
    Assert.assertEquals(response.getStatusCode(), 
        Integer.parseInt(data.get("ExpectedStatus")));
    
    if (!data.get("ExpectedValue").equals("not_null")) {
        Assert.assertEquals(
            response.jsonPath().getString(data.get("ResponseField")),
            data.get("ExpectedValue")
        );
    }
}
```

## Troubleshooting

### Common Issues

1. **File Not Found**
   - Verify file path is correct
   - Use absolute path or correct relative path
   - Check file exists in expected location

2. **Empty Data Returned**
   - Verify sheet name is correct (Excel)
   - Check headers match exactly
   - Ensure file has data rows beyond header

3. **Column Not Found**
   - Column names are case-sensitive
   - Check for extra spaces in column names
   - Verify header row exists

4. **Data Type Mismatch**
   - Excel numeric cells return as strings
   - Parse strings to numbers when needed
   - Use appropriate data types in assertions

### Debug Tips

```java
// Debug Excel structure
ExcelHandler excel = new ExcelHandler("file.xlsx");
System.out.println("Sheets: " + excel.getSheetNames());
excel.selectSheet(0);
System.out.println("Rows: " + excel.getRowCount());
System.out.println("Columns: " + excel.getColumnCount());

// Debug CSV structure
CSVHandler csv = new CSVHandler("file.csv");
System.out.println("Headers: " + csv.getHeaders());
System.out.println("Rows: " + csv.getRowCount());
System.out.println("First row: " + csv.getRow(0));
```

## Performance Tips

1. **Close Files After Use**
   ```java
   ExcelHandler excel = new ExcelHandler("file.xlsx");
   try {
       // Use excel
   } finally {
       excel.close();
   }
   ```

2. **Batch Operations**
   ```java
   // Instead of multiple saves
   excel.setCellValue(1, "A", "value1");
   excel.save(); // Don't save after each operation
   excel.setCellValue(2, "A", "value2");
   excel.save();
   
   // Batch and save once
   excel.setCellValue(1, "A", "value1");
   excel.setCellValue(2, "A", "value2");
   excel.save(); // Save once
   ```

3. **Reuse Handlers**
   ```java
   // In test class
   private static ExcelHandler testData;
   
   @BeforeClass
   public static void loadData() {
       testData = new ExcelHandler("testdata.xlsx");
       testData.selectSheet("Tests");
   }
   
   @AfterClass
   public static void cleanup() {
       testData.close();
   }
   ```

---

For more examples, see:
- `ExcelDataHandlingTest.java`
- `CSVDataHandlingTest.java`
- `DataDrivenLoginTest.java`
- `DataDrivenUserApiTest.java`
