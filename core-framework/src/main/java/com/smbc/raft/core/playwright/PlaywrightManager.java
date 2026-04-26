package com.smbc.raft.core.playwright;

import com.smbc.raft.core.config.ConfigurationManager;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import lombok.extern.log4j.Log4j2;

/**
 * Manages Playwright browser instances and contexts
 */
@Log4j2
public class PlaywrightManager {
    
    private static final ThreadLocal<BrowserContext> CONTEXT = new ThreadLocal<>();
    private static final ThreadLocal<Page> PAGE = new ThreadLocal<>();
    
    private static ConfigurationManager config = ConfigurationManager.getInstance();
    
    /**
     * Initialize Playwright and create a fresh, isolated BrowserContext for the current thread.
     */
    public static void initializeBrowser() {
        initializeBrowser(BrowserEngine.valueOf(config.getBrowser().toUpperCase(java.util.Locale.ROOT)));
    }
    
    public static void initializeBrowser(BrowserEngine  browserEngine) {
        initializeBrowser(browserEngine, null);
    }

    /**
     * Launch a fresh BrowserContext using a pooled Browser process.
     */
    public static void initializeBrowser(BrowserEngine browserEngine, String channel) {
        try {
            // Get shared browser process from pool
            Browser br = BrowserPool.getBrowser(browserEngine, channel);

            // Create fresh CONTEXT
            Browser.NewContextOptions contextOptions = new Browser.NewContextOptions()
                    .setViewportSize(1920, 1080)
                    .setAcceptDownloads(true);

            BrowserContext ctx = br.newContext(contextOptions);
            CONTEXT.set(ctx);

            // Create PAGE
            Page pg = ctx.newPage();
            pg.setDefaultTimeout(config.getTimeout());
            PAGE.set(pg);

            log.debug("Playwright context and page initialized for current thread");

        } catch (Exception e) {
            log.error("Error initializing Playwright context/page", e);
            throw e;
        }
    }
    
    /**
     * Get the global Playwright instance
     */
    public static Playwright getPlaywright() {
        return BrowserPool.getPlaywright();
    }
    
    /**
     * Get the pooled Browser instance corresponding to current configuration
     */
    public static Browser getBrowser() {
        return BrowserPool.getBrowser(
            BrowserEngine.valueOf(config.getBrowser().toUpperCase(java.util.Locale.ROOT)),
            null
        );
    }
    
    /**
     * Get current thread's BrowserContext
     */
    public static BrowserContext getContext() {
        return CONTEXT.get();
    }
    
    /**
     * Get current thread's Page
     */
    public static Page getPage() {
        Page pg = PAGE.get();
        if (pg == null) {
            log.debug("Page not found for thread, initializing...");
            initializeBrowser();
            pg = PAGE.get();
        }
        return pg;
    }
    
    /**
     * Create a new page in the current CONTEXT
     */
    public static Page createNewPage() {
        BrowserContext ctx = getContext();
        if (ctx == null) {
            initializeBrowser();
            ctx = getContext();
        }
        Page newPage = ctx.newPage();
        newPage.setDefaultTimeout(config.getTimeout());
        PAGE.set(newPage);
        return newPage;
    }
    
    /**
     * Close current PAGE
     */
    public static void closePage() {
        Page pg = PAGE.get();
        if (pg != null) {
            pg.close();
            PAGE.remove();
        }
    }
    
    /**
     * Close the current context and page. 
     * The underlying pooled browser process remains alive for the next test.
     */
    public static void closeBrowser() {
        try {
            Page pg = PAGE.get();
            if (pg != null) {
                pg.close();
                PAGE.remove();
            }
            
            BrowserContext ctx = CONTEXT.get();
            if (ctx != null) {
                ctx.close();
                CONTEXT.remove();
            }
            
            log.debug("Playwright context and page closed for thread");
            
        } catch (Exception e) {
            log.error("Error closing Playwright context/page", e);
        }
    }
    
    /**
     * Take screenshot
     */
    public static byte[] takeScreenshot() {
        Page pg = getPage();
        if (pg != null) {
            return pg.screenshot();
        }
        return null;
    }
    
    /**
     * Take screenshot of specific element
     */
    public static byte[] takeScreenshot(String selector) {
        Page pg = getPage();
        if (pg != null) {
            Locator element = pg.locator(selector);
            if (element != null) {
                return element.screenshot();
            }
        }
        return null;
    }
    
    public enum BrowserEngine  {
        CHROMIUM,
        FIREFOX,
        WEBKIT
    }
}
