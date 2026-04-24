# ADR-005: Environment configuration in JSON over .properties files

**Date:** 2024-05-20  
**Status:** Accepted  
**Deciders:** Framework Architecture Team

## Context

The framework supports multiple environments (dev, qa, uat, uat2, prod). Each
environment has a distinct set of databases, message queue brokers, application URLs,
and user credentials. The original `ConfigurationManager` loaded flat `.properties`
files (`qa.properties`, `uat.properties`) with dot-notation keys:

```properties
db.type=oracle
db.url=jdbc:oracle:thin:@qa-server:1521:QAXE
db.secondary.type=mssql
db.secondary.url=jdbc:sqlserver://...
app.url=https://qa.example.com
```

This model broke down under three pressures:

1. **Multiple named databases**: adding a third database (`db.tertiary.*`) required
   adding a new prefix convention with no schema enforcement.
2. **Multiple named websites with per-site users**: there was no clean way to express
   that `qa` has a `BBC` site with a `reader`, `editor`, and `admin` user each with
   distinct credentials, and a `Yahoo` site with a `guest` and `member` user —
   flat properties produced deeply nested keys that were error-prone to read and
   maintain.
3. **External config directory**: the `external-config` run profile mounts credentials
   from a secure volume outside the repository. `.properties` files do not have a
   natural directory-loading convention; JSON files do.

## Decision

Replace per-environment `.properties` files (for connection and URL configuration)
with **per-environment JSON files** loaded by `EnvironmentConfigLoader` and cached by
`EnvironmentContext`. The flat `ConfigurationManager` is retained for browser,
timeout, and reporting settings that are not environment-specific.

Environment JSON files live at:
`application-tests/src/test/resources/environments/<env>.json`

Or, when a run profile sets `environmentConfigDir`, at an external filesystem path.

## Options considered

### Option A — Structured JSON per environment (chosen)

**Pros**
- `EnvironmentConfig` is a typed Java POJO (`@Data`, `@JsonIgnoreProperties`) — the
  compiler catches missing fields; `ConfigurationManager.getProperty("db.url")` does
  not.
- Named databases, queues, and websites are first-class map keys:
  `config.getDatabases().get("contentDb")` is clearer than
  `getProperty("db.contentDb.url")`.
- Per-site user credentials (`bbc.users.reader`, `yahoo.users.guest`) are modelled
  as `Map<String, UserCredential>` — adding a new user is a JSON object, not a
  new key naming convention.
- Jackson's `@JsonIgnoreProperties(ignoreUnknown = true)` means new fields added by
  one team member do not break older consumers.
- External directory loading (`EnvironmentConfigLoader.loadFromDir`) is natural —
  the run profile simply sets `environmentConfigDir` to the mounted volume path.

**Cons**
- JSON is less familiar than `.properties` for configuration to some team members.
- Comments are not supported in standard JSON — rationale must be in the `description`
  field or in adjacent documentation.
- Requires Jackson on the classpath (already present via REST Assured).

### Option B — YAML per environment

**Pros**
- Supports comments; hierarchical structure similar to JSON.
- Common in Spring Boot projects — familiar to many Java developers.

**Cons**
- Requires SnakeYAML or Jackson YAML module — an additional dependency not otherwise
  needed.
- Indentation-sensitive — copy-paste errors are harder to spot than mismatched JSON
  braces.
- No benefit over JSON given the team had no existing YAML investment.

### Option C — Retain flat `.properties` with nested key conventions

**Pros**
- No new dependency or loader code required.
- `ConfigurationManager` is already in place.

**Cons**
- Multi-database and multi-site configuration becomes unmaintainable at scale — the
  `uat2` environment has two databases (`contentDb` MSSQL, `archiveDb` Oracle) and
  three message queues; flat keys for this require careful coordination.
- No type safety — `getProperty("db.pool.size")` returns `String`; a typo in the key
  returns `null` silently.
- Cannot support the external config directory use case cleanly.

## Consequences

**Positive**
- `EnvironmentContextTest` verifies the full config structure at test time:
  `config.getDatabases().containsKey("archiveDb")` catches missing config immediately
  rather than at first database call.
- The BDD step `theTestDataIsRelatedTo(key)` resolves
  `config.getCustomProperties().get(key)` — arbitrary application-specific properties
  live in the `customProperties` map without requiring new POJO fields.
- Run profiles can point to a directory outside the repository
  (`external-config.json` sets `environmentConfigDir: /opt/secure-configs/environments`)
  so production credentials are never committed to source control.

**Negative / trade-offs**
- Two configuration systems now exist side-by-side: `ConfigurationManager`
  (`.properties`, for browser/timeout/headless/reporting) and `EnvironmentContext`
  (JSON, for databases/URLs/users). New contributors must understand which system
  to use for a given setting. The rule of thumb: if the value changes per
  _environment_, it belongs in the JSON; if it changes per _run profile_, it belongs
  in the `.properties` file or `ci.json`.
- Adding a new top-level concept (e.g. an S3 bucket per environment) requires a
  new field in `EnvironmentConfig` and a corresponding POJO — more effort than
  adding a line to a `.properties` file.

## References

- `core-framework/src/main/java/com/smbc/raft/core/environment/EnvironmentConfig.java`
- `core-framework/src/main/java/com/smbc/raft/core/environment/EnvironmentContext.java`
- `core-framework/src/main/java/com/smbc/raft/core/environment/EnvironmentConfigLoader.java`
- `application-tests/src/test/resources/environments/qa.json`
- `application-tests/src/test/resources/profiles/external-config.json`
