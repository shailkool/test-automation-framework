package com.smbc.raft.core.playwright;

import com.smbc.raft.core.config.ConfigurationManager;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import lombok.extern.log4j.Log4j2;

/**
 * Manages Playwright browser instances and contexts
 */
@Log4j2
public class PlaywrightManager {
    
    private static final ThreadLocal<Playwright> PLAYWRIGHT = new ThreadLocal<>();
    private static final ThreadLocal<Browser> BROWSER = new ThreadLocal<>();
    private static final ThreadLocal<BrowserContext> CONTEXT = new ThreadLocal<>();
    private static final ThreadLocal<Page> PAGE = new ThreadLocal<>();
    
    private static ConfigurationManager config = ConfigurationManager.getInstance();
    
    /**
     * Initialize Playwright and create BROWSER
     */
    public static void initializeBrowser() {
        initializeBrowser(BrowserEngine.valueOf(config.getBrowser().toUpperCase(java.util.Locale.ROOT)));
    }
    
    public static void initializeBrowser(BrowserEngine  browserEngine) {
        initializeBrowser(browserEngine, null);
    }

    /**
     * Launch Playwright with an explicit Chromium channel (e.g. {@code "chrome"}
     * or {@code "msedge"}) so run profiles that ask for stock Chrome or Edge
     * get the actual branded binary rather than bundled Chromium. The channel
     * is ignored for Firefox / WebKit engines.
     */
    public static void initializeBrowser(BrowserEngine browserEngine, String channel) {
        try {
            // Create Playwright instance
            Playwright pw = Playwright.create();
            PLAYWRIGHT.set(pw);

            // Launch BROWSER
            Browser.NewContextOptions contextOptions = new Browser.NewContextOptions()
                    .setViewportSize(1920, 1080)
                    .setAcceptDownloads(true);

            BrowserType.LaunchOptions launchOptions = new BrowserType.LaunchOptions()
                    .setHeadless(config.isHeadless())
                    .setTimeout(config.getTimeout());

            if (channel != null && !channel.isBlank() && browserEngine == BrowserEngine.CHROMIUM) {
                launchOptions.setChannel(channel);
            }

            Browser br;
            switch (browserEngine) {
                case CHROMIUM:
                    br = pw.chromium().launch(launchOptions);
                    break;
                case FIREFOX:
                    br = pw.firefox().launch(launchOptions);
                    break;
                case WEBKIT:
                    br = pw.webkit().launch(launchOptions);
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported browser engine: " + browserEngine);
            }

            BROWSER.set(br);

            // Create CONTEXT
            BrowserContext ctx = br.newContext(contextOptions);
            CONTEXT.set(ctx);

            // Create PAGE
            Page pg = ctx.newPage();
            pg.setDefaultTimeout(config.getTimeout());
            PAGE.set(pg);

            log.info("Playwright BROWSER initialized: engine={}, channel={}", browserEngine, channel);

        } catch (Exception e) {
            log.error("Error initializing Playwright BROWSER", e);
            throw e;
        }
    }
    
    /**
     * Get current Playwright instance
     */
    public static Playwright getPlaywright() {
        return PLAYWRIGHT.get();
    }
    
    /**
     * Get current BROWSER instance
     */
    public static Browser getBrowser() {
        return BROWSER.get();
    }
    
    /**
     * Get current CONTEXT
     */
    public static BrowserContext getContext() {
        return CONTEXT.get();
    }
    
    /**
     * Get current PAGE
     */
    public static Page getPage() {
        if (PAGE.get() == null) {
            createNewPage();
        }
        return PAGE.get();
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
     * Close BROWSER and cleanup
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
            
            Browser br = BROWSER.get();
            if (br != null) {
                br.close();
                BROWSER.remove();
            }
            
            Playwright pw = PLAYWRIGHT.get();
            if (pw != null) {
                pw.close();
                PLAYWRIGHT.remove();
            }
            
            log.info("Playwright BROWSER closed");
            
        } catch (Exception e) {
            log.error("Error closing Playwright BROWSER", e);
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
