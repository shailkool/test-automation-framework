# ADR-003: Three-layer Maven module architecture

**Date:** 2024-01-15  
**Status:** Accepted  
**Deciders:** Framework Architecture Team

## Context

The framework must support multiple applications being tested by the same underlying
infrastructure. Without architectural separation it is common for test projects to
accumulate:

- Application-specific selectors and API URLs mixed into reusable utilities.
- Database helpers that hard-code table names from one application.
- Tests that reach directly into `PlaywrightManager` and `DatabaseManager` instead of
  going through a stable abstraction.

This makes it difficult to onboard a second application, upgrade a core dependency
(e.g. Playwright minor version), or enforce that test authors do not bypass the
framework layer.

## Decision

Structure the project as **three Maven modules with enforced dependency direction**:

```
core-framework          ← no dependency on application code
      ↑
application-automation  ← depends on core-framework only
      ↑
application-tests       ← depends on both layers; contains only test classes
```

No module is permitted to depend on a module above it in this hierarchy.

## Options considered

### Option A — Three-layer Maven multi-module (chosen)

**Pros**
- Maven's dependency graph enforces the layering at compile time — `core-framework`
  cannot accidentally import a class from `application-automation`.
- Each module has a clean, single responsibility:
  - `core-framework`: browser, API client, DB pooling, config, reporting — technology.
  - `application-automation`: page objects, API clients, DB helpers — application knowledge.
  - `application-tests`: test classes and suite XML — assertions and scenarios.
- A second application can add a new `application-automation` module pointing at the
  same `core-framework` without touching the existing module.
- `core-framework` can be built and published to a Maven repository independently,
  allowing multiple teams to share the same version of the foundation layer.

**Cons**
- More POM files to maintain (parent + 3 child POMs).
- New contributors must understand the module boundary rules before writing their
  first test.
- Running a single test class from the IDE requires the correct module to be selected.

### Option B — Single Maven module, package-based separation

**Pros**
- One POM file; simpler project structure.
- Any class can be run directly without navigating module boundaries.

**Cons**
- No compile-time enforcement of layering; a test class can import directly from
  `PlaywrightManager` and bypass `BasePage`, or a page object can reference a
  test-only utility class.
- Dependency upgrades require careful audit of the entire classpath — there is no
  way to say "only `core-framework` depends on Playwright."
- Cannot extract the core layer into a shared library later without significant
  refactoring.

### Option C — Separate Git repositories per layer

**Pros**
- Hard boundary — a different repository literally cannot import code from the other.
- Independent versioning and release cycles.

**Cons**
- Multi-repo development workflow is significantly more friction for a test automation
  project — changing a page object and the test that uses it requires two PRs.
- Local development requires building and publishing the core artifact before changing
  the application layer; `mvn clean install` no longer builds everything.
- Overkill for a team where all three layers are owned by the same group.

## Consequences

**Positive**
- The parent POM's `<dependencyManagement>` block pins all version numbers in one
  place; child modules declare dependencies without versions, eliminating
  version drift.
- `core-framework` can be compiled and tested independently:
  `mvn test -pl core-framework` builds just the foundation layer.
- The separation is visible in the project structure and reinforced by the
  ARCHITECTURE.md documentation, making it self-describing to new joiners.

**Negative / trade-offs**
- Three `pom.xml` files plus the parent must be kept in sync when adding a new
  dependency. The parent `<dependencyManagement>` block must always be the canonical
  source of truth for versions.
- IDE navigation sometimes requires "project" awareness — IntelliJ's multi-module
  support handles this well, but developers using editors without Maven support may
  find cross-module navigation less smooth.

## References

- `pom.xml` (parent)
- `core-framework/pom.xml`
- `application-automation/pom.xml`
- `application-tests/pom.xml`
- `ARCHITECTURE.md`
