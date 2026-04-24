# ADR-001: Use Playwright instead of Selenium for UI automation

**Date:** 2024-01-15  
**Status:** Accepted  
**Deciders:** Framework Architecture Team

## Context

The framework needs a UI automation library to drive Chromium, Firefox, and WebKit
browsers. The team evaluated options at the point of starting the project.

Selenium WebDriver 4 was the incumbent choice — the team had prior experience with it.
Playwright 1.x had reached production-stable status and offered a substantially different
execution model.

Key requirements that drove the evaluation:

- **Parallel safety**: tests must run concurrently without shared browser state.
- **Reliability**: flaky tests caused by timing gaps between action and assertion were a
  significant pain point on the previous project.
- **Multi-browser**: Chromium, Firefox, and WebKit must all be supported without
  separate driver binaries managed outside the build.
- **Modern web support**: the target application makes heavy use of shadow DOM,
  iframes, and single-page navigation.
- **Headless CI performance**: tests must run headlessly in a Docker container without
  a display server.

## Decision

Use **Playwright 1.x** (currently `1.47.0`) as the sole UI automation library.
Selenium WebDriver is not included in the framework.

## Options considered

### Option A — Playwright

**Pros**
- Browser binaries are managed by the Playwright CLI and pinned to a specific version —
  no separate ChromeDriver / geckodriver version management.
- Auto-wait on every action: `click`, `fill`, `type` all wait for the element to be
  actionable before proceeding, eliminating most `WebDriverWait` boilerplate.
- `BrowserContext` isolation gives each test thread its own cookie jar, local storage,
  and network state without launching a new browser process — essential for
  `parallel="methods"` execution.
- `ThreadLocal<Page>` in `PlaywrightManager` maps cleanly onto TestNG's parallel
  execution model.
- Native shadow DOM piercing, CDP access, and network interception built in.
- Headless mode works without Xvfb on Linux — important for Docker-based CI.

**Cons**
- Smaller community and fewer StackOverflow answers than Selenium.
- Team had no existing Playwright experience.
- `Page` API is not a drop-in replacement for `WebDriver` — migration from existing
  Selenium tests requires rewriting page objects.

### Option B — Selenium WebDriver 4

**Pros**
- Mature ecosystem; team had existing experience.
- `WebDriver` interface is familiar; page objects from earlier projects could be reused.
- Larger community.

**Cons**
- Driver binary management (`WebDriverManager` or manual) is an ongoing maintenance
  burden and a frequent source of CI failures after browser auto-updates.
- Explicit waits must be written manually with `ExpectedConditions` — a significant
  source of flakiness when conditions are wrong or timing is tight.
- No built-in `BrowserContext` equivalent; parallel isolation requires one browser
  process per thread, which is significantly more memory-intensive.
- Shadow DOM, CDP, and network interception require additional libraries.

### Option C — Cypress

**Pros**
- Developer-friendly; excellent debugging experience.

**Cons**
- JavaScript/TypeScript only — incompatible with the Java stack chosen for REST Assured
  and database test layers.
- Cannot be called from the same JVM as TestNG, which would require a second test
  runner and split reporting.

## Consequences

**Positive**
- `PlaywrightManager` uses four `ThreadLocal` fields (`Playwright`, `Browser`,
  `BrowserContext`, `Page`) giving complete thread isolation at zero additional cost
  compared to Selenium's per-thread browser approach.
- Browser installation is a single `mvn exec:java ... install` command; no external
  driver management tool is needed.
- Baked into the Docker image as a one-time layer — no install step per pipeline run.
- The `ci.json` profile's `headless: true` works without any Linux display server
  configuration.

**Negative / trade-offs**
- Team members familiar only with Selenium must learn the Playwright API. The
  `BasePage` abstraction reduces this surface, but differences in locator syntax
  (CSS/text/role selectors vs. By.*) still require learning.
- If Playwright introduces a breaking API change between minor versions, the
  `PlaywrightManager` and `BasePage` must be updated. The version is pinned in the
  parent POM (`playwright.version=1.47.0`) to control this.

## References

- [Playwright Java documentation](https://playwright.dev/java/)
- `core-framework/src/main/java/com/automation/core/playwright/PlaywrightManager.java`
- `core-framework/src/main/java/com/automation/core/playwright/BasePage.java`
