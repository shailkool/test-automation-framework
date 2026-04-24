# Framework Architecture Documentation

## Overview
This is a three-layered test automation framework designed for enterprise applications with complex testing requirements across UI, API, and database layers.

## Layer Architecture

### Layer 1: Core Framework (`core-framework`)
The foundational layer that provides reusable components and utilities.

#### Components:

**Configuration Management** (`com.framework.config`)
- `ConfigManager.java` - Centralized configuration handling
- Supports multiple environments (QA, UAT, PROD)
- Loads properties from files and system properties
- Provides type-safe getters for common configurations

**Database Management** (`com.framework.database`)
- `DatabaseManager.java` - Multi-database support with connection pooling
- Supported databases: Oracle, MS SQL Server, PostgreSQL, MySQL
- Uses HikariCP for efficient connection pooling
- Provides methods for queries, updates, and stored procedures
- Thread-safe singleton implementation

**UI Automation** (`com.framework.ui`)
- `PlaywrightManager.java` - Manages Playwright browser instances
- `BasePage.java` - Base class for Page Objects with common methods
- Supports Chromium, Firefox, and WebKit
- Thread-safe browser management
- Built-in screenshot capabilities

**API Testing** (`com.framework.api`)
- `RestAssuredClient.java` - REST API testing client
- Fluent API for building requests
- Built-in authentication support (Basic, OAuth2)
- JSON serialization/deserialization
- Request/response logging

**Reporting** (`com.framework.reporting`)
- `ExtentReportManager.java` - HTML report generation
- Screenshot attachment on failures
- Test categorization and metadata
- Thread-safe report management

**Test Data Registry** (`com.framework.data`)
- `TestDataRegistry.java` - Thread-safe tracking of test data for automatic cleanup
- Uses LIFO stack for teardown actions
- Integrated into `BaseTest` lifecycle

**Base Classes** (`com.framework.base`)
- `BaseTest.java` - Base test class with TestNG hooks
- Automatic setup and teardown
- Automatic test data cleanup via `TestDataRegistry`
- Failure screenshot capture

**Utilities** (`com.framework.utils`)
- `DataUtils.java` - Test data handling
- JSON and Excel file reading
- Random data generation
- Date/time utilities

### Layer 2: Application Automation (`app-automation`)
Application-specific implementations that extend the core framework.

#### Components:

**Page Objects** (`com.app.pages`)
- `LoginPage.java` - Login functionality
- `HomePage.java` - Home page interactions
- Implements Page Object Model pattern
- Extends `BasePage` from core framework

**API Clients** (`com.app.api`)
- `UserApiClient.java` - User management API endpoints
- Encapsulates API logic for specific features
- Built on `RestAssuredClient` from core framework

**Database Queries** (`com.app.database`)
- `UserDatabaseQueries.java` - User-related database operations
- Application-specific SQL queries
- Data access layer for test data setup and verification

**Workflows** (`com.app.workflows`)
- `UserWorkflow.java` - Business process workflows
- Combines UI, API, and Database operations
- Reusable business logic for tests

**Data Fixtures** (`com.app.fixtures`)
- `UserFixture.java` - Domain-specific data builders
- `TestDataFactory.java` - Central entry point for creating test data
- Automatic registration for cleanup via Core Framework

### Layer 3: Tests (`app-tests`)
Actual test implementations organized by type.

#### Test Types:

**UI Tests** (`com.tests.ui`)
- End-user interface testing
- Uses Page Objects from Layer 2
- Example: `LoginUITest.java`

**API Tests** (`com.tests.api`)
- REST API endpoint testing
- Uses API Clients from Layer 2
- Example: `UserApiTest.java`

**Database Tests** (`com.tests.integration`)
- Database integration testing
- Direct database verification
- Example: `UserDatabaseTest.java`

**End-to-End Tests** (`com.tests.integration`)
- Complete business scenarios
- Tests across all layers
- Example: `EndToEndIntegrationTest.java`

## Key Design Patterns

### 1. Page Object Model (POM)
- Separates test logic from page structure
- Improves maintainability
- Reduces code duplication

### 2. Singleton Pattern
- Used for managers (ConfigManager, DatabaseManager)
- Ensures single instance across test execution
- Thread-safe implementation

### 3. Factory Pattern
- Database connection factory
- Browser creation factory

### 4. Fluent API
- Chainable method calls in API client
- Improves readability

### 5. Builder Pattern
- Request specification building
- Test data construction

### 6. Test Data Factory Pattern
- Centralized creation of domain objects (Users, Orders, etc.)
- Thread-safe automatic cleanup registration
- Decouples test data setup from test logic

## Database Support

### Supported Databases:
1. **Oracle Database**
   - Driver: ojdbc11
   - Connection URL format: `jdbc:oracle:thin:@host:port:sid`

2. **Microsoft SQL Server**
   - Driver: mssql-jdbc
   - Connection URL format: `jdbc:sqlserver://host:port;databaseName=dbname`

3. **PostgreSQL**
   - Driver: postgresql
   - Connection URL format: `jdbc:postgresql://host:port/database`

4. **MySQL**
   - Driver: mysql-connector-j
   - Connection URL format: `jdbc:mysql://host:port/database`

### Connection Pooling:
- Uses HikariCP for optimal performance
- Configurable pool size
- Automatic connection validation
- Connection timeout handling

## Configuration

### Environment-Specific Configuration:
- `config.properties` - Default configuration
- `config-qa.properties` - QA environment
- `config-uat.properties` - UAT environment
- `config-prod.properties` - Production environment

### Property Precedence:
1. System properties (highest)
2. Environment-specific properties
3. Default properties (lowest)

## Best Practices

### 1. Test Independence
- Each test should be independent
- No dependencies between tests
- Clean up test data in @AfterMethod

### 2. Data Management
- Use DataUtils for test data generation
- Store test data in JSON/Excel files
- Clean up database after tests

### 3. Logging
- Use Log4j2 for logging
- Log important actions and verifications
- Different log levels for different environments

### 4. Reporting
- Use ExtentReports for HTML reports
- Add meaningful test descriptions
- Attach screenshots on failures
- Categorize tests appropriately

### 5. Error Handling
- Use try-catch for external operations
- Meaningful error messages
- Fail fast on critical errors

## Execution

### Running Tests:

```bash
# Run all tests
mvn clean test

# Run specific test suite
mvn test -Dsuite=testng.xml

# Run with specific environment
mvn test -Denv=qa

# Run specific test class
mvn test -Dtest=LoginUITest

# Run with parallel execution
mvn test -Dparallel=classes -DthreadCount=3

# Generate reports only
mvn surefire-report:report
```

### CI/CD Integration:

```yaml
# Jenkins/GitLab CI example
script:
  - mvn clean install
  - mvn test -Denv=$ENVIRONMENT
  - mvn surefire-report:report
artifacts:
  paths:
    - target/extent-reports/
    - target/surefire-reports/
```

## Troubleshooting

### Common Issues:

1. **Browser not launching**
   - Check Playwright installation: `mvn exec:java -e -D exec.mainClass=com.microsoft.playwright.CLI -D exec.args="install"`
   
2. **Database connection fails**
   - Verify connection properties
   - Check if database is accessible
   - Test with `DatabaseManager.testConnection()`

3. **Tests failing randomly**
   - Check for race conditions
   - Increase timeouts if needed
   - Review thread safety

4. **Memory issues**
   - Increase Maven memory: `export MAVEN_OPTS="-Xmx2048m"`
   - Clean up resources in @AfterMethod

## Extension Points

### Adding New Database Type:
1. Add driver dependency in parent POM
2. Update `DatabaseManager.DatabaseType` enum
3. Add case in `initializeDataSource()` method
4. Add configuration properties

### Adding New Page:
1. Create class in `app-automation/pages`
2. Extend `BasePage`
3. Define locators as constants
4. Implement page-specific methods

### Adding New API Client:
1. Create class in `app-automation/api`
2. Initialize `RestAssuredClient`
3. Define endpoint constants
4. Implement API methods

### Adding New Test:
1. Create test class in appropriate package
2. Extend `BaseTest`
3. Use @Test annotation
4. Add to testng.xml

## Performance Considerations

1. **Parallel Execution**
   - Configure in testng.xml
   - Use ThreadLocal for thread safety
   - Set appropriate thread count

2. **Database Optimization**
   - Use connection pooling
   - Batch database operations when possible
   - Close connections properly

3. **Browser Performance**
   - Reuse browser contexts when possible
   - Clean up resources after tests
   - Use headless mode for faster execution

## Maintenance

### Regular Tasks:
- Update dependencies monthly
- Review and refactor duplicate code
- Update test data
- Archive old test reports
- Review and update documentation

### Dependency Updates:
```bash
# Check for updates
mvn versions:display-dependency-updates

# Update specific dependency
mvn versions:use-latest-versions
```

## Support

For issues or questions:
1. Check this documentation
2. Review example tests
3. Check logs in `logs/` directory
4. Review ExtentReports in `target/extent-reports/`
