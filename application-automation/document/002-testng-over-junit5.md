# ADR-002: Use TestNG instead of JUnit 5 as the test runner

**Date:** 2024-01-15  
**Status:** Accepted  
**Deciders:** Framework Architecture Team

## Context

The framework needs a test runner to manage test lifecycle hooks, parallel execution,
data providers, and suite configuration. The two mature options for a Java project
are TestNG and JUnit 5.

Key requirements:

- **Suite-level XML configuration**: CI pipelines must be able to select a subset of
  tests (smoke, regression, nightly) without changing Java source files.
- **Fine-grained parallelism**: tests must run at method level with a configurable
  thread count, and data-driven test rows must also run in parallel.
- **Data providers**: parameterised tests must accept rich objects
  (`Map<String, String>`) not just flat primitive arrays.
- **Dependency chains**: certain integration tests have a strict execution order
  (`priority` and `dependsOnMethods`) — if step 1 fails, steps 2–4 should be skipped.
- **Cucumber integration**: BDD scenarios are run via `AbstractTestNGCucumberTests`;
  the runner class must integrate with the chosen test framework.

## Decision

Use **TestNG 7.9.0** as the test runner. JUnit 5 is not included in the framework.

## Options considered

### Option A — TestNG

**Pros**
- `testng.xml` suite files allow group-based test selection, thread count, and
  parallelism mode to be declared outside Java source — critical for the tiered suite
  strategy (smoke / regression / nightly).
- `parallel="methods"` with `thread-count` and `data-provider-thread-count` as
  separate attributes gives precise control over both method-level and
  data-driven parallelism.
- `@Test(dependsOnMethods=..., priority=...)` provides native ordered dependency
  chains needed by `UserEndToEndTest`.
- `@DataProvider` returns `Object[][]` and accepts a `Method` parameter, enabling the
  auto-discovery pattern in `TestDataProvider` (find the CSV/Excel file by test method
  name).
- `IRetryAnalyzer` interface supports retry logic without a third-party extension.
- Cucumber's `AbstractTestNGCucumberTests` is a first-class integration point.

**Cons**
- Less momentum in the Java community than JUnit 5 for unit tests; most new Java
  projects default to JUnit 5.
- Configuration lives in XML, not Java — some teams find this harder to refactor.
- Slightly more verbose annotation set than JUnit 5.

### Option B — JUnit 5

**Pros**
- Larger community; most Java testing tutorials use JUnit 5.
- Extension model (`@ExtendWith`) is more composable than TestNG listeners.
- `@ParameterizedTest` with `@MethodSource` is cleaner syntax than `@DataProvider`.
- Better IDE support (most IntelliJ/Eclipse shortcuts default to JUnit).

**Cons**
- No equivalent to `testng.xml` — suite selection requires either Maven Surefire
  category filtering (verbose) or a custom extension, both harder to maintain than
  a single XML file.
- `parallel` execution requires Surefire configuration; method-level parallelism and
  data-provider parallelism cannot be configured independently.
- `@Test(dependsOn=...)` has no JUnit 5 equivalent — ordered integration test chains
  must use `@TestMethodOrder(OrderAnnotation.class)` which does not support
  conditional skipping when an earlier step fails.
- Cucumber's JUnit 5 runner (`@Suite` + `@SelectClasspathResource`) is less mature
  than the TestNG equivalent.

## Consequences

**Positive**
- Suite files (`smoke.xml`, `regression.xml`, `nightly.xml`) cleanly express which
  groups run at each tier with no Java changes required.
- `data-provider-thread-count="6"` in the regression suite gives parallel data-driven
  rows for `DataDrivenUserApiTest` and `DataDrivenLoginTest` without any code changes.
- `TaggedCucumberRunner extends AbstractTestNGCucumberTests` integrates Cucumber
  scenarios into the same Allure and Extent Reports pipeline as all other tests.

**Negative / trade-offs**
- New team members from pure unit-testing backgrounds will be more familiar with JUnit
  5 idioms. The `ARCHITECTURE.md` and these ADRs exist to explain the rationale.
- If the team wants to add unit tests for utility classes in `core-framework`, JUnit 5
  would be the natural choice — running both frameworks simultaneously is possible
  but adds complexity. Current guidance: use TestNG throughout for consistency.

## References

- [TestNG documentation](https://testng.org/)
- `application-tests/src/test/resources/testng.xml`
- `application-tests/src/test/resources/smoke.xml`
- `core-framework/src/main/java/com/smbc/raft/core/data/TestDataProvider.java`
- `application-tests/src/test/java/com/smbc/raft/tests/bdd/runner/TaggedCucumberRunner.java`
