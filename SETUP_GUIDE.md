# Quick Setup Guide

## Prerequisites Installation

### 1. Install Java JDK 17
**Windows:**
- Download from Oracle or use OpenJDK
- Set JAVA_HOME environment variable

**Mac:**
```bash
brew install openjdk@17
```

**Linux:**
```bash
sudo apt-get install openjdk-17-jdk
```

Verify:
```bash
java -version
```

### 2. Install Maven
**Windows:**
- Download from https://maven.apache.org/
- Add to PATH

**Mac:**
```bash
brew install maven
```

**Linux:**
```bash
sudo apt-get install maven
```

Verify:
```bash
mvn -version
```

## Framework Setup

### Step 1: Download/Clone Framework
```bash
git clone <repository-url>
cd test-automation-framework
```

### Step 2: Install Framework Dependencies
```bash
mvn clean install -DskipTests
```

### Step 3: Install Playwright Browsers
```bash
mvn exec:java -e -D exec.mainClass=com.microsoft.playwright.CLI -D exec.args="install"
```

## Database Setup

### Oracle Database Setup

1. Install Oracle Database or use existing instance

2. Create test user:
```sql
CREATE USER testuser IDENTIFIED BY testpass;
GRANT CONNECT, RESOURCE TO testuser;
GRANT CREATE SESSION TO testuser;
GRANT CREATE TABLE TO testuser;
GRANT CREATE VIEW TO testuser;
GRANT CREATE SEQUENCE TO testuser;
```

3. Create test table:
```sql
CREATE TABLE users (
    user_id NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name VARCHAR2(100),
    email VARCHAR2(100) UNIQUE,
    password VARCHAR2(255),
    role VARCHAR2(50),
    status VARCHAR2(20) DEFAULT 'active',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);
```

### MS SQL Server Setup

1. Install SQL Server or use existing instance

2. Create test database and user:
```sql
CREATE DATABASE testdb;
GO

USE testdb;
GO

CREATE LOGIN testuser WITH PASSWORD = 'TestPassword123!';
CREATE USER testuser FOR LOGIN testuser;
ALTER ROLE db_owner ADD MEMBER testuser;
GO
```

3. Create test table:
```sql
CREATE TABLE users (
    user_id INT IDENTITY(1,1) PRIMARY KEY,
    name NVARCHAR(100),
    email NVARCHAR(100) UNIQUE,
    password NVARCHAR(255),
    role NVARCHAR(50),
    status NVARCHAR(20) DEFAULT 'active',
    created_at DATETIME DEFAULT GETDATE(),
    updated_at DATETIME
);
```

## Configuration

### Step 4: Configure Environment

Edit `core-framework/src/main/resources/config/qa.properties`:

```properties
# Application URL
app.url=https://your-app-url.com

# Browser Configuration
browser=chromium
headless=false
timeout=30000

# API Configuration
api.base.url=https://your-api-url.com

# Oracle Database Configuration
db.type=oracle
db.url=jdbc:oracle:thin:@localhost:1521:xe
db.username=testuser
db.password=testpass

# MS SQL Database Configuration (optional secondary)
db.secondary.type=mssql
db.secondary.url=jdbc:sqlserver://localhost:1433;databaseName=testdb
db.secondary.username=testuser
db.secondary.password=TestPassword123!
```

## Running Your First Test

### Step 5: Verify Setup

Run a simple smoke test:
```bash
mvn clean test -Dtest=LoginTest#testLoginPageElements -Denv=qa
```

### Step 6: Run All Tests
```bash
mvn clean test -Denv=qa
```

## Viewing Reports

### Extent Reports
After test execution, open:
```
test-output/extent-reports/TestReport_<timestamp>.html
```

### Allure Reports
Generate and view Allure report:
```bash
mvn allure:serve
```

## Common Issues and Solutions

### Issue: Browsers not found
**Solution:**
```bash
mvn exec:java -e -D exec.mainClass=com.microsoft.playwright.CLI -D exec.args="install"
```

### Issue: Database connection failed
**Solution:**
1. Verify database is running
2. Check connection URL format
3. Test credentials manually
4. Ensure firewall allows connection

### Issue: Tests fail with timeout
**Solution:**
- Increase timeout in properties file
- Check network connectivity
- Verify application is accessible

### Issue: Maven build fails
**Solution:**
```bash
mvn clean install -U
```

### Issue: Java version mismatch
**Solution:**
Ensure Java 17 or higher:
```bash
java -version
mvn -version
```

## Creating Your First Test

### 1. Create Page Object
Location: `application-automation/src/main/java/com/automation/app/pages/`

```java
public class MyPage extends BasePage {
    private static final String MY_ELEMENT = "#myElement";
    
    public void clickMyElement() {
        click(MY_ELEMENT);
    }
}
```

### 2. Create Test Class
Location: `application-tests/src/test/java/com/automation/tests/ui/`

```java
public class MyTest extends BaseTest {
    private MyPage myPage;
    
    @BeforeMethod
    public void setup() {
        PlaywrightManager.initializeBrowser();
        myPage = new MyPage();
    }
    
    @Test
    public void testMyFeature() {
        myPage.navigateTo("https://example.com");
        myPage.clickMyElement();
        Assert.assertTrue(true);
    }
}
```

### 3. Add to TestNG Suite
Edit: `application-tests/src/test/resources/testng.xml`

```xml
<test name="My Tests">
    <classes>
        <class name="com.automation.tests.ui.MyTest"/>
    </classes>
</test>
```

## Next Steps

1. Review example tests in `application-tests/src/test/java/`
2. Customize page objects for your application
3. Add API endpoints to API clients
4. Create database helpers for your schema
5. Write integration tests combining layers

## Getting Help

- Check README.md for detailed documentation
- Review example test implementations
- Check logs in `test-output/logs/automation.log`
- Review framework code and Javadocs

## Tips for Success

1. **Start Small**: Begin with simple UI tests
2. **Follow Patterns**: Use existing tests as templates
3. **Test Incrementally**: Test each layer independently
4. **Use Logging**: Leverage ExtentReportManager for visibility
5. **Keep Tests Clean**: Follow page object model principles
6. **Data Management**: Clean up test data after execution
7. **Version Control**: Commit working code frequently

## Framework Structure Reminder

```
Layer 1 (Core Framework): 
  - Reusable components for all projects
  - Browser, API, Database management
  
Layer 2 (Application Automation):
  - Your application-specific automation
  - Page objects, API clients, DB helpers
  
Layer 3 (Application Tests):
  - Your actual test cases
  - UI, API, Database, Integration tests
```

## Customization Points

1. **Modify Page Objects**: Update locators and methods for your app
2. **Add API Endpoints**: Create clients for your APIs
3. **Database Schema**: Update database helpers for your tables
4. **Configuration**: Add environment-specific properties
5. **Reporting**: Customize extent report configuration

Happy Testing!
