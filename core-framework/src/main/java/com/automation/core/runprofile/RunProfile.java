package com.automation.core.runprofile;

import com.automation.core.playwright.PlaywrightManager.BrowserEngine;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Runtime parameters that describe <em>how</em> a test suite executes, as
 * opposed to {@code EnvironmentConfig} which describes <em>what</em> system
 * is under test.
 *
 * <p>Typical knobs carried here:
 * <ul>
 *   <li>Browser channel &amp; engine (chrome, edge, firefox, webkit, chromium)</li>
 *   <li>Headless / viewport</li>
 *   <li>An absolute path to a directory holding environment JSON files when
 *       they live <em>outside</em> this repo (e.g. a shared secure volume)</li>
 *   <li>Output directories for reports, logs, screenshots and other
 *       run-time artefacts</li>
 * </ul>
 */
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class RunProfile {

    private String name;
    private String description;
    private String browser;
    private Boolean headless;
    private Integer timeout;
    private Viewport viewport;
    private String environmentConfigDir;
    private OutputDirs outputDirs;
    private String screenshotMode;
    private Map<String, Object> properties = new LinkedHashMap<>();

    /** Playwright engine (CHROMIUM / FIREFOX / WEBKIT) derived from {@link #browser}. */
    public BrowserEngine resolveBrowserEngine() {
        String name = browser == null ? "" : browser.trim().toLowerCase(Locale.ROOT);
        switch (name) {
            case "firefox":
                return BrowserEngine.FIREFOX;
            case "webkit":
            case "safari":
                return BrowserEngine.WEBKIT;
            case "chrome":
            case "chromium":
            case "edge":
            case "msedge":
            case "":
                return BrowserEngine.CHROMIUM;
            default:
                return BrowserEngine.CHROMIUM;
        }
    }

    /**
     * Playwright launch channel for Chromium-family browsers (e.g.
     * {@code "chrome"} or {@code "msedge"}). Returns {@code null} when the
     * profile asks for vanilla Chromium or a non-Chromium engine.
     */
    public String resolveBrowserChannel() {
        String name = browser == null ? "" : browser.trim().toLowerCase(Locale.ROOT);
        switch (name) {
            case "chrome":
                return "chrome";
            case "edge":
            case "msedge":
                return "msedge";
            default:
                return null;
        }
    }

    public boolean isHeadless() {
        return headless != null && headless;
    }

    public int timeoutOrDefault(int defaultMillis) {
        return timeout == null || timeout <= 0 ? defaultMillis : timeout;
    }

    /**
     * Typed view of {@link #screenshotMode}. Defaults to {@link ScreenshotMode#ON_FAILURE}
     * when the profile doesn't specify a value.
     */
    public ScreenshotMode resolveScreenshotMode() {
        return ScreenshotMode.parse(screenshotMode);
    }
}
