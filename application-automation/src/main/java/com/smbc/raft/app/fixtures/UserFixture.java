package com.smbc.raft.app.fixtures;

import com.smbc.raft.app.api.UserApiClient;
import com.smbc.raft.app.database.UserDatabaseHelper;
import com.smbc.raft.core.data.TestDataRegistry;
import io.restassured.response.Response;
import java.util.UUID;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;

/**
 * Fixture for creating User test data. Use via TestDataFactory — don't instantiate directly.
 *
 * <p>Example: UserFixture user = TestDataFactory.user().withRole("admin").create(); // use
 * user.getId(), user.getEmail() etc. // cleanup happens automatically in @AfterMethod via
 * TestDataRegistry
 */
@Log4j2
@Getter
public class UserFixture {

  // Created record fields
  private int id;
  private String name;
  private String email;
  private String role;

  // Builder fields — set before create()
  private String desiredName = "Test User";
  private String desiredRole = "user";
  private String desiredEmail = null; // null = auto-generate

  private final UserApiClient api;
  private final UserDatabaseHelper db;

  UserFixture(UserApiClient api, UserDatabaseHelper db) {
    this.api = api;
    this.db = db;
  }

  public UserFixture withName(String name) {
    this.desiredName = name;
    return this;
  }

  public UserFixture withRole(String role) {
    this.desiredRole = role;
    return this;
  }

  public UserFixture withEmail(String email) {
    this.desiredEmail = email;
    return this;
  }

  /** Creates the user via the API and registers automatic cleanup. */
  public UserFixture create() {
    this.email =
        desiredEmail != null
            ? desiredEmail
            : "test_" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
    this.name = desiredName;
    this.role = desiredRole;

    Response response = api.createUser(name, email, role);
    if (response.getStatusCode() != 201) {
      throw new RuntimeException("Failed to create user fixture: " + response.body().asString());
    }

    this.id = response.jsonPath().getInt("id");
    log.debug("UserFixture created: id={}, email={}", id, email);

    // Register cleanup — runs automatically at end of test
    int currentId = this.id;
    TestDataRegistry.register(
        () -> {
          try {
            api.deleteUser(currentId);
            log.debug("UserFixture deleted: id={}", currentId);
          } catch (Exception e) {
            log.warn("Could not delete UserFixture id={}", currentId, e);
          }
        });

    return this;
  }

  /**
   * Creates the user directly in the DB (useful for DB-layer tests where the API isn't the system
   * under test).
   */
  public UserFixture createInDb() {
    this.email =
        desiredEmail != null
            ? desiredEmail
            : "test_" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
    this.name = desiredName;
    this.role = desiredRole;

    db.createUser(name, email, "hashed_test_pw", role);
    log.debug("UserFixture created in DB: email={}", email);

    String currentEmail = this.email;
    TestDataRegistry.register(
        () -> {
          db.deleteUserByEmail(currentEmail);
          log.debug("UserFixture deleted from DB: email={}", currentEmail);
        });

    return this;
  }
}
