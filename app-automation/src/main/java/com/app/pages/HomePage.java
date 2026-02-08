package com.app.pages;

import com.framework.ui.BasePage;
import lombok.extern.log4j.Log4j2;

/**
 * Page Object for Home Page
 * This is an example implementation
 */
@Log4j2
public class HomePage extends BasePage {
    
    // Locators
    private static final String USER_PROFILE = ".user-profile";
    private static final String LOGOUT_BUTTON = "button:has-text('Logout')";
    private static final String DASHBOARD_HEADER = "h1:has-text('Dashboard')";
    private static final String NAVIGATION_MENU = ".navigation-menu";
    
    /**
     * Verify home page is loaded
     */
    public boolean isHomePageLoaded() {
        waitForElement(DASHBOARD_HEADER);
        return isVisible(DASHBOARD_HEADER);
    }
    
    /**
     * Get username from profile
     */
    public String getLoggedInUsername() {
        waitForElement(USER_PROFILE);
        String username = getText(USER_PROFILE);
        log.info("Logged in user: {}", username);
        return username;
    }
    
    /**
     * Perform logout
     */
    public void logout() {
        click(USER_PROFILE);
        click(LOGOUT_BUTTON);
        log.info("User logged out");
    }
    
    /**
     * Navigate to a menu item
     */
    public void navigateToMenuItem(String menuItem) {
        String menuLocator = String.format("%s a:has-text('%s')", NAVIGATION_MENU, menuItem);
        click(menuLocator);
        log.info("Navigated to menu item: {}", menuItem);
    }
    
    /**
     * Verify navigation menu is displayed
     */
    public boolean isNavigationMenuDisplayed() {
        return isVisible(NAVIGATION_MENU);
    }
}
