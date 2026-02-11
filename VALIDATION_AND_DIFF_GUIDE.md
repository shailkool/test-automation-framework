## Validation Loop and Data Diff Guide

## Overview

The framework provides powerful capabilities for:
1. **Validation Loop**: Polling/retry mechanism to validate expected vs actual results
2. **Data Diff**: Compare datasets and identify differences with HTML reports

These features work seamlessly with UI, API, and Database testing.

---

## Part 1: Validation Loop (Polling & Retry)

### Loop Types

The framework provides predefined loop strategies:

| Loop Type | Iterations | Interval | Total Time | Use Case |
|-----------|-----------|----------|------------|----------|
| **ONCE** | 1 | 0ms | Immediate | Quick checks, no retry needed |
| **SHORT** | 15 | 2000ms | 30 seconds | UI elements, API responses |
| **LONG** | 60 | 5000ms | 5 minutes | Async processing, batch jobs |
| **CUSTOM** | Custom | Custom | Variable | Project-specific needs |

### Basic Usage

#### 1. Simple Validation (Once)

```java
ValidationLoop loop = ValidationLoop.once();

ValidationResult<Boolean> result = loop.validateTrue(() -> {
    return homePage.isLoaded();
});

Assert.assertTrue(result.isSuccess());
```

#### 2. Short Loop (30 seconds)

```java
ValidationLoop loop = ValidationLoop.shortLoop();

ValidationResult<Boolean> result = loop.validateTrue(() -> {
    return element.isVisible();
});
```

#### 3. Long Loop (5 minutes)

```java
ValidationLoop loop = ValidationLoop.longLoop();

ValidationResult<Boolean> result = loop.validateTrue(() -> {
    return dataProcessingComplete();
});
```

#### 4. Custom Loop

```java
// 20 iterations, 3 seconds interval = 60 seconds total
ValidationLoop loop = ValidationLoop.custom(20, 3000);

ValidationResult<String> result = loop.validateEquals(
    () -> getStatus(),
    "COMPLETED"
);
```

### UI Validation Examples

#### Wait for Element to Appear

```java
ValidationLoop loop = ValidationLoop.shortLoop();

ValidationResult<Boolean> result = loop.validateTrue(() -> {
    return page.isVisible("#submit-button");
});

if (result.isSuccess()) {
    page.click("#submit-button");
}
```

#### Wait for Text to Change

```java
ValidationLoop loop = ValidationLoop.shortLoop();

ValidationResult<String> result = loop.validateEquals(
    () -> page.getText(".status"),
    "Processing Complete"
);
```

#### Validate Element Contains Text

```java
ValidationLoop loop = ValidationLoop.shortLoop();

ValidationResult<String> result = loop.validateContains(
    () -> page.getText(".message"),
    "Success"
);
```

### API Validation Examples

#### Wait for API to Return Success

```java
ValidationLoop loop = ValidationLoop.shortLoop();

ValidationResult<Response> result = loop.validateCondition(
    () -> apiClient.getStatus(orderId),
    response -> response.getStatusCode() == 200
);
```

#### Validate API Response Data

```java
ValidationLoop loop = ValidationLoop.shortLoop();

ValidationResult<Response> result = loop.validate(
    () -> apiClient.getOrder(orderId),
    (actual, expected) -> {
        return actual.jsonPath().getString("status").equals("SHIPPED");
    },
    null
);
```

#### Wait for Record Count

```java
ValidationLoop loop = ValidationLoop.longLoop();

ValidationResult<Integer> result = loop.validateEquals(
    () -> {
        Response response = apiClient.getOrders();
        return response.jsonPath().getList("$").size();
    },
    expectedCount
);
```

### Database Validation Examples

#### Wait for Record to Exist

```java
ValidationLoop loop = ValidationLoop.shortLoop();

ValidationResult<Boolean> result = loop.validateTrue(() -> {
    return dbHelper.recordExists(userId);
});
```

#### Validate Field Value

```java
ValidationLoop loop = ValidationLoop.longLoop();

ValidationResult<String> result = loop.validateEquals(
    () -> {
        Map<String, Object> record = dbHelper.getRecord(id);
        return (String) record.get("status");
    },
    "COMPLETED"
);
```

#### Wait for Row Count

```java
ValidationLoop loop = ValidationLoop.shortLoop();

ValidationResult<Integer> result = loop.validateEquals(
    () -> dbHelper.getRecordCount("orders", "status='PENDING'"),
    0  // Wait until no pending orders
);
```

### Configuration from Properties

Define loop parameters in configuration files:

```properties
# default.properties
validation.loop.type=SHORT
validation.loop.iterations=15
validation.loop.interval=2000

# For specific scenarios
order.processing.loop.type=LONG
order.processing.loop.iterations=120
order.processing.loop.interval=5000
```

Use in code:

```java
LoopConfig config = LoopConfig.fromProperties("order.processing");
ValidationLoop loop = new ValidationLoop(config);
```

### Advanced Usage

#### Custom Validation Logic

```java
ValidationLoop loop = ValidationLoop.shortLoop();

ValidationResult<Response> result = loop.validate(
    () -> apiClient.getUserById(userId),
    (actual, expected) -> {
        // Complex validation logic
        boolean statusOk = actual.getStatusCode() == 200;
        boolean emailVerified = actual.jsonPath().getBoolean("emailVerified");
        boolean accountActive = actual.jsonPath().getString("status").equals("active");
        
        return statusOk && emailVerified && accountActive;
    },
    null
);
```

#### Handle Failures Gracefully

```java
LoopConfig config = LoopConfig.custom(5, 1000);
config.setThrowOnFailure(false);  // Don't throw exception

ValidationLoop loop = new ValidationLoop(config);

ValidationResult<Boolean> result = loop.validateTrue(() -> {
    return checkCondition();
});

if (result.isFailed()) {
    log.warn("Validation failed after " + result.getIterations() + " attempts");
    // Handle failure
}
```

#### Get Detailed Results

```java
ValidationResult<String> result = loop.validateEquals(
    () -> getStatus(),
    "READY"
);

System.out.println("Success: " + result.isSuccess());
System.out.println("Iterations: " + result.getIterations());
System.out.println("Duration: " + result.getDurationSeconds() + " seconds");
System.out.println("Actual Value: " + result.getActualValue());
System.out.println("Expected Value: " + result.getExpectedValue());
```

---

## Part 2: Data Diff (Dataset Comparison)

### Basic Comparison

```java
// Prepare datasets
List<Map<String, String>> expected = getExpectedData();
List<Map<String, String>> actual = getActualData();

// Compare
DataDiff diff = DataDiff.builder()
        .keyField("ID")
        .build();

DiffResult result = diff.compare(expected, actual);

// Check results
if (result.isIdentical()) {
    System.out.println("Data is identical!");
} else {
    System.out.println("Differences found:");
    System.out.println("  Added: " + result.getAddedRows());
    System.out.println("  Deleted: " + result.getDeletedRows());
    System.out.println("  Modified: " + result.getModifiedRows());
}
```

### Configuration Options

```java
DataDiff diff = DataDiff.builder()
        .keyField("UserID")           // Primary key field
        .keyFields(Arrays.asList("FirstName", "LastName"))  // Composite key
        .ignoreCase(true)             // Case-insensitive comparison
        .trimValues(true)             // Trim whitespace
        .ignoreField("UpdatedAt")     // Ignore timestamp fields
        .ignoreField("CreatedAt")
        .build();
```

### Comparing Different Data Sources

#### Compare CSV Files

```java
// Read CSV files
CSVHandler expectedCsv = new CSVHandler("expected.csv");
CSVHandler actualCsv = new CSVHandler("actual.csv");

List<Map<String, String>> expected = expectedCsv.getAllData();
List<Map<String, String>> actual = actualCsv.getAllData();

// Compare
DataDiff diff = DataDiff.builder()
        .keyField("OrderID")
        .build();

DiffResult result = diff.compare(expected, actual);
```

#### Compare Excel Files

```java
// Read Excel files
ExcelHandler expectedExcel = new ExcelHandler("expected.xlsx");
expectedExcel.selectSheet("Products");
List<Map<String, String>> expected = expectedExcel.getAllData();

ExcelHandler actualExcel = new ExcelHandler("actual.xlsx");
actualExcel.selectSheet("Products");
List<Map<String, String>> actual = actualExcel.getAllData();

// Compare
DataDiff diff = DataDiff.builder()
        .keyField("ProductID")
        .ignoreField("LastModified")
        .build();

DiffResult result = diff.compare(expected, actual);
```

#### Compare Database Results

```java
DatabaseManager db = DatabaseManager.getInstance();

// Get expected data
String expectedQuery = "SELECT * FROM users WHERE region='WEST'";
List<Map<String, Object>> expectedRaw = db.executeQuery(expectedQuery);

// Get actual data
String actualQuery = "SELECT * FROM users_staging WHERE region='WEST'";
List<Map<String, Object>> actualRaw = db.executeQuery(actualQuery);

// Convert to String maps
List<Map<String, String>> expected = convertToStringMaps(expectedRaw);
List<Map<String, String>> actual = convertToStringMaps(actualRaw);

// Compare
DataDiff diff = DataDiff.builder()
        .keyField("user_id")
        .build();

DiffResult result = diff.compare(expected, actual);
```

#### Compare API Responses

```java
// Get expected response
Response expectedResponse = apiClient.get("/api/products");
List<Map<String, String>> expected = expectedResponse.jsonPath().getList("$");

// Get actual response (from another environment)
Response actualResponse = apiClient2.get("/api/products");
List<Map<String, String>> actual = actualResponse.jsonPath().getList("$");

// Compare
DataDiff diff = DataDiff.builder()
        .keyField("productId")
        .build();

DiffResult result = diff.compare(expected, actual);
```

### HTML Report Generation

#### Basic Report

```java
DiffResult result = diff.compare(expected, actual);

DiffReportGenerator generator = new DiffReportGenerator();
generator.saveReport(result, "Product Comparison Report", 
                    "reports/product-diff.html");
```

#### Custom Report Title

```java
String title = String.format("Data Comparison - %s vs %s", 
                            environment1, environment2);
generator.saveReport(result, title, "reports/env-comparison.html");
```

### Analyzing Diff Results

#### Get Modified Records

```java
for (DiffResult.RowDifference row : result.getModifiedRecords()) {
    System.out.println("Modified Record Key: " + row.getKeyValue());
    
    for (DiffResult.FieldDifference field : row.getFieldDifferences()) {
        System.out.println("  Field: " + field.getFieldName());
        System.out.println("  Expected: " + field.getExpectedValue());
        System.out.println("  Actual: " + field.getActualValue());
    }
}
```

#### Get Added Records

```java
for (Map<String, String> record : result.getAddedRecords()) {
    System.out.println("Added Record: " + record);
}
```

#### Get Deleted Records

```java
for (Map<String, String> record : result.getDeletedRecords()) {
    System.out.println("Deleted Record: " + record);
}
```

### Integration with Tests

```java
@Test
public void testDataConsistency() {
    // Get data from two sources
    List<Map<String, String>> sourceA = getDataFromSourceA();
    List<Map<String, String>> sourceB = getDataFromSourceB();
    
    // Compare
    DataDiff diff = DataDiff.builder()
            .keyField("ID")
            .build();
    
    DiffResult result = diff.compare(sourceA, sourceB);
    
    // Generate report
    DiffReportGenerator generator = new DiffReportGenerator();
    generator.saveReport(result, "Data Consistency Check", 
                        "test-output/consistency-report.html");
    
    // Assert
    Assert.assertTrue(result.isIdentical(), 
        "Data sources should be identical. Report: consistency-report.html");
}
```

### Real-World Examples

#### Compare Production vs Staging Data

```java
DatabaseManager prodDb = DatabaseManager.getInstance("production");
DatabaseManager stagingDb = DatabaseManager.getInstance("staging");

String query = "SELECT * FROM orders WHERE order_date = CURRENT_DATE";

List<Map<String, String>> prodData = convertToString(prodDb.executeQuery(query));
List<Map<String, String>> stagingData = convertToString(stagingDb.executeQuery(query));

DataDiff diff = DataDiff.builder()
        .keyField("order_id")
        .ignoreField("created_at")    // Ignore timestamps
        .ignoreField("updated_at")
        .build();

DiffResult result = diff.compare(prodData, stagingData);

if (!result.isIdentical()) {
    DiffReportGenerator generator = new DiffReportGenerator();
    generator.saveReport(result, "Prod vs Staging Comparison", 
                        "reports/prod-staging-diff.html");
    
    // Send alert
    sendAlert("Data mismatch detected! Check report.");
}
```

#### Validate Data Migration

```java
// Before migration
List<Map<String, String>> beforeMigration = dbHelper.getAllRecords("users");

// Perform migration
performMigration();

// After migration
List<Map<String, String>> afterMigration = dbHelper.getAllRecords("users_new");

// Compare
DataDiff diff = DataDiff.builder()
        .keyField("user_id")
        .build();

DiffResult result = diff.compare(beforeMigration, afterMigration);

Assert.assertTrue(result.isIdentical(), 
    "Migration should preserve all data");
```

#### Compare API Versions

```java
// V1 API response
List<Map<String, String>> v1Response = apiClientV1.getUsers();

// V2 API response
List<Map<String, String>> v2Response = apiClientV2.getUsers();

DataDiff diff = DataDiff.builder()
        .keyField("userId")
        .ignoreField("apiVersion")  // Different by design
        .build();

DiffResult result = diff.compare(v1Response, v2Response);
```

### Best Practices

1. **Choose the Right Key Field**
   ```java
   // Use unique identifier
   .keyField("ID")
   
   // Or composite key
   .keyFields(Arrays.asList("FirstName", "LastName", "DOB"))
   ```

2. **Ignore Irrelevant Fields**
   ```java
   .ignoreField("timestamp")
   .ignoreField("last_updated")
   .ignoreField("version")
   ```

3. **Handle Case Sensitivity**
   ```java
   .ignoreCase(true)  // For text comparisons
   .trimValues(true)  // Remove leading/trailing spaces
   ```

4. **Generate Reports for Failed Tests**
   ```java
   if (!result.isIdentical()) {
       String reportPath = "test-output/diff-" + testName + ".html";
       generator.saveReport(result, testName, reportPath);
       Assert.fail("Differences found. See report: " + reportPath);
   }
   ```

5. **Use with Validation Loop**
   ```java
   ValidationLoop loop = ValidationLoop.shortLoop();
   
   ValidationResult<Boolean> validation = loop.validateTrue(() -> {
       List<Map<String, String>> expected = getExpectedData();
       List<Map<String, String>> actual = getCurrentData();
       
       DiffResult diff = DataDiff.builder()
               .keyField("ID")
               .build()
               .compare(expected, actual);
       
       return diff.isIdentical();
   });
   ```

---

## Complete Example: E2E Validation with Diff

```java
@Test
public void testOrderProcessingWithValidation() {
    // Submit order via API
    Response response = orderApi.submitOrder(orderData);
    String orderId = response.jsonPath().getString("orderId");
    
    // Wait for order to be processed (with retry)
    ValidationLoop loop = ValidationLoop.longLoop();
    
    ValidationResult<String> statusCheck = loop.validateEquals(
        () -> orderApi.getOrderStatus(orderId),
        "COMPLETED"
    );
    
    Assert.assertTrue(statusCheck.isSuccess(), 
        "Order should be processed");
    
    // Get expected order details
    List<Map<String, String>> expectedItems = getExpectedOrderItems();
    
    // Get actual order details from database
    List<Map<String, String>> actualItems = dbHelper.getOrderItems(orderId);
    
    // Compare
    DataDiff diff = DataDiff.builder()
            .keyField("itemId")
            .ignoreField("timestamp")
            .build();
    
    DiffResult result = diff.compare(expectedItems, actualItems);
    
    if (!result.isIdentical()) {
        DiffReportGenerator generator = new DiffReportGenerator();
        generator.saveReport(result, 
            "Order Items Comparison - " + orderId, 
            "reports/order-" + orderId + "-diff.html");
        
        Assert.fail("Order items mismatch. Report generated.");
    }
    
    ExtentReportManager.logPass("Order processed correctly with all items matching");
}
```

---

For more examples, see:
- `ValidationLoopExamplesTest.java`
- `DataDiffExamplesTest.java`
