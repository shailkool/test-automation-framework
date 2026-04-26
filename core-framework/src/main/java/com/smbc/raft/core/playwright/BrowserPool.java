package com.smbc.raft.core.playwright;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Playwright;
import com.smbc.raft.core.config.ConfigurationManager;
import lombok.extern.log4j.Log4j2;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages the lifecycle of Playwright Browser processes.
 * Implements a pooled approach to reuse the heavy browser process
 * across multiple threads/tests while providing isolation via BrowserContexts.
 */
@Log4j2
public class BrowserPool {

    private static final Map<String, Browser> BROWSERS = new ConcurrentHashMap<>();
    private static Playwright playwright;
    private static final ConfigurationManager CONFIG = ConfigurationManager.getInstance();

    private BrowserPool() {}

    /**
     * Gets or creates the global Playwright instance.
     */
    public static synchronized Playwright getPlaywright() {
        if (playwright == null) {
            playwright = Playwright.create();
            log.info("Global Playwright instance created");
        }
        return playwright;
    }

    /**
     * Gets a shared Browser instance for the given engine and channel.
     * Reuses the browser process if it already exists for the engine/channel combination.
     */
    public static synchronized Browser getBrowser(PlaywrightManager.BrowserEngine engine, String channel) {
        String key = engine.name() + (channel != null ? ":" + channel : "");
        return BROWSERS.computeIfAbsent(key, k -> launchBrowser(engine, channel));
    }

    private static Browser launchBrowser(PlaywrightManager.BrowserEngine engine, String channel) {
        Playwright pw = getPlaywright();
        BrowserType.LaunchOptions options = new BrowserType.LaunchOptions()
                .setHeadless(CONFIG.isHeadless())
                .setTimeout(CONFIG.getTimeout());

        if (channel != null && !channel.isBlank() && engine == PlaywrightManager.BrowserEngine.CHROMIUM) {
            options.setChannel(channel);
        }

        log.info("Launching shared Browser process: engine={}, channel={}, headless={}",
                engine, channel, CONFIG.isHeadless());

        return switch (engine) {
            case CHROMIUM -> pw.chromium().launch(options);
            case FIREFOX -> pw.firefox().launch(options);
            case WEBKIT -> pw.webkit().launch(options);
        };
    }

    /**
     * Closes all active browser processes and cleans up the Playwright instance.
     * Should be called at the end of the test suite (e.g., in a Global Shutdown Hook).
     */
    public static synchronized void closeAll() {
        BROWSERS.forEach((key, browser) -> {
            try {
                browser.close();
                log.info("Pooled Browser process closed: {}", key);
            } catch (Exception e) {
                log.error("Error closing pooled browser {}", key, e);
            }
        });
        BROWSERS.clear();

        if (playwright != null) {
            try {
                playwright.close();
                log.info("Global Playwright instance closed");
            } catch (Exception e) {
                log.error("Error closing global Playwright", e);
            }
            playwright = null;
        }
    }
}
