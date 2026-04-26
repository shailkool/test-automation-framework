# Security Hardening & Secret Management

The RAFT framework is designed with enterprise security in mind, ensuring that sensitive credentials never reside in plain text within the repository and are protected during execution.

## 1. Secret Management & Placeholder Resolution

The `ConfigurationManager` automatically resolves placeholders in your property files or JSON configurations. This allows you to decouple sensitive data from your source code.

### Usage
In your `default.properties` or environment-specific files (e.g., `qa.properties`):
```properties
db.username=test_user
# Decoupled password - resolved at runtime
db.password=${DB_PASSWORD}
# Explicit ENV prefix (optional)
api.key=${ENV:API_TOKEN}
```

### Resolution Priority
1.  **Environment Variables**: Checked first (e.g., injected via GitHub Secrets, Jenkins, or Docker).
2.  **System Properties**: Checked second (e.g., passed via `-Dvar=val`).
3.  **Default Value**: If neither is found, the placeholder remains un-resolved (e.g., `${DB_PASSWORD}`).

## 2. Sensitive Data Masking

To prevent "accidental leakage" of credentials into logs or report artifacts, the framework implements automatic masking:

- **Database URLs**: The `DatabaseManager` masks JDBC connection strings before logging them or including them in exception messages.
  - *Raw*: `jdbc:oracle:thin:user/password@host:port:sid`
  - *Log Output*: `jdbc:oracle:thin:user/********@host:port:sid`
- **Protected toString()**: Core managers like `DatabaseManager` override `toString()` to return only non-sensitive metadata.

## 3. SSL/TLS Configuration

When testing against internal APIs or corporate proxies, you may encounter self-signed certificates or require custom trust anchors.

### Relaxed HTTPS Validation
Use this for early-stage testing against development environments with self-signed certs:
```java
new RestAssuredClient()
    .setRelaxedHTTPSValidation()
    .get("/api/endpoint");
```

### Custom TrustStores (mTLS / Certificate Pinning)
For secure enterprise endpoints, you can provide a custom TrustStore:
```java
new RestAssuredClient()
    .setTrustStore("path/to/truststore.jks", "changeit")
    .post("/api/secure", body);
```

## 4. Best Practices for Enterprise Teams

1.  **Never commit plain-text passwords**: Always use the `${VAR}` pattern for any credential.
2.  **Utilize .gitignore**: Ensure that local developer property files (like `local.properties`) are never committed.
3.  **CI/CD Secret Injection**: Use your CI tool's native secret store to inject environment variables into the test runner container.
4.  **Least Privilege**: Ensure the database and API credentials used for automation have the minimum necessary permissions for the tests.
