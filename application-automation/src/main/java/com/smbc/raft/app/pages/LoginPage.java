package com.smbc.raft.app.pages;

import com.microsoft.playwright.Page;
import com.smbc.raft.core.playwright.BasePage;
import com.smbc.raft.core.reporting.ExtentReportManager;
import lombok.extern.log4j.Log4j2;

/** Page Object for Login Page This is an example - modify according to your application */
@Log4j2
public class LoginPage extends BasePage {

  // Locators
  private static final String USERNAME_INPUT = "#username";
  private static final String PASSWORD_INPUT = "#password";
  private static final String LOGIN_BUTTON = "#login-button";
  private static final String ERROR_MESSAGE = ".error-message";
  private static final String REMEMBER_ME_CHECKBOX = "#remember-me";

  public LoginPage() {
    super();
  }

  public LoginPage(Page page) {
    super(page);
  }

  /** Navigate to login page */
  public LoginPage navigateToLoginPage(String url) {
    log.info("Navigating to login page");
    navigateTo(url);
    ExtentReportManager.logInfo("Navigated to login page: " + url);
    return this;
  }

  /** Enter username */
  public LoginPage enterUsername(String username) {
    log.info("Entering username: {}", username);
    waitForVisible(USERNAME_INPUT);
    fill(USERNAME_INPUT, username);
    ExtentReportManager.logInfo("Entered username");
    return this;
  }

  /** Enter password */
  public LoginPage enterPassword(String password) {
    log.info("Entering password");
    fill(PASSWORD_INPUT, password);
    ExtentReportManager.logInfo("Entered password");
    return this;
  }

  /** Click remember me checkbox */
  public LoginPage checkRememberMe() {
    log.info("Checking remember me checkbox");
    check(REMEMBER_ME_CHECKBOX);
    ExtentReportManager.logInfo("Checked remember me");
    return this;
  }

  /** Click login button */
  public void clickLogin() {
    log.info("Clicking login button");
    click(LOGIN_BUTTON);
    ExtentReportManager.logInfo("Clicked login button");
  }

  /** Perform login with credentials */
  public void login(String username, String password) {
    log.info("Performing login");
    enterUsername(username);
    enterPassword(password);
    clickLogin();
    ExtentReportManager.logInfo("Login performed");
  }

  /** Perform login with remember me */
  public void loginWithRememberMe(String username, String password) {
    log.info("Performing login with remember me");
    enterUsername(username);
    enterPassword(password);
    checkRememberMe();
    clickLogin();
    ExtentReportManager.logInfo("Login with remember me performed");
  }

  /** Get error message */
  public String getErrorMessage() {
    log.info("Getting error message");
    waitForVisible(ERROR_MESSAGE);
    String errorText = getText(ERROR_MESSAGE);
    ExtentReportManager.logInfo("Error message: " + errorText);
    return errorText;
  }

  /** Check if error message is displayed */
  public boolean isErrorMessageDisplayed() {
    return isVisible(ERROR_MESSAGE);
  }

  /** Verify login page is loaded */
  public boolean isLoginPageLoaded() {
    return isVisible(USERNAME_INPUT) && isVisible(PASSWORD_INPUT) && isVisible(LOGIN_BUTTON);
  }
}
