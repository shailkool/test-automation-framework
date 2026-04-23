package com.automation.tests.bdd.steps;

import com.automation.core.environment.EnvironmentContext;
import com.automation.core.environment.UserCredential;
import com.automation.core.environment.WebsiteSettings;
import com.automation.core.playwright.PlaywrightManager;
import com.automation.core.runprofile.RunProfile;
import com.automation.core.runprofile.RunProfileContext;
import com.automation.core.runprofile.ScreenshotMode;
import com.microsoft.playwright.Page;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.extern.log4j.Log4j2;
import org.testng.Assert;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Glue for {@code site_navigation.feature}.
 *
 * <p>Uses only agnostic core components: {@link EnvironmentContext} to pull
 * per-environment website/user settings and {@link PlaywrightManager} to drive
 * the browser. Nothing in here knows that "BBC" or "Yahoo" are special — the
 * site keys come from the currently active environment JSON.
 */
@Log4j2
public class SiteNavigationSteps {

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS");

    private Scenario scenario;
    private EnvironmentContext environment;
    private ScreenshotMode screenshotMode = ScreenshotMode.ON_FAILURE;
    private WebsiteSettings activeSite;
    private String activeSiteName;
    private UserCredential activeUser;
    private final List<String> journey = new ArrayList<>();

    @Before("@navigation")
    public void beforeNavigationScenario(Scenario scenario) {
        this.scenario = scenario;
        this.journey.clear();

        this.environment = EnvironmentContext.getInstance();
        RunProfile profile = RunProfileContext.getInstance().getProfile();
        this.screenshotMode = profile.resolveScreenshotMode();

        String banner = String.format(
            "Active environment : %s%n"
                + "Active run profile : %s (browser=%s, channel=%s, headless=%s)%n"
                + "Screenshot mode    : %s%n"
                + "Available websites : %s",
            environment.getEnvironmentName(),
            profile.getName(),
            profile.resolveBrowserEngine(),
            profile.resolveBrowserChannel(),
            profile.isHeadless(),
            screenshotMode,
            environment.getWebsites().keySet());

        log.info("\n{}", banner);
        scenario.log(banner);
    }

    @After("@navigation")
    public void afterNavigationScenario() {
        try {
            if (scenario != null && scenario.isFailed() && screenshotMode != ScreenshotMode.OFF) {
                captureScreenshot("failure");
            }
        } finally {
            try {
                PlaywrightManager.closeBrowser();
            } catch (RuntimeException e) {
                log.warn("Browser cleanup failed: {}", e.getMessage());
            }
        }
    }

    @Given("a browser session for the {string} website")
    public void aBrowserSessionForTheWebsite(String siteName) {
        openSession(siteName, null);
    }

    @Given("a browser session for the {string} website as user {string}")
    public void aBrowserSessionForTheWebsiteAsUser(String siteName, String userKey) {
        openSession(siteName, userKey);
    }

    @And("I switch the browser session to the {string} website as user {string}")
    public void iSwitchTheBrowserSessionToTheWebsiteAsUser(String siteName, String userKey) {
        this.activeSite = environment.getWebsite(siteName);
        this.activeSiteName = siteName;
        this.activeUser = environment.getUser(siteName, userKey);
        log.info("Switched context to '{}' (base={}, user={})",
            siteName, activeSite.getBaseUrl(), activeUser.getUsername());
    }

    @When("I open the BBC home page")
    public void iOpenTheBbcHomePage() {
        openLandingPage("BBC");
    }

    @When("I open the Yahoo home page")
    public void iOpenTheYahooHomePage() {
        openLandingPage("Yahoo");
    }

    @And("I navigate to the {string} section")
    public void iNavigateToTheSection(String pathPropertyKey) {
        requireActiveSite();
        Object pathValue = activeSite.getProperties().get(pathPropertyKey);
        Assert.assertNotNull(pathValue, String.format(
            "Website '%s' in environment '%s' has no property '%s' to navigate to",
            activeSiteName, environment.getEnvironmentName(), pathPropertyKey));

        String absoluteUrl = joinUrl(activeSite.getBaseUrl(), String.valueOf(pathValue));
        visit(absoluteUrl);
    }

    @Then("the navigation journey should contain {int} steps")
    public void theNavigationJourneyShouldContainSteps(Integer expected) {
        Assert.assertEquals(
            journey.size(),
            expected.intValue(),
            String.format("Expected %d journey steps, got %d: %s",
                expected, journey.size(), journey));
    }

    @And("every visited page should belong to the {string} base URL")
    public void everyVisitedPageShouldBelongToTheBaseUrl(String siteName) {
        String expectedHost = hostOf(environment.getWebsite(siteName).getBaseUrl());
        for (String visited : journey) {
            String actualHost = hostOf(visited);
            Assert.assertTrue(actualHost.endsWith(stripWww(expectedHost))
                    || stripWww(actualHost).endsWith(stripWww(expectedHost)),
                String.format("Visited URL '%s' is not under '%s'", visited, expectedHost));
        }
    }

    @And("the journey should include a page under the {string} base URL")
    public void theJourneyShouldIncludeAPageUnderTheBaseUrl(String siteName) {
        String expectedHost = stripWww(hostOf(environment.getWebsite(siteName).getBaseUrl()));
        boolean hit = journey.stream()
            .anyMatch(u -> stripWww(hostOf(u)).endsWith(expectedHost));
        Assert.assertTrue(hit,
            String.format("Journey %s contains no page under '%s'", journey, expectedHost));
    }

    private void openSession(String siteName, String userKey) {
        this.activeSite = environment.getWebsite(siteName);
        this.activeSiteName = siteName;
        this.activeUser = userKey == null ? null : environment.getUser(siteName, userKey);

        RunProfile profile = RunProfileContext.getInstance().getProfile();
        PlaywrightManager.initializeBrowser(
            profile.resolveBrowserEngine(),
            profile.resolveBrowserChannel());

        log.info("Opened browser session for '{}' (base={}, user={}, browser={}, channel={})",
            siteName, activeSite.getBaseUrl(),
            activeUser == null ? "<anonymous>" : activeUser.getUsername(),
            profile.resolveBrowserEngine(), profile.resolveBrowserChannel());
    }

    private void openLandingPage(String expectedSiteName) {
        requireActiveSite();
        Assert.assertEquals(activeSiteName, expectedSiteName,
            "Active browser session is on '" + activeSiteName
                + "' but the step expects '" + expectedSiteName + "'");

        Object landing = activeSite.getProperties().getOrDefault("defaultLandingPath", "/");
        String url = joinUrl(activeSite.getBaseUrl(), String.valueOf(landing));
        visit(url);
    }

    private void visit(String url) {
        Page page = PlaywrightManager.getPage();
        page.navigate(url);
        String landed = page.url();
        journey.add(landed);
        log.info("Visited {} (step {} of journey)", landed, journey.size());
        if (scenario != null) {
            scenario.log("Visited: " + landed);
        }
        if (screenshotMode == ScreenshotMode.EACH_STEP) {
            captureScreenshot("step-" + journey.size());
        }
    }

    /**
     * Take a PNG via Playwright, attach it to the current Cucumber scenario,
     * and persist it under the run profile's screenshots directory. Failures
     * are swallowed with a warning so screenshot issues never mask the real
     * test outcome.
     */
    private void captureScreenshot(String label) {
        try {
            byte[] png = PlaywrightManager.takeScreenshot();
            if (png == null || png.length == 0) {
                return;
            }
            String name = sanitize(scenarioName()) + "_" + label + "_"
                + LocalDateTime.now().format(TS) + ".png";
            if (scenario != null) {
                scenario.attach(png, "image/png", name);
            }
            Path dir = RunProfileContext.getInstance().getScreenshotsDir();
            Files.createDirectories(dir);
            Path target = dir.resolve(name);
            Files.write(target, png);
            log.info("Screenshot '{}' saved to {}", label, target.toAbsolutePath());
        } catch (IOException | RuntimeException e) {
            log.warn("Screenshot capture failed ({}): {}", label, e.getMessage());
        }
    }

    private String scenarioName() {
        if (scenario == null || scenario.getName() == null || scenario.getName().isBlank()) {
            return "navigation";
        }
        return scenario.getName();
    }

    private static String sanitize(String value) {
        return value.replaceAll("[^A-Za-z0-9._-]+", "_");
    }

    private void requireActiveSite() {
        Objects.requireNonNull(activeSite,
            "No active website selected - open a browser session first");
    }

    private static String joinUrl(String baseUrl, String path) {
        if (path == null || path.isBlank() || "/".equals(path)) {
            return baseUrl;
        }
        String trimmedBase = baseUrl.endsWith("/")
            ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        String trimmedPath = path.startsWith("/") ? path : "/" + path;
        return trimmedBase + trimmedPath;
    }

    private static String hostOf(String url) {
        try {
            return java.net.URI.create(url).getHost();
        } catch (IllegalArgumentException e) {
            return url;
        }
    }

    private static String stripWww(String host) {
        if (host == null) {
            return "";
        }
        return host.startsWith("www.") ? host.substring(4) : host;
    }
}
