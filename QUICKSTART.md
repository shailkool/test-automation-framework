# Quick Start Guide

## Prerequisites Installation

### 1. Install Java
```bash
# Verify Java installation
java -version

# Should show Java 17 or higher
```

### 2. Install Maven
```bash
# Verify Maven installation
mvn -version

# Should show Maven 3.8 or higher
```

### 3. Clone and Setup Project
```bash
# Navigate to project directory
cd test-automation-framework

# Install all dependencies and build
mvn clean install

# Install Playwright browsers
mvn exec:java -e -D exec.mainClass=com.microsoft.playwright.CLI -D exec.args="install"
```

## Configuration Setup

### Step 1: Configure Database Connections

Edit `core-framework/src/main/resources/config.properties`:

```properties
# Oracle Database
db.oracle.url=jdbc:oracle:thin:@your-oracle-host:1521:xe
db.oracle.username=your_username
db.oracle.password=your_password

# MS SQL Server
db.mssql.url=jdbc:sqlserver://your-mssql-host:1433;databaseName=testdb
db.mssql.username=your_username
db.mssql.password=your_password

# Application URLs
app.ui.url=https://your-application-url.com
app.api.baseurl=https://api.your-application-url.com
```

### Step 2: Environment-Specific Configuration (Optional)

Create `config-qa.properties` for QA environment:
```properties
app.ui.url=https://qa.your-application.com
app.api.baseurl=https://api-qa.your-application.com
```

## Running Your First Test

### Option 1: Run from Command Line
```bash
# Run all tests
mvn clean test

# Run with specific environment
mvn test -Denv=qa

# Run specific test class
mvn test -Dtest=LoginUITest
```

### Option 2: Run from IDE (IntelliJ IDEA)
1. Open project in IntelliJ IDEA
2. Navigate to `app-tests/src/test/java/com/smbc/raft/tests/ui/LoginUITest.java`
3. Right-click on the test class
4. Select "Run LoginUITest"

### Option 3: Run via TestNG XML
```bash
mvn test -DsuiteXmlFile=testng.xml
```

## Viewing Test Reports

### ExtentReports (Recommended)
After test execution:
```bash
# Report location
open target/extent-reports/TestReport_[timestamp].html
```

### Surefire Reports
```bash
# Generate HTML report
mvn surefire-report:report

# View report
open target/site/surefire-report.html
```

## Creating Your First Test

### Step 1: Create a New Page Object
Create `app-automation/src/main/java/com/smbc/raft/app/pages/ProductPage.java`:

```java
package com.smbc.raft.app.pages;

import com.smbc.raft.core.ui.BasePage;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class ProductPage extends BasePage {
    
    private static final String PRODUCT_TITLE = "h1.product-title";
    private static final String ADD_TO_CART_BUTTON = "button#add-to-cart";
    
    public String getProductTitle() {
        return getText(PRODUCT_TITLE);
    }
    
    public void addToCart() {
        click(ADD_TO_CART_BUTTON);
        log.info("Product added to cart");
    }
}
```

### Step 2: Create a New Test
Create `app-tests/src/test/java/com/smbc/raft/tests/ui/ProductUITest.java`:

```java
package com.smbc.raft.tests.ui;

import com.smbc.raft.app.pages.ProductPage;
import com.smbc.raft.core.base.BaseTest;
import com.smbc.raft.core.ui.PlaywrightManager;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class ProductUITest extends BaseTest {
    
    private ProductPage productPage;
    
    @BeforeMethod
    public void setupTest() {
        PlaywrightManager.initializeBrowser();
        productPage = new ProductPage();
    }
    
    @Test
    public void testAddProductToCart() {
        PlaywrightManager.navigateTo("https://example.com/product/1");
        
        String title = productPage.getProductTitle();
        Assert.assertNotNull(title);
        
        productPage.addToCart();
    }
}
```

### Step 3: Add Test to Suite
Edit `app-tests/src/test/resources/testng.xml`:

```xml
<test name="Product Tests">
    <classes>
        <class name="com.smbc.raft.tests.ui.ProductUITest"/>
    </classes>
</test>
```

## Creating Your First API Test

### Step 1: Create API Client
Create `app-automation/src/main/java/com/smbc/raft/app/api/ProductApiClient.java`:

```java
package com.smbc.raft.app.api;

import com.smbc.raft.core.api.RestAssuredClient;
import io.restassured.response.Response;

public class ProductApiClient {
    private RestAssuredClient client;
    
    public ProductApiClient() {
        this.client = new RestAssuredClient();
    }
    
    public Response getProduct(int productId) {
        return client.get("/api/products/" + productId);
    }
}
```

### Step 2: Create API Test
Create `app-tests/src/test/java/com/smbc/raft/tests/api/ProductApiTest.java`:

```java
package com.smbc.raft.tests.api;

import com.smbc.raft.app.api.ProductApiClient;
import com.smbc.raft.core.base.BaseTest;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.Test;

public class ProductApiTest extends BaseTest {
    
    @Test
    public void testGetProduct() {
        ProductApiClient client = new ProductApiClient();
        Response response = client.getProduct(1);
        
        Assert.assertEquals(response.getStatusCode(), 200);
        Assert.assertNotNull(response.jsonPath().getString("name"));
    }
}
```

## Creating Your First Database Test

### Step 1: Create Database Queries
Create `app-automation/src/main/java/com/smbc/raft/app/database/ProductDatabaseQueries.java`:

```java
package com.smbc.raft.app.database;

import com.smbc.raft.core.database.DatabaseManager;
import com.smbc.raft.core.database.DatabaseManager.DatabaseType;
import java.util.List;
import java.util.Map;

public class ProductDatabaseQueries {
    private DatabaseManager dbManager;
    private DatabaseType dbType;
    
    public ProductDatabaseQueries(DatabaseType dbType) {
        this.dbManager = DatabaseManager.getInstance();
        this.dbType = dbType;
    }
    
    public Map<String, Object> getProductById(int productId) {
        String query = "SELECT * FROM products WHERE product_id = ?";
        List<Map<String, Object>> results = 
            dbManager.executeQuery(dbType, query, productId);
        return results.isEmpty() ? null : results.get(0);
    }
}
```

### Step 2: Create Database Test
Create `app-tests/src/test/java/com/smbc/raft/tests/integration/ProductDatabaseTest.java`:

```java
package com.smbc.raft.tests.integration;

import com.smbc.raft.app.database.ProductDatabaseQueries;
import com.smbc.raft.core.base.BaseTest;
import com.smbc.raft.core.database.DatabaseManager.DatabaseType;
import org.testng.Assert;
import org.testng.annotations.Test;
import java.util.Map;

public class ProductDatabaseTest extends BaseTest {
    
    @Test
    public void testGetProductFromDatabase() {
        ProductDatabaseQueries queries = 
            new ProductDatabaseQueries(DatabaseType.ORACLE);
        
        Map<String, Object> product = queries.getProductById(1);
        
        Assert.assertNotNull(product);
        Assert.assertEquals(product.get("product_id"), 1);
    }
}
```

## Common Tasks

### Change Browser Type
Edit `config.properties`:
```properties
# Options: chromium, firefox, webkit
browser.type=chromium
```

### Run Tests in Headless Mode
Edit `config.properties`:
```properties
browser.headless=true
```

### Change Database
In your test or workflow:
```java
DatabaseType dbType = DatabaseType.MSSQL; // or ORACLE, POSTGRESQL, MYSQL
```

### Add Custom Configuration
1. Add property to `config.properties`:
   ```properties
   custom.property=value
   ```

2. Add getter in ConfigManager:
   ```java
   public String getCustomProperty() {
       return getProperty("custom.property");
   }
   ```

### Take Screenshots During Test
```java
PlaywrightManager.takeScreenshot("my-screenshot.png");
```

### Execute Custom SQL
```java
String query = "SELECT * FROM custom_table WHERE condition = ?";
List<Map<String, Object>> results = 
    dbManager.executeQuery(DatabaseType.ORACLE, query, "value");
```

## Troubleshooting

### Tests Not Running
```bash
# Clean and rebuild
mvn clean install

# Check for compilation errors
mvn compile
```

### Browser Not Launching
```bash
# Reinstall Playwright browsers
mvn exec:java -e -D exec.mainClass=com.microsoft.playwright.CLI -D exec.args="install chromium"
```

### Database Connection Issues
```bash
# Test connection independently
# Create a simple test to check DatabaseManager.testConnection()
```

### Maven Build Fails
```bash
# Skip tests during build
mvn clean install -DskipTests

# Update dependencies
mvn clean install -U
```

## Next Steps

1. **Read Architecture Documentation**: `ARCHITECTURE.md`
2. **Review Example Tests**: Check tests in `app-tests/src/test/java/com/smbc/raft/tests/`
3. **Customize for Your Application**: Modify page objects, API clients, and database queries
4. **Add Your Tests**: Create new test classes following the examples
5. **Configure CI/CD**: Integrate with your CI/CD pipeline

## Getting Help

- Review logs in `logs/test-automation.log`
- Check ExtentReports in `target/extent-reports/`
- Review example tests for usage patterns
- Consult `ARCHITECTURE.md` for detailed information

## Tips for Success

1. **Start Small**: Begin with simple UI or API tests
2. **Follow Patterns**: Use existing tests as templates
3. **Keep Tests Independent**: Each test should run standalone
4. **Clean Up Data**: Always clean test data after execution
5. **Use Descriptive Names**: Name tests clearly indicating what they test
6. **Add Logging**: Log important steps for debugging
7. **Handle Waits Properly**: Use explicit waits instead of Thread.sleep()
8. **Organize Tests**: Group related tests in same class/suite
