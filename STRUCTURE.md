# Test Automation Framework - Directory Structure

```
test-automation-framework/
│
├── README.md                           # Project overview and setup
├── ARCHITECTURE.md                     # Detailed architecture documentation
├── QUICKSTART.md                       # Quick start guide
├── .gitignore                          # Git ignore file
├── pom.xml                             # Parent Maven POM
│
├── core-framework/                     # Layer 1: Core Framework
│   ├── pom.xml
│   └── src/
│       ├── main/
│       │   ├── java/
│       │   │   └── com/smbc/raft/core/
│       │   │       ├── config/
│       │   │       │   └── ConfigManager.java          # Configuration management
│       │   │       ├── database/
│       │   │       │   └── DatabaseManager.java        # Multi-database support
│       │   │       ├── ui/
│       │   │       │   ├── PlaywrightManager.java      # Browser management
│       │   │       │   └── BasePage.java               # Base page object
│       │   │       ├── api/
│       │   │       │   └── RestAssuredClient.java      # API testing client
│       │   │       ├── reporting/
│       │   │       │   └── ExtentReportManager.java    # Test reporting
│       │   │       ├── base/
│       │   │       │   └── BaseTest.java               # Base test class
│       │   │       └── utils/
│       │   │           └── DataUtils.java              # Data utilities
│       │   └── resources/
│       │       ├── config.properties                    # Default configuration
│       │       └── log4j2.xml                          # Logging configuration
│       └── test/
│           └── java/                                    # (Core framework tests)
│
├── app-automation/                     # Layer 2: Application Automation
│   ├── pom.xml
│   └── src/
│       └── main/
│           └── java/
│               └── com/smbc/raft/app/
│                   ├── pages/                          # Page Objects
│                   │   ├── LoginPage.java
│                   │   └── HomePage.java
│                   ├── api/                            # API Clients
│                   │   └── UserApiClient.java
│                   ├── database/                       # Database Queries
│                   │   └── UserDatabaseQueries.java
│                   └── workflows/                      # Business Workflows
│                       └── UserWorkflow.java
│
└── app-tests/                          # Layer 3: Tests
    ├── pom.xml
    └── src/
        ├── test/
        │   ├── java/
        │   │   └── com/smbc/raft/tests/
        │   │       ├── ui/                             # UI Tests
        │   │       │   └── LoginUITest.java
        │   │       ├── api/                            # API Tests
        │   │       │   └── UserApiTest.java
        │   │       └── integration/                    # Integration Tests
        │   │           ├── UserDatabaseTest.java
        │   │           └── EndToEndIntegrationTest.java
        │   └── resources/
        │       ├── testng.xml                          # TestNG suite configuration
        │       └── testdata/                           # Test data files
        │           ├── users.json                      # (Example test data)
        │           └── test-data.xlsx                  # (Example test data)
        └── target/                                     # Generated artifacts (after build)
            ├── extent-reports/                         # HTML test reports
            ├── screenshots/                            # Test screenshots
            ├── videos/                                 # Test recordings
            ├── surefire-reports/                       # TestNG reports
            └── logs/                                   # Test execution logs
```

## File Count by Layer

### Layer 1 - Core Framework (8 files)
- ConfigManager.java
- DatabaseManager.java
- PlaywrightManager.java
- BasePage.java
- RestAssuredClient.java
- ExtentReportManager.java
- BaseTest.java
- DataUtils.java

### Layer 2 - Application Automation (5 files)
- LoginPage.java
- HomePage.java
- UserApiClient.java
- UserDatabaseQueries.java
- UserWorkflow.java

### Layer 3 - Tests (4 files)
- LoginUITest.java
- UserApiTest.java
- UserDatabaseTest.java
- EndToEndIntegrationTest.java

## Key Directories

- **src/main/java/** - Source code for framework and automation
- **src/test/java/** - Test implementations
- **src/main/resources/** - Configuration files
- **src/test/resources/** - Test resources and data
- **target/** - Build artifacts and reports (generated)

## Maven Modules

1. **test-automation-framework** (Parent)
   - Manages dependencies and versions
   - Defines build configuration

2. **core-framework** (Module 1)
   - Core testing utilities
   - Framework foundation

3. **app-automation** (Module 2)
   - Application-specific implementation
   - Depends on: core-framework

4. **app-tests** (Module 3)
   - Test execution
   - Depends on: core-framework, app-automation
