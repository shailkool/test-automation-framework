package com.smbc.raft.app.pages;

import com.smbc.raft.core.playwright.BasePage;
import com.smbc.raft.core.reporting.ExtentReportManager;
import com.microsoft.playwright.Page;
import lombok.extern.log4j.Log4j2;

/**
 * Page Object for Home Page
 * This is an example - modify according to your application
 */
@Log4j2
public class HomePage extends BasePage {
    
    // Locators
    private static final String WELCOME_MESSAGE = ".welcome-message";
    private static final String USER_PROFILE = "#user-profile";
    private static final String LOGOUT_BUTTON = "#logout";
    private static final String DASHBOARD_LINK = "a[href='/dashboard']";
    private static final String SETTINGS_LINK = "a[href='/settings']";
    
    public HomePage() {
        super();
    }
    
    public HomePage(Page page) {
        super(page);
    }
    
    /**
     * Verify home page is loaded
     */
    public boolean isHomePageLoaded() {
        waitForVisible(WELCOME_MESSAGE);
        boolean isLoaded = isVisible(WELCOME_MESSAGE) && isVisible(USER_PROFILE);
        ExtentReportManager.logInfo("Home page loaded: " + isLoaded);
        return isLoaded;
    }
    
    /**
     * Get welcome message
     */
    public String getWelcomeMessage() {
        log.info("Getting welcome message");
        String message = getText(WELCOME_MESSAGE);
        ExtentReportManager.logInfo("Welcome message: " + message);
        return message;
    }
    
    /**
     * Get username from profile
     */
    public String getUsername() {
        log.info("Getting username from profile");
        String username = getText(USER_PROFILE);
        ExtentReportManager.logInfo("Username: " + username);
        return username;
    }
    
    /**
     * Navigate to dashboard
     */
    public void navigateToDashboard() {
        log.info("Navigating to dashboard");
        click(DASHBOARD_LINK);
        ExtentReportManager.logInfo("Navigated to dashboard");
    }
    
    /**
     * Navigate to settings
     */
    public void navigateToSettings() {
        log.info("Navigating to settings");
        click(SETTINGS_LINK);
        ExtentReportManager.logInfo("Navigated to settings");
    }
    
    /**
     * Logout
     */
    public void logout() {
        log.info("Logging out");
        click(LOGOUT_BUTTON);
        ExtentReportManager.logInfo("Logged out");
    }
}
