package com.automation.app.fixtures;

import com.automation.app.api.UserApiClient;
import com.automation.app.database.UserDatabaseHelper;

/**
 * Central entry point for all test data creation.
 *
 * Usage:
 *   UserFixture user  = TestDataFactory.user().withRole("admin").create();
 *   UserFixture admin = TestDataFactory.user().withName("Admin User").withRole("admin").create();
 */
public class TestDataFactory {

    private static final UserApiClient API = new UserApiClient();
    private static final UserDatabaseHelper DB  = new UserDatabaseHelper();

    private TestDataFactory() {}

    public static UserFixture user() {
        return new UserFixture(API, DB);
    }

    // Add more fixture types here as your domain grows:
    // public static OrderFixture order()   { return new OrderFixture(API, DB); }
    // public static ProductFixture product() { return new ProductFixture(API, DB); }
}
