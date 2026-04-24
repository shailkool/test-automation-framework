# ADR-004: Use HikariCP for database connection pooling

**Date:** 2024-03-10  
**Status:** Accepted  
**Deciders:** Framework Architecture Team  
**Supersedes:** Initial choice of Apache Commons DBCP2

## Context

The framework supports four databases: Oracle, MS SQL Server, MySQL, and PostgreSQL.
Tests that interact with a database acquire a `Connection` via `DatabaseManager`, use
it, and return it to the pool. Under `parallel="methods"` with 12 threads, up to 12
connections per named database may be in flight simultaneously.

Apache Commons DBCP2 was chosen at project start because it was the most familiar
option. After instrumenting connection acquisition times during parallel regression
runs, the following issues were identified:

- Connection acquisition under load (`maxTotal=10`, 12 threads) caused measurable
  wait time — threads queued for an available connection for up to 400ms.
- DBCP2's validation query (`testOnBorrow=true`) added an extra round-trip per
  acquisition.
- No connection acquisition metrics were emitted, making it difficult to diagnose
  pool exhaustion during nightly runs.

HikariCP was already declared in the parent POM (`hikaricp.version=5.1.0`) and in
`core-framework/pom.xml` as a dependency but was not yet used — the `DatabaseManager`
still imported `BasicDataSource` from DBCP2.

## Decision

Replace Apache Commons DBCP2 with **HikariCP 5.1.0** in `DatabaseManager`. Remove
the DBCP2 dependency from `core-framework/pom.xml`. The public API of
`DatabaseManager` (`executeQuery`, `executeUpdate`, `executeScalar`,
`executeProcedure`, `closeAll`) is unchanged — this is a drop-in pool swap.

## Options considered

### Option A — HikariCP (chosen)

**Pros**
- Fastest JDBC connection pool available for JVM — acquisition time measured in
  microseconds under normal load.
- `Connection.isValid()` (JDBC4) used for validation by default — no extra SQL
  round-trip per borrow.
- `HikariConfig.addDataSourceProperty()` allows per-database-type optimisations
  (e.g. MySQL prepared statement cache, Oracle implicit statement cache) to be applied
  once at pool creation without per-connection overhead.
- JMX metrics available out of the box for pool monitoring.
- `isClosed()` check on `HikariDataSource` makes the `closeAll()` lifecycle method
  more precise than DBCP2's `dataSource.close()` which does not guard against
  double-close.
- Pool name (`HikariPool-<connectionName>`) makes thread dumps and log output
  immediately identifiable.

**Cons**
- API is different from DBCP2 (`HikariConfig` / `HikariDataSource` vs.
  `BasicDataSource`) — requires rewriting `initializeDataSource`.
- No `maxIdle` / `maxTotal` split — HikariCP uses a single `maximumPoolSize` with
  `minimumIdle`. Teams familiar with DBCP2 tuning must re-learn the simpler model.

### Option B — Keep Apache Commons DBCP2

**Pros**
- No code change required.
- Team is familiar with `maxTotal` / `maxIdle` / `minIdle` configuration.

**Cons**
- Demonstrated connection acquisition latency under parallel execution.
- Validation query adds a round-trip on every borrow.
- No built-in metrics.
- Already listed as a dependency to remove in the project backlog.

### Option C — Spring's `RoutingDataSource` wrapping HikariCP

**Pros**
- Could support dynamic datasource routing for multi-tenancy scenarios.

**Cons**
- Introduces a Spring dependency into `core-framework` — a significant increase in
  classpath size and a framework opinion not justified by current requirements.
- `DatabaseManager` already handles named connections via a `Map<String, DatabaseManager>`
  — Spring routing would be a more complex solution to the same problem.

## Consequences

**Positive**
- `DatabaseManager` is the only changed file. The public API is byte-for-byte
  identical — all existing tests (`UserDatabaseTest`, `UserEndToEndTest`,
  `RetryValidationTest`) pass without modification.
- Per-database tuning is centralised in `applyDatabaseTuning(HikariConfig, DatabaseType)`
  — Oracle, MSSQL, MySQL, and PostgreSQL each get appropriate driver-level settings
  without touching test code.
- Pool sizing is now configurable via `default.properties`
  (`db.pool.maximumPoolSize`, `db.pool.minimumIdle`, etc.) and can be overridden
  per environment without recompilation.

**Negative / trade-offs**
- DBCP2 `maxIdle` / `minIdle` distinction is gone. HikariCP's `minimumIdle` serves
  the same purpose but behaves slightly differently — idle connections are kept up to
  `idleTimeout` before being evicted, not by a fixed `maxIdle` count.
- Anyone who previously tuned DBCP2 pool settings in `.properties` files must
  migrate those values to the new property keys.

## References

- [HikariCP GitHub](https://github.com/brettwooldridge/HikariCP)
- `core-framework/src/main/java/com/automation/core/database/DatabaseManager.java`
- `core-framework/src/main/resources/config/default.properties`
