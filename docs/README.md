# Architecture Decision Records

This directory contains the Architecture Decision Records (ADRs) for the Test Automation Framework.

An ADR captures a significant architectural decision made during the project — the context that
drove it, the options considered, the choice made, and the consequences of that choice. They are
written once and amended only when a decision is formally revisited and superseded.

## What is an ADR?

An ADR is a short document (usually one page) that answers:
- **What** decision was made?
- **Why** was it made — what forces, constraints, or requirements drove it?
- **What alternatives** were considered and rejected?
- **What are the consequences** — good and bad — of the choice?

## Status lifecycle

| Status | Meaning |
|--------|---------|
| `Proposed` | Under discussion, not yet adopted |
| `Accepted` | In force — this is the current decision |
| `Deprecated` | No longer recommended but not yet replaced |
| `Superseded by ADR-NNN` | Replaced by a later decision |

## Index

| ADR | Title | Status |
|-----|-------|--------|
| [ADR-001](001-playwright-over-selenium.md) | Use Playwright instead of Selenium for UI automation | Accepted |
| [ADR-002](002-testng-over-junit5.md) | Use TestNG instead of JUnit 5 as the test runner | Accepted |
| [ADR-003](003-three-layer-architecture.md) | Three-layer Maven module architecture | Accepted |
| [ADR-004](004-hikaricp-connection-pool.md) | Use HikariCP for database connection pooling | Accepted |
| [ADR-005](005-environment-json-over-properties.md) | Environment configuration in JSON over .properties files | Accepted |
| [ADR-006](006-bdd-for-cross-layer-scenarios.md) | Use Cucumber BDD for cross-layer integration scenarios | Accepted |

## How to add a new ADR

1. Copy the template below into a new file named `NNN-short-title.md` where `NNN` is the next
   number in sequence.
2. Fill in every section — do not leave placeholders.
3. Add a row to the index table above.
4. Open a PR. The ADR is `Proposed` until the PR is merged, at which point it becomes `Accepted`.

## Template

```markdown
# ADR-NNN: Title

**Date:** YYYY-MM-DD  
**Status:** Proposed | Accepted | Deprecated | Superseded by ADR-NNN  
**Deciders:** [names or roles]

## Context

[Describe the situation, problem, or requirement that forced a decision.
Include relevant constraints: team size, existing infrastructure, performance
requirements, deadline pressures, etc.]

## Decision

[State the decision clearly in one or two sentences.]

## Options considered

### Option A — [name]
[Describe it. Pros. Cons.]

### Option B — [name]
[Describe it. Pros. Cons.]

## Consequences

**Positive**
- ...

**Negative / trade-offs**
- ...

## References
- [Link or document]
```
