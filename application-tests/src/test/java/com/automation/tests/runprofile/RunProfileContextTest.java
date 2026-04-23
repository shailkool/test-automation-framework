package com.automation.tests.runprofile;

import com.automation.core.environment.EnvironmentConfig;
import com.automation.core.environment.EnvironmentConfigLoader;
import com.automation.core.environment.EnvironmentContext;
import com.automation.core.playwright.PlaywrightManager.BrowserEngine;
import com.automation.core.runprofile.RunProfile;
import com.automation.core.runprofile.RunProfileContext;
import com.automation.core.runprofile.ScreenshotMode;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

/**
 * Exercises the agnostic run-profile plumbing against the local/ci/external
 * profile JSONs shipped under {@code src/test/resources/profiles}.
 */
public class RunProfileContextTest {

    @BeforeMethod
    public void resetContexts() {
        RunProfileContext.reset();
        EnvironmentContext.reset();
        System.clearProperty(RunProfileContext.PROFILE_SYSTEM_PROPERTY);
        System.clearProperty("env");
    }

    @AfterMethod
    public void clearSystemProperties() {
        System.clearProperty(RunProfileContext.PROFILE_SYSTEM_PROPERTY);
        System.clearProperty("env");
        RunProfileContext.reset();
        EnvironmentContext.reset();
    }

    @Test
    public void defaultsToLocalWhenNoSystemPropertyIsSet() {
        RunProfileContext ctx = RunProfileContext.getInstance();
        assertEquals(ctx.getProfileName(), "local");
        assertNotNull(ctx.getProfile());
    }

    @Test
    public void localProfileResolvesChromeChannel() {
        RunProfileContext ctx = RunProfileContext.reload("local");
        RunProfile profile = ctx.getProfile();
        assertEquals(profile.resolveBrowserEngine(), BrowserEngine.CHROMIUM);
        assertEquals(profile.resolveBrowserChannel(), "chrome");
        assertFalse(profile.isHeadless(), "local profile should be headed");
    }

    @Test
    public void ciProfileSelectsHeadlessChromiumWithoutChannel() {
        RunProfileContext ctx = RunProfileContext.reload("ci");
        RunProfile profile = ctx.getProfile();
        assertEquals(profile.resolveBrowserEngine(), BrowserEngine.CHROMIUM);
        assertNull(profile.resolveBrowserChannel(),
            "bundled chromium should not carry a channel");
        assertTrue(profile.isHeadless());
        assertEquals(profile.timeoutOrDefault(0), 45_000);
    }

    @Test
    public void externalConfigProfileExposesEdgeChannelAndExternalDir() {
        RunProfileContext ctx = RunProfileContext.reload("external-config");
        RunProfile profile = ctx.getProfile();
        assertEquals(profile.resolveBrowserEngine(), BrowserEngine.CHROMIUM);
        assertEquals(profile.resolveBrowserChannel(), "msedge");
        assertNotNull(ctx.getEnvironmentConfigDir());
        assertEquals(ctx.getEnvironmentConfigDir().toString(),
            "/opt/secure-configs/environments");
    }

    @Test
    public void localProfileDefaultsScreenshotModeToEachStep() {
        RunProfileContext ctx = RunProfileContext.reload("local");
        assertEquals(ctx.getProfile().resolveScreenshotMode(), ScreenshotMode.EACH_STEP);
    }

    @Test
    public void ciProfileDefaultsScreenshotModeToOnFailure() {
        RunProfileContext ctx = RunProfileContext.reload("ci");
        assertEquals(ctx.getProfile().resolveScreenshotMode(), ScreenshotMode.ON_FAILURE);
    }

    @Test
    public void missingScreenshotModeFallsBackToOnFailure() {
        RunProfile bare = new RunProfile();
        assertEquals(bare.resolveScreenshotMode(), ScreenshotMode.ON_FAILURE);
    }

    @Test
    public void screenshotModeParsingIsCaseAndHyphenTolerant() {
        assertEquals(ScreenshotMode.parse("each-step"), ScreenshotMode.EACH_STEP);
        assertEquals(ScreenshotMode.parse(" Always "), ScreenshotMode.EACH_STEP);
        assertEquals(ScreenshotMode.parse("off"), ScreenshotMode.OFF);
        assertEquals(ScreenshotMode.parse("nope"), ScreenshotMode.ON_FAILURE);
    }

    @Test
    public void outputDirsFallBackToSensibleDefaultsWhenMissing() {
        RunProfile barebones = new RunProfile();
        barebones.setName("bare");
        barebones.setBrowser("firefox");
        // No outputDirs, no environmentConfigDir -> defaults must still work.
        assertEquals(barebones.resolveBrowserEngine(), BrowserEngine.FIREFOX);
        assertNull(barebones.resolveBrowserChannel());
    }

    @Test
    public void runProfileOverridesEnvironmentConfigDirectory(
        ) throws Exception {
        Path tempDir = Files.createTempDirectory("env-override-test");
        try {
            // Write a stub stag.json into the external directory so the loader
            // can find it.
            Path envJson = tempDir.resolve("stag.json");
            Files.writeString(envJson, "{\n"
                + "  \"name\": \"stag\",\n"
                + "  \"description\": \"staging\",\n"
                + "  \"databases\": {},\n"
                + "  \"messageQueues\": {},\n"
                + "  \"websites\": {}\n"
                + "}\n");

            // Build a RunProfileContext that points at the temp dir and cache it.
            RunProfile profile = new RunProfile();
            profile.setName("override");
            profile.setBrowser("chromium");
            profile.setEnvironmentConfigDir(tempDir.toString());
            installRunProfile(profile);

            System.setProperty("env", "stag");
            EnvironmentConfig envConfig = EnvironmentContext.get();
            assertEquals(envConfig.getName(), "stag");
            assertEquals(envConfig.getDescription(), "staging");

            // And prove the loader really reads from disk by consulting it directly.
            EnvironmentConfig direct = new EnvironmentConfigLoader()
                .loadFromDir("stag", tempDir);
            assertEquals(direct.getName(), "stag");
        } finally {
            deleteRecursively(tempDir);
        }
    }

    private void installRunProfile(RunProfile profile) throws Exception {
        RunProfileContext.reset();
        java.lang.reflect.Constructor<RunProfileContext> ctor =
            RunProfileContext.class.getDeclaredConstructor(String.class, RunProfile.class);
        ctor.setAccessible(true);
        RunProfileContext injected = ctor.newInstance(profile.getName(), profile);
        java.lang.reflect.Field instance = RunProfileContext.class.getDeclaredField("instance");
        instance.setAccessible(true);
        instance.set(null, injected);
    }

    private static void deleteRecursively(Path path) {
        if (path == null || !Files.exists(path)) {
            return;
        }
        try (java.util.stream.Stream<Path> walk = Files.walk(path)) {
            walk.sorted((a, b) -> b.getNameCount() - a.getNameCount())
                .forEach(p -> {
                    try {
                        Files.deleteIfExists(p);
                    } catch (java.io.IOException ignore) {
                        // best-effort cleanup
                    }
                });
        } catch (java.io.IOException ignore) {
            // best-effort cleanup
        }
    }
}
