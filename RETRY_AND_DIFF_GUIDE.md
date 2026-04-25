## Retry Validation and Data Diff Guide

## Overview

The framework provides powerful capabilities for:
1. **Retry/Loop Validation** - Validate expected vs actual results with configurable retry mechanisms
2. **Data Diff** - Compare data tables and identify differences
3. **HTML Diff Reports** - Generate beautiful HTML reports showing data differences

## Retry/Loop Validation

### Loop Types

The framework supports three predefined loop types plus custom:

| Loop Type | Iterations | Interval | Total Time | Use Case |
|-----------|------------|----------|------------|----------|
| **ONCE** | 1 | 0ms | 0ms | No retry, immediate check |
| **SHORT** | 15 | 2000ms | 30s | Fast operations, quick validation |
| **LONG** | 60 | 5000ms | 5min | Async operations, batch processing |
| **CUSTOM** | Configurable | Configurable | Variable | Specific requirements |

### Basic Usage

```java
import com.smbc.raft.core.retry.*;

// Validate with SHORT loop (15 attempts, 2s interval)
ValidationResult<Integer> result = RetryValidator.validateWithRetry(
    200,  // Expected value
    () -> apiClient.getStatus(),  // Supplier that fetches actual value
    LoopType.SHORT,  // Loop strategy
    "API status code validation"  // Description
);

// Check if validation passed
if (result.isMatched()) {
    System.out.println("Validation passed on attempt: " + result.getAttemptNumber());
}
```

### API Validation Example

```java
// Wait until API returns expected status
boolean success = RetryValidator.waitUntilApiStatus(
    200,  // Expected status code
    () -> userApi.getUserById(userId).getStatusCode(),  // Status supplier
    LoopType.SHORT
);

Assert.assertTrue(success, "API should return 200");
```

### Database Validation Example

```java
// Wait until database record exists (for async operations)
boolean recordExists = RetryValidator.waitUntilRecordExists(
    () -> userDb.emailExists(email),  // Existence check
    LoopType.LONG,  // 60 attempts, 5s interval
    "users"  // Table name
);

Assert.assertTrue(recordExists, "Record should exist in database");
```

### UI Validation Example

```java
// Wait until element is visible
boolean isVisible = RetryValidator.waitUntilVisible(
    () -> page.isVisible("#elementId"),  // Visibility check
    LoopType.SHORT
);

Assert.assertTrue(isVisible, "Element should be visible");
```

### Custom Loop Parameters

```java
// Create custom loop: 20 iterations, 3 second interval
ValidationResult<String> result = RetryValidator.validateWithCustomLoop(
    "Expected Value",
    () -> fetchActualValue(),
    20,     // iterations
    3000,   // interval in milliseconds
    "Custom validation"
);
```

### String Contains Validation

```java
// Validate that response contains expected substring
ValidationResult<String> result = RetryValidator.validateStringContainsWithRetry(
    "user_created",  // Expected substring
    () -> apiResponse.getBody().asString(),  // Actual string
    LoopType.SHORT,
    "Response contains user_created message"
);
```

### Condition Waiting

```java
// Wait until a condition becomes true
boolean success = RetryValidator.waitUntil(
    () -> order.getStatus().equals("COMPLETED"),  // Condition
    LoopType.LONG,
    "Order completion"
);
```

### Validation Result

The `ValidationResult` object contains detailed information:

```java
ValidationResult<T> result = RetryValidator.validateWithRetry(...);

// Access result properties
result.getExpected();           // Expected value
result.getActual();            // Actual value
result.isMatched();            // True if matched
result.getAttemptNumber();     // Which attempt succeeded
result.getTotalAttempts();     // Total attempts made
result.getElapsedTimeMillis(); // Time taken
result.getElapsedTimeSeconds(); // Time in seconds
result.getSummary();           // Formatted summary string
```

## Data Diff

### Basic Data Comparison

```java
import com.smbc.raft.core.diff.*;

// Prepare expected data
List<Map<String, String>> expected = new ArrayList<>();
expected.add(Map.of("ID", "1", "Name", "John", "Status", "Active"));
expected.add(Map.of("ID", "2", "Name", "Jane", "Status", "Active"));

// Prepare actual data
List<Map<String, String>> actual = new ArrayList<>();
actual.add(Map.of("ID", "1", "Name", "John", "Status", "Active"));
actual.add(Map.of("ID", "2", "Name", "Jane", "Status", "Inactive"));  // Status changed

// Perform diff with "ID" as key field
DataDiff diff = new DataDiff(expected, actual, "ID");

// Check for differences
if (diff.isHasDifferences()) {
    System.out.println("Found differences!");
    diff.printSummary();
}
```

### Composite Key Diff

```java
// Use multiple fields as composite key
DataDiff diff = new DataDiff(
    expectedData,
    actualData,
    Arrays.asList("FirstName", "LastName")  // Composite key
);
```

### Accessing Diff Results

```java
DataDiff diff = new DataDiff(expected, actual, "ID");

// Get added rows (exist in actual but not in expected)
List<DiffRow> added = diff.getAddedRows();

// Get deleted rows (exist in expected but not in actual)
List<DiffRow> deleted = diff.getDeletedRows();

// Get modified rows (exist in both but with different values)
List<DiffRow> modified = diff.getModifiedRows();

// Get unchanged rows
List<DiffRow> unchanged = diff.getUnchangedRows();

// Get all differences
List<DiffRow> allDiffs = diff.getAllDifferences();

// Get summary
DiffSummary summary = diff.getSummary();
System.out.println("Added: " + summary.getAddedCount());
System.out.println("Deleted: " + summary.getDeletedCount());
System.out.println("Modified: " + summary.getModifiedCount());
System.out.println("Unchanged: " + summary.getUnchangedCount());
```

### Inspecting Modified Rows

```java
for (DiffRow row : diff.getModifiedRows()) {
    String key = row.getKey();
    Map<String, String> leftRow = row.getLeftRow();
    Map<String, String> rightRow = row.getRightRow();
    
    // Get field-level differences
    List<FieldDiff> fieldDiffs = row.getFieldDiffs();
    for (FieldDiff fieldDiff : fieldDiffs) {
        System.out.println(fieldDiff.getFieldName() + ": " + 
                          fieldDiff.getLeftValue() + " -> " + 
                          fieldDiff.getRightValue());
    }
}
```

## HTML Diff Reports

### Generate HTML Report

```java
// Perform diff
DataDiff diff = new DataDiff(expectedData, actualData, "ID");

// Generate HTML report
DiffHtmlReportGenerator.generateReport(
    diff,
    "test-output/diff-report.html",  // Output path
    "Expected Data",  // Left title
    "Actual Data"     // Right title
);
```

The HTML report includes:
- **Summary dashboard** with statistics
- **Added rows** section (highlighted in green)
- **Deleted rows** section (highlighted in red)
- **Modified rows** section (highlighted in yellow) with old/new values
- **Visual diff** showing exactly what changed
- **Responsive design** for easy viewing

### Compare CSV Files

```java
// Read CSV files
CSVHandler expected = new CSVHandler("expected.csv");
CSVHandler actual = new CSVHandler("actual.csv");

// Get data
List<Map<String, String>> expectedData = expected.getAllData();
List<Map<String, String>> actualData = actual.getAllData();

// Perform diff
DataDiff diff = new DataDiff(expectedData, actualData, "ID");

// Generate report
DiffHtmlReportGenerator.generateReport(
    diff,
    "test-output/csv-diff-report.html",
    "Expected CSV",
    "Actual CSV"
);
```

### Compare Excel Files

```java
// Read Excel files
ExcelHandler expected = new ExcelHandler("expected.xlsx");
expected.selectSheet("Sheet1");
List<Map<String, String>> expectedData = expected.getAllData();

ExcelHandler actual = new ExcelHandler("actual.xlsx");
actual.selectSheet("Sheet1");
List<Map<String, String>> actualData = actual.getAllData();

// Diff and report
DataDiff diff = new DataDiff(expectedData, actualData, "ID");
DiffHtmlReportGenerator.generateReport(diff, "report.html", "Expected", "Actual");
```

### Compare Database Results

```java
// Get database results
List<Map<String, Object>> expectedResults = 
    dbManager.executeQuery("SELECT * FROM expected_users");
List<Map<String, Object>> actualResults = 
    dbManager.executeQuery("SELECT * FROM actual_users");

// Convert to String maps
List<Map<String, String>> expected = convertToStringMaps(expectedResults);
List<Map<String, String>> actual = convertToStringMaps(actualResults);

// Diff and report
DataDiff diff = new DataDiff(expected, actual, "user_id");
DiffHtmlReportGenerator.generateReport(
    diff,
    "test-output/db-diff-report.html",
    "Expected Table",
    "Actual Table"
);
```

### Compare API Responses

```java
// Get API responses
Response expectedResp = apiClient.get("/api/expected");
Response actualResp = apiClient.get("/api/actual");

// Parse to data lists
List<Map<String, String>> expected = parseApiResponse(expectedResp);
List<Map<String, String>> actual = parseApiResponse(actualResp);

// Diff and report
DataDiff diff = new DataDiff(expected, actual, "id");
DiffHtmlReportGenerator.generateReport(
    diff,
    "test-output/api-diff-report.html",
    "Expected API Response",
    "Actual API Response"
);
```

## Combined Usage: Retry + Diff

### Retry API Validation Then Diff Results

```java
// Wait for API to return expected count
boolean success = RetryValidator.waitUntil(
    () -> userApi.getAllUsers().jsonPath().getList("$").size() == 5,
    LoopType.SHORT,
    "Wait for 5 users"
);

// Get actual data
List<Map<String, String>> actualUsers = parseApiUsers(userApi.getAllUsers());

// Compare with expected
DataDiff diff = new DataDiff(expectedUsers, actualUsers, "id");

// Generate report if there are differences
if (diff.isHasDifferences()) {
    DiffHtmlReportGenerator.generateReport(
        diff,
        "test-output/user-diff.html",
        "Expected Users",
        "Actual API Users"
    );
}

Assert.assertFalse(diff.isHasDifferences(), "Users should match");
```

### Retry Database Sync Then Validate

```java
// Wait for database to sync
RetryValidator.waitUntilRecordExists(
    () -> userDb.emailExists(email),
    LoopType.LONG,
    "users"
);

// Get database results
List<Map<String, String>> dbUsers = userDb.getAllUsers();

// Compare with expected state
DataDiff diff = new DataDiff(expectedState, dbUsers, "user_id");

if (diff.isHasDifferences()) {
    DiffHtmlReportGenerator.generateReport(
        diff,
        "test-output/db-sync-diff.html",
        "Expected State",
        "Database State"
    );
    Assert.fail("Database state doesn't match expected: " + diff.getSummary());
}
```

## Real-World Examples

### Example 1: E-commerce Order Processing

```java
@Test
public void testOrderProcessing() {
    // Create order
    int orderId = createOrder();
    
    // Wait for order to be processed (with retry)
    ValidationResult<String> result = RetryValidator.validateWithRetry(
        "COMPLETED",
        () -> orderApi.getOrder(orderId).jsonPath().getString("status"),
        LoopType.LONG,
        "Order processing status"
    );
    
    Assert.assertTrue(result.isMatched(), 
        "Order should be completed: " + result.getSummary());
    
    // Verify order details match expected
    Map<String, String> expectedOrder = getExpectedOrderDetails(orderId);
    Map<String, String> actualOrder = getActualOrderDetails(orderId);
    
    DataDiff diff = new DataDiff(
        List.of(expectedOrder),
        List.of(actualOrder),
        "order_id"
    );
    
    if (diff.isHasDifferences()) {
        DiffHtmlReportGenerator.generateReport(
            diff,
            "test-output/order-diff-" + orderId + ".html",
            "Expected Order",
            "Actual Order"
        );
    }
    
    Assert.assertFalse(diff.isHasDifferences(), "Order details should match");
}
```

### Example 2: Data Migration Validation

```java
@Test
public void testDataMigration() {
    // Trigger migration
    triggerDataMigration();
    
    // Wait for migration to complete
    boolean completed = RetryValidator.waitUntil(
        () -> getMigrationStatus().equals("COMPLETED"),
        LoopType.custom(120, 5000),  // 10 minutes max
        "Data migration completion"
    );
    
    Assert.assertTrue(completed, "Migration should complete");
    
    // Compare source and target data
    List<Map<String, String>> sourceData = getSourceData();
    List<Map<String, String>> targetData = getTargetData();
    
    DataDiff diff = new DataDiff(sourceData, targetData, "record_id");
    
    DiffHtmlReportGenerator.generateReport(
        diff,
        "test-output/migration-validation.html",
        "Source System Data",
        "Target System Data"
    );
    
    DiffSummary summary = diff.getSummary();
    System.out.println("Migration validation:");
    System.out.println("- Migrated: " + summary.getUnchangedCount());
    System.out.println("- Missing: " + summary.getDeletedCount());
    System.out.println("- Corrupted: " + summary.getModifiedCount());
    
    Assert.assertEquals(summary.getDeletedCount(), 0, "No records should be missing");
    Assert.assertEquals(summary.getModifiedCount(), 0, "No records should be corrupted");
}
```

### Example 3: Multi-System Integration Test

```java
@Test
public void testSystemIntegration() {
    // Send data to System A
    sendToSystemA(testData);
    
    // Wait for data to appear in System B (async integration)
    boolean synced = RetryValidator.waitUntil(
        () -> systemBApi.getRecordCount() == testData.size(),
        LoopType.LONG,
        "System A to System B sync"
    );
    
    Assert.assertTrue(synced, "Systems should be synced");
    
    // Get data from both systems
    List<Map<String, String>> systemAData = getSystemAData();
    List<Map<String, String>> systemBData = getSystemBData();
    
    // Compare
    DataDiff diff = new DataDiff(systemAData, systemBData, "transaction_id");
    
    if (diff.isHasDifferences()) {
        DiffHtmlReportGenerator.generateReport(
            diff,
            "test-output/integration-diff.html",
            "System A Data",
            "System B Data"
        );
        
        // Log detailed differences
        for (DiffRow row : diff.getModifiedRows()) {
            System.out.println("Data mismatch for: " + row.getKey());
            for (FieldDiff field : row.getFieldDiffs()) {
                System.out.println("  " + field);
            }
        }
    }
    
    Assert.assertFalse(diff.isHasDifferences(), 
        "Systems should have identical data");
}
```

## Best Practices

### 1. Choose Appropriate Loop Type

- **ONCE**: Use for immediate checks, no async operations
- **SHORT**: Use for fast operations (< 30 seconds)
- **LONG**: Use for batch jobs, async processing
- **CUSTOM**: Use for specific timing requirements

### 2. Provide Clear Descriptions

```java
// Good - clear description
RetryValidator.validateWithRetry(
    expectedValue,
    actualSupplier,
    LoopType.SHORT,
    "User creation API returns 201 status"
);

// Bad - vague description
RetryValidator.validateWithRetry(
    expectedValue,
    actualSupplier,
    LoopType.SHORT,
    "test"
);
```

### 3. Always Check Validation Results

```java
ValidationResult<T> result = RetryValidator.validateWithRetry(...);

if (!result.isMatched()) {
    // Log details for debugging
    log.error("Validation failed: " + result.getSummary());
    log.error("Expected: " + result.getExpected());
    log.error("Actual: " + result.getActual());
}

Assert.assertTrue(result.isMatched(), result.getSummary());
```

### 4. Use Meaningful Key Fields for Diff

```java
// Good - use stable, unique identifier
new DataDiff(expected, actual, "user_id");

// Good - composite key when needed
new DataDiff(expected, actual, Arrays.asList("first_name", "last_name", "dob"));

// Bad - don't use auto-generated or changing values
new DataDiff(expected, actual, "timestamp");
```

### 5. Generate Reports for Failed Validations

```java
if (diff.isHasDifferences()) {
    String reportPath = String.format(
        "test-output/diff-report-%s.html",
        LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))
    );
    
    DiffHtmlReportGenerator.generateReport(diff, reportPath, "Expected", "Actual");
    
    ExtentReportManager.logFail("Data mismatch detected. Report: " + reportPath);
}
```

## Troubleshooting

### Issue: Validation Never Succeeds

**Cause**: Actual value never matches expected, loop times out

**Solution**:
- Verify the supplier is returning correct data
- Check if expected value is correct
- Increase loop iterations or interval
- Add logging inside the supplier

```java
ValidationResult<String> result = RetryValidator.validateWithRetry(
    expected,
    () -> {
        String actual = fetchActualValue();
        System.out.println("Attempt value: " + actual);  // Debug logging
        return actual;
    },
    LoopType.SHORT,
    "description"
);
```

### Issue: Diff Shows Many False Positives

**Cause**: Data format or whitespace differences

**Solution**: Normalize data before comparison

```java
// Normalize data
List<Map<String, String>> normalized = data.stream()
    .map(row -> row.entrySet().stream()
        .collect(Collectors.toMap(
            Map.Entry::getKey,
            e -> e.getValue().trim().toLowerCase()
        )))
    .collect(Collectors.toList());
```

### Issue: HTML Report Not Generated

**Cause**: File path issue or permissions

**Solution**:
- Ensure output directory exists
- Use absolute paths
- Check file permissions

```java
// Create directory if it doesn't exist
new File("test-output").mkdirs();

// Use absolute path
String absolutePath = new File("test-output/report.html").getAbsolutePath();
DiffHtmlReportGenerator.generateReport(diff, absolutePath, "Left", "Right");
```

---

For more examples, see:
- `RetryValidationTest.java`
- `DataDiffTest.java`
