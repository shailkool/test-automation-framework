package com.app.pages;

import com.framework.ui.BasePage;
import lombok.extern.log4j.Log4j2;

/**
 * Page Object for Login Page
 * This is an example implementation
 */
@Log4j2
public class LoginPage extends BasePage {
    
    // Locators
    private static final String USERNAME_INPUT = "input[name='username']";
    private static final String PASSWORD_INPUT = "input[name='password']";
    private static final String LOGIN_BUTTON = "button[type='submit']";
    private static final String ERROR_MESSAGE = ".error-message";
    private static final String FORGOT_PASSWORD_LINK = "a:has-text('Forgot Password')";
    
    /**
     * Navigate to login page
     */
    public LoginPage navigateToLoginPage(String url) {
        navigateTo(url);
        log.info("Navigated to login page");
        return this;
    }
    
    /**
     * Enter username
     */
    public LoginPage enterUsername(String username) {
        fill(USERNAME_INPUT, username);
        log.info("Entered username: {}", username);
        return this;
    }
    
    /**
     * Enter password
     */
    public LoginPage enterPassword(String password) {
        fill(PASSWORD_INPUT, password);
        log.info("Password entered");
        return this;
    }
    
    /**
     * Click login button
     */
    public void clickLogin() {
        click(LOGIN_BUTTON);
        log.info("Clicked login button");
    }
    
    /**
     * Perform complete login
     */
    public void login(String username, String password) {
        enterUsername(username);
        enterPassword(password);
        clickLogin();
        log.info("Login performed for user: {}", username);
    }
    
    /**
     * Get error message text
     */
    public String getErrorMessage() {
        waitForElement(ERROR_MESSAGE);
        String error = getText(ERROR_MESSAGE);
        log.info("Error message displayed: {}", error);
        return error;
    }
    
    /**
     * Check if error message is displayed
     */
    public boolean isErrorMessageDisplayed() {
        return isVisible(ERROR_MESSAGE);
    }
    
    /**
     * Click forgot password link
     */
    public void clickForgotPassword() {
        click(FORGOT_PASSWORD_LINK);
        log.info("Clicked forgot password link");
    }
    
    /**
     * Verify login page is loaded
     */
    public boolean isLoginPageLoaded() {
        waitForElement(USERNAME_INPUT);
        return isVisible(USERNAME_INPUT) && isVisible(PASSWORD_INPUT) && isVisible(LOGIN_BUTTON);
    }
}
