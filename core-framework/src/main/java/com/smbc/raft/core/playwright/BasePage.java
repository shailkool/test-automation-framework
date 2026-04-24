package com.smbc.raft.core.playwright;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.WaitForSelectorState;
import lombok.extern.log4j.Log4j2;

import java.nio.file.Paths;
import java.util.List;

/**
 * Base page class with common Playwright operations
 */
@Log4j2
public class BasePage {
    
    protected Page page;
    
    public BasePage() {
        this.page = PlaywrightManager.getPage();
    }
    
    public BasePage(Page page) {
        this.page = page;
    }
    
    // Navigation
    public void navigateTo(String url) {
        log.info("Navigating to: {}", url);
        page.navigate(url);
    }
    
    public void reload() {
        log.debug("Reloading page");
        page.reload();
    }
    
    public void goBack() {
        log.debug("Navigating back");
        page.goBack();
    }
    
    public void goForward() {
        log.debug("Navigating forward");
        page.goForward();
    }
    
    public String getCurrentUrl() {
        return page.url();
    }
    
    public String getTitle() {
        return page.title();
    }
    
    // Element interactions
    public void click(String selector) {
        log.debug("Clicking element: {}", selector);
        page.locator(selector).click();
    }
    
    public void clickByText(String text) {
        log.debug("Clicking element with text: {}", text);
        page.getByText(text).click();
    }
    
    public void clickByRole(AriaRole role, String name) {
        log.debug("Clicking element by role: {} with name: {}", role, name);
        page.getByRole(role, new Page.GetByRoleOptions().setName(name)).click();
    }
    
    public void fill(String selector, String value) {
        log.debug("Filling element {} with value: {}", selector, value);
        page.locator(selector).fill(value);
    }
    
    public void type(String selector, String text) {
        log.debug("Typing into element {}: {}", selector, text);
        page.locator(selector).pressSequentially(text);
    }
    
    public void selectOption(String selector, String value) {
        log.debug("Selecting option {} in element {}", value, selector);
        page.locator(selector).selectOption(value);
    }
    
    public void check(String selector) {
        log.debug("Checking checkbox: {}", selector);
        page.locator(selector).check();
    }
    
    public void uncheck(String selector) {
        log.debug("Unchecking checkbox: {}", selector);
        page.locator(selector).uncheck();
    }
    
    public void hover(String selector) {
        log.debug("Hovering over element: {}", selector);
        page.locator(selector).hover();
    }
    
    public void dragAndDrop(String sourceSelector, String targetSelector) {
        log.debug("Dragging {} to {}", sourceSelector, targetSelector);
        page.locator(sourceSelector).dragTo(page.locator(targetSelector));
    }
    
    // Element queries
    public String getText(String selector) {
        return page.locator(selector).textContent();
    }
    
    public String getValue(String selector) {
        return page.locator(selector).inputValue();
    }
    
    public String getAttribute(String selector, String attribute) {
        return page.locator(selector).getAttribute(attribute);
    }
    
    public boolean isVisible(String selector) {
        return page.locator(selector).isVisible();
    }
    
    public boolean isEnabled(String selector) {
        return page.locator(selector).isEnabled();
    }
    
    public boolean isChecked(String selector) {
        return page.locator(selector).isChecked();
    }
    
    public int getElementCount(String selector) {
        return page.locator(selector).count();
    }
    
    public List<String> getAllTexts(String selector) {
        return page.locator(selector).allTextContents();
    }
    
    // Waits
    public void waitForSelector(String selector) {
        log.debug("Waiting for selector: {}", selector);
        page.waitForSelector(selector);
    }
    
    public void waitForSelector(String selector, int timeout) {
        log.debug("Waiting for selector: {} with timeout: {}ms", selector, timeout);
        page.waitForSelector(selector, new Page.WaitForSelectorOptions().setTimeout(timeout));
    }
    
    public void waitForVisible(String selector) {
        log.debug("Waiting for element to be visible: {}", selector);
        page.locator(selector).waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));
    }
    
    public void waitForHidden(String selector) {
        log.debug("Waiting for element to be hidden: {}", selector);
        page.locator(selector).waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.HIDDEN));
    }
    
    public void waitForLoadState() {
        page.waitForLoadState();
    }
    
    public void waitForTimeout(int milliseconds) {
        page.waitForTimeout(milliseconds);
    }
    
    // Alerts and dialogs
    public void acceptDialog() {
        page.onDialog(Dialog::accept);
    }
    
    public void dismissDialog() {
        page.onDialog(Dialog::dismiss);
    }
    
    // Screenshots
    public byte[] takeScreenshot() {
        return page.screenshot();
    }
    
    public void takeScreenshot(String path) {
        page.screenshot(new Page.ScreenshotOptions().setPath(Paths.get(path)));
    }
    
    // Frames
    public FrameLocator getFrame(String selector) {
        return page.frameLocator(selector);
    }
    
    // JavaScript execution
    public Object executeScript(String script) {
        return page.evaluate(script);
    }
    
    public Object executeScript(String script, Object arg) {
        return page.evaluate(script, arg);
    }
    
    // Cookies
    public void clearCookies() {
        page.context().clearCookies();
    }
    
    // Local storage
    public void clearLocalStorage() {
        page.evaluate("window.localStorage.clear()");
    }
    
    // Session storage
    public void clearSessionStorage() {
        page.evaluate("window.sessionStorage.clear()");
    }
}
