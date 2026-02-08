# Data Handling Quick Reference

## Quick Start Examples

### Excel - Read Data
```java
ExcelHandler excel = new ExcelHandler("testdata.xlsx");
excel.selectSheet("Users");
List<Map<String, String>> data = excel.getAllData();
excel.close();
```

### Excel - Write Data
```java
ExcelHandler excel = new ExcelHandler("output.xlsx", "Results");
Map<String, String> row = Map.of("Name", "John", "Age", "30");
excel.writeData(List.of(row));
excel.save();
excel.close();
```

### CSV - Read Data
```java
CSVHandler csv = new CSVHandler("testdata.csv");
List<Map<String, String>> data = csv.getAllData();
```

### CSV - Write Data
```java
List<String> headers = List.of("Name", "Email");
CSVHandler csv = new CSVHandler("output.csv", headers);
Map<String, String> row = Map.of("Name", "John", "Email", "john@example.com");
csv.addRow(row);
csv.save();
```

### Data-Driven Test
```java
@Test(dataProvider = "csvDataProvider", dataProviderClass = TestDataProvider.class)
public void test(Map<String, String> data) {
    String username = data.get("Username");
    String password = data.get("Password");
    loginPage.login(username, password);
}
```

## Common Operations

| Operation | Excel | CSV |
|-----------|-------|-----|
| Read all data | `excel.getAllData()` | `csv.getAllData()` |
| Get cell value | `excel.getCellValue(row, col)` | `csv.getCellValue(row, col)` |
| Set cell value | `excel.setCellValue(row, col, val)` | `csv.setCellValue(row, col, val)` |
| Add row | `excel.writeData(List.of(row))` | `csv.addRow(row)` |
| Filter data | `excel.getTestDataWhere(col, val)` | `csv.filterRows(col, val)` |
| Get row count | `excel.getRowCount()` | `csv.getRowCount()` |
| Save file | `excel.save()` | `csv.save()` |

## Test Data File Locations

Place test data files in:
- `src/test/resources/testdata/`

Example structure:
```
testdata/
├── login_testdata.csv
├── user_api_testdata.csv
├── product_data.xlsx
└── test_scenarios.xlsx
```

## Data Provider Names

Built-in data providers:
- `excelDataProvider` - Auto-detects Excel file based on test method name
- `csvDataProvider` - Auto-detects CSV file based on test method name

Custom data providers:
```java
@DataProvider(name = "myData")
public Object[][] getData() {
    return TestDataProvider.getExcelData("file.xlsx", "Sheet1");
}
```

## Quick Tips

1. **Always close handlers:**
   ```java
   excel.close(); // or use try-with-resources if implemented
   ```

2. **Make emails unique in API tests:**
   ```java
   String email = System.currentTimeMillis() + "_" + data.get("Email");
   ```

3. **Filter test data:**
   ```java
   // Only get active test cases
   List<Map<String, String>> active = csv.filterRows("Status", "Active");
   ```

4. **Export results:**
   ```java
   ExcelHandler results = new ExcelHandler("results.xlsx", "TestResults");
   results.writeData(resultsList);
   results.save();
   ```

## File Format Examples

### CSV Format
```csv
TestCase,Username,Password,ExpectedResult
TC_001,user@example.com,Pass123,Success
TC_002,invalid@example.com,Wrong,Failure
```

### Excel Format
| TestCase | Username | Password | ExpectedResult |
|----------|----------|----------|----------------|
| TC_001 | user@example.com | Pass123 | Success |
| TC_002 | invalid@example.com | Wrong | Failure |

## Complete Example

```java
public class MyDataDrivenTest extends BaseTest {
    
    @Test(dataProvider = "csvDataProvider", 
          dataProviderClass = TestDataProvider.class)
    public void testLogin(Map<String, String> testData) {
        // Extract data
        String username = testData.get("Username");
        String password = testData.get("Password");
        String expected = testData.get("ExpectedResult");
        
        // Log
        ExtentReportManager.logInfo("Testing: " + testData.get("TestCase"));
        
        // Execute
        loginPage.login(username, password);
        
        // Verify
        if ("Success".equals(expected)) {
            Assert.assertTrue(homePage.isLoaded());
        } else {
            Assert.assertTrue(loginPage.isErrorDisplayed());
        }
    }
}
```

For detailed documentation, see [DATA_HANDLING_GUIDE.md](DATA_HANDLING_GUIDE.md)
