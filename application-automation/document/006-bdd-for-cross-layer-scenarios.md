# ADR-006: Use Cucumber BDD for cross-layer integration scenarios

**Date:** 2024-07-01  
**Status:** Accepted  
**Deciders:** Framework Architecture Team

## Context

The framework already has three test tiers: pure API tests, pure database tests, and
integration tests (`UserEndToEndTest`) that combine all three layers. A fourth need
emerged: **cross-environment, cross-site scenarios** that non-technical stakeholders
must be able to read and verify — for example, "a BBC reader navigates to the news
section, then the journey is validated against the QA environment's configured BBC
base URL."

There was also a requirement to test the CSV filtering engine (`CsvFilterEngine`) with
scenario tables that business analysts author directly — the test data and expected
outputs are defined in Gherkin `DataTable` blocks, not in code.

Two questions needed answering:
1. Should BDD (Gherkin / Cucumber) be adopted at all, or should plain TestNG tests
   suffice?
2. If BDD is adopted, how should it integrate with the existing TestNG runner?

## Decision

Adopt **Cucumber 7.x with `cucumber-testng`** for scenarios that:
- Combine multiple layers (UI + environment config, or filter engine + CSV data), **and**
- Have readable acceptance criteria that a business analyst or QA lead can verify
  without reading Java source.

Plain TestNG tests remain the default for unit-style API, database, and UI tests.
BDD is not mandated for all tests — it is an additional layer where it provides
genuine communication value.

The runner class (`TaggedCucumberRunner`) extends `AbstractTestNGCucumberTests` so
BDD scenarios participate in the same Allure and Extent Reports pipeline as all
other tests. Scenarios are tagged (`@smoke`, `@regression`, `@wip`) and the active
tag expression is injected via `-Dcucumber.filter.tags`.

## Options considered

### Option A — Cucumber with TestNG integration (chosen)

**Pros**
- `AbstractTestNGCucumberTests` makes Cucumber scenarios first-class TestNG tests —
  they appear in Allure reports, trigger the same `@BeforeMethod`/`@AfterMethod`
  hooks in `BaseTest`, and can be included in suite XML files.
- Tag expressions (`@smoke and not @wip`) are equivalent to TestNG group filters —
  the same smoke/regression/nightly tiering applies to BDD scenarios.
- `SiteNavigationSteps` uses `EnvironmentContext.get()` and `RunProfileContext` —
  the same config infrastructure as all other tests; no parallel config system.
- `DynamicDataResolver` uses `ThreadLocal` storage, making it safe under
  `parallel="methods"` just like `PlaywrightManager`.
- Business analysts can read and contribute to `.feature` files without Java
  knowledge.
- `masterthought` (`cucumber-reporting`) generates a rich BDD-specific HTML report
  from `cucumber.json` alongside the Allure report.

**Cons**
- Two test authoring styles in the same project (Gherkin + Java `@Test`) can confuse
  new contributors about which to use for a given scenario.
- Step definitions (`SiteNavigationSteps`, `CsvFilterSteps`) add a layer of
  indirection — debugging a failing scenario requires tracing from the `.feature`
  file through the step definition to the underlying page object or engine.
- `TaggedCucumberRunner` must be kept in sync with new feature directories and glue
  packages.

### Option B — Plain TestNG parameterised tests for all scenarios

**Pros**
- Single test authoring style throughout.
- No additional dependency (`cucumber-java`, `cucumber-testng`, `cucumber-reporting`).
- Step definitions are unnecessary — test logic is directly in Java.

**Cons**
- Cross-environment navigation scenarios expressed as `@Test` methods with multi-line
  assertions are not readable by non-technical stakeholders.
- CSV filter test cases expressed as `DataTable`-style `Object[][]` arrays in a
  `@DataProvider` are harder to maintain than Gherkin tables.
- No executable specification that can be shared with business analysts for sign-off.

### Option C — Serenity BDD

**Pros**
- Rich narrative reports; designed for team communication.

**Cons**
- Serenity wraps TestNG in its own runner, which conflicts with the
  `AbstractTestNGCucumberTests` lifecycle management already in place.
- Requires replacing `ExtentReportManager` and `AllureTestNg` with Serenity's own
  reporting stack — a significant disruption to an already working pipeline.
- Much heavier dependency tree.

## Consequences

**Positive**
- The `site_navigation.feature` file expresses multi-site, multi-environment journeys
  in plain English — QA leads can verify the acceptance criteria without reading
  `SiteNavigationSteps.java`.
- `@smoke @bbc` and `@smoke @yahoo` tags map directly to the smoke suite's
  `cucumber.filter.tags=@smoke` — no test code changes required to include or
  exclude BDD scenarios from a pipeline tier.
- `CsvFilterSteps` step definitions allow the data and expected outputs for the
  filter engine to live in `.feature` files as `DataTable` blocks — business analysts
  can add new test cases without touching Java.
- `TaggedCucumberRunner` emits `cucumber.json`, consumed by
  `MasterthoughtReportGenerator` (registered as a shutdown hook) to produce a
  feature-oriented HTML report separate from the Allure/Extent reports.

**Negative / trade-offs**
- The rule "use BDD only where it provides communication value" must be enforced by
  convention, not by tooling. Without discipline, teams will write BDD tests for
  low-level API checks where plain TestNG is more appropriate.
- Each new feature area requires a new step definitions class in
  `com.automation.tests.bdd.steps` and a corresponding `glue` package entry in
  `TaggedCucumberRunner`. Forgetting to update `glue` causes
  `UndefinedStepException` at runtime.
- The Masterthought report is generated as a JVM shutdown hook — if the JVM is
  killed (e.g. by an OOM or Surefire timeout), the report will not be produced.
  The raw `cucumber.json` is always written and can be converted separately.

## References

- [Cucumber-JVM documentation](https://cucumber.io/docs/installation/java/)
- `application-tests/src/test/java/com/automation/tests/bdd/runner/TaggedCucumberRunner.java`
- `application-tests/src/test/java/com/automation/tests/bdd/steps/SiteNavigationSteps.java`
- `application-tests/src/test/java/com/automation/tests/bdd/steps/CsvFilterSteps.java`
- `application-tests/src/test/resources/features/site_navigation.feature`
