package com.automation.core.playwright;

import com.automation.core.config.ConfigurationManager;
import com.microsoft.playwright.*;
import lombok.extern.log4j.Log4j2;

/**
 * Manages Playwright browser instances and contexts
 */
@Log4j2
public class PlaywrightManager {
    
    private static final ThreadLocal<Playwright> playwright = new ThreadLocal<>();
    private static final ThreadLocal<Browser> browser = new ThreadLocal<>();
    private static final ThreadLocal<BrowserContext> context = new ThreadLocal<>();
    private static final ThreadLocal<Page> page = new ThreadLocal<>();
    
    private static ConfigurationManager config = ConfigurationManager.getInstance();
    
    /**
     * Initialize Playwright and create browser
     */
    public static void initializeBrowser() {
        initializeBrowser(BrowserType.valueOf(config.getBrowser().toUpperCase()));
    }
    
    public static void initializeBrowser(BrowserType browserType) {
        try {
            // Create Playwright instance
            Playwright pw = Playwright.create();
            playwright.set(pw);
            
            // Launch browser
            Browser.NewContextOptions contextOptions = new Browser.NewContextOptions()
                    .setViewportSize(1920, 1080)
                    .setAcceptDownloads(true);
            
            BrowserType.LaunchOptions launchOptions = new BrowserType.LaunchOptions()
                    .setHeadless(config.isHeadless())
                    .setTimeout(config.getTimeout());
            
            Browser br;
            switch (browserType) {
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
                    br = pw.chromium().launch(launchOptions);
            }
            
            browser.set(br);
            
            // Create context
            BrowserContext ctx = br.newContext(contextOptions);
            context.set(ctx);
            
            // Create page
            Page pg = ctx.newPage();
            pg.setDefaultTimeout(config.getTimeout());
            page.set(pg);
            
            log.info("Playwright browser initialized: {}", browserType);
            
        } catch (Exception e) {
            log.error("Error initializing Playwright browser", e);
            throw e;
        }
    }
    
    /**
     * Get current Playwright instance
     */
    public static Playwright getPlaywright() {
        return playwright.get();
    }
    
    /**
     * Get current browser instance
     */
    public static Browser getBrowser() {
        return browser.get();
    }
    
    /**
     * Get current context
     */
    public static BrowserContext getContext() {
        return context.get();
    }
    
    /**
     * Get current page
     */
    public static Page getPage() {
        if (page.get() == null) {
            createNewPage();
        }
        return page.get();
    }
    
    /**
     * Create a new page in the current context
     */
    public static Page createNewPage() {
        BrowserContext ctx = getContext();
        if (ctx == null) {
            initializeBrowser();
            ctx = getContext();
        }
        Page newPage = ctx.newPage();
        newPage.setDefaultTimeout(config.getTimeout());
        page.set(newPage);
        return newPage;
    }
    
    /**
     * Close current page
     */
    public static void closePage() {
        Page pg = page.get();
        if (pg != null) {
            pg.close();
            page.remove();
        }
    }
    
    /**
     * Close browser and cleanup
     */
    public static void closeBrowser() {
        try {
            Page pg = page.get();
            if (pg != null) {
                pg.close();
                page.remove();
            }
            
            BrowserContext ctx = context.get();
            if (ctx != null) {
                ctx.close();
                context.remove();
            }
            
            Browser br = browser.get();
            if (br != null) {
                br.close();
                browser.remove();
            }
            
            Playwright pw = playwright.get();
            if (pw != null) {
                pw.close();
                playwright.remove();
            }
            
            log.info("Playwright browser closed");
            
        } catch (Exception e) {
            log.error("Error closing Playwright browser", e);
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
    
    public enum BrowserType {
        CHROMIUM,
        FIREFOX,
        WEBKIT
    }
}
