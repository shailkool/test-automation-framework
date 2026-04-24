# Test Data Management Guide

## Overview

The framework uses a **Centralised Test Data Factory** pattern to manage the creation and automatic cleanup of test data. This ensures that tests are isolated, thread-safe for parallel execution, and do not leave orphaned data in the database or external systems.

## Core Components

### 1. TestDataRegistry (core-framework)
A thread-safe registry that tracks all test data created during a single test method execution. It stores "teardown actions" (Runnables) in a `ThreadLocal` LIFO (Last-In-First-Out) stack.

### 2. TestDataFactory (application-automation)
The single entry point for all test data creation. It provides static methods to get domain-specific fixtures.

### 3. Domain Fixtures (application-automation)
Fluent builders (e.g., `UserFixture`) that handle the logic of creating data via API or Database and automatically register cleanup actions.

---

## How to Use

### 1. Basic Creation
Creating a user with default settings. Cleanup is automatic.

```java
@Test
public void testUserCreation() {
    // Create a user with default values
    UserFixture user = TestDataFactory.user().create();
    
    // Access created data
    System.out.println("User ID: " + user.getId());
    System.out.println("Email: " + user.getEmail());
}
```

### 2. Fluent Configuration
Customizing the data before creation.

```java
@Test
public void testAdminPermissions() {
    UserFixture admin = TestDataFactory.user()
        .withName("Admin User")
        .withRole("admin")
        .withEmail("custom-admin@example.com")
        .create();
    
    // Test logic here...
}
```

### 3. Database Creation
For tests that need data directly in the database (bypassing the API).

```java
@Test
public void testDirectDbValidation() {
    UserFixture dbUser = TestDataFactory.user()
        .createInDb();
    
    // Test logic here...
}
```

---

## Why Use the Factory?

1.  **Automatic Cleanup**: You don't need to write `@AfterMethod` cleanup code. The `BaseTest` calls `TestDataRegistry.cleanup()` automatically.
2.  **LIFO Teardown**: Data is deleted in reverse order of creation. This respects foreign key constraints (e.g., an Order is deleted before the User who placed it).
3.  **Parallel Safety**: Uses `UUID` for unique identifiers and `ThreadLocal` for storage, preventing collisions when running 16+ threads.
4.  **Crash Resilience**: If a test fails or crashes mid-way, the registry still attempts to clean up everything created up to that point.

---

## Adding New Fixtures

To add a new data type (e.g., `OrderFixture`):

1.  **Create the Fixture Class**:
    ```java
    public class OrderFixture {
        private int orderId;
        private int userId;
        
        public OrderFixture forUser(UserFixture user) {
            this.userId = user.getId();
            return this;
        }
        
        public OrderFixture create() {
            // Logic to create order via API
            Response r = api.createOrder(userId);
            this.orderId = r.jsonPath().getInt("id");
            
            // REGISTER CLEANUP
            TestDataRegistry.register(() -> api.deleteOrder(orderId));
            return this;
        }
    }
    ```

2.  **Add to TestDataFactory**:
    ```java
    public static OrderFixture order() {
        return new OrderFixture(API, DB);
    }
    ```

3.  **Use in Tests**:
    ```java
    UserFixture user = TestDataFactory.user().create();
    OrderFixture order = TestDataFactory.order().forUser(user).create();
    ```

---

## Best Practices

*   **Don't Manual Cleanup**: Avoid calling `deleteUser` directly in your test if you used the Factory to create it.
*   **Use UUIDs**: The Factory uses `UUID` by default for emails/names to ensure uniqueness in parallel runs. Avoid using `System.currentTimeMillis()` as it can collide in high-concurrency environments.
*   **Chain Fixtures**: Use one fixture to provide data to another (like the `forUser` example above) to maintain relational integrity.
