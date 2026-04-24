package com.smbc.raft.core.utils;

import com.smbc.raft.core.database.DatabaseManager;
import com.smbc.raft.core.messaging.MessagingManager;
import com.smbc.raft.core.playwright.PlaywrightManager;
import com.smbc.raft.core.reporting.ExtentReportManager;
import lombok.extern.log4j.Log4j2;
import org.testng.ITestResult;
import org.testng.annotations.*;

/**
 * Base test class with setup and teardown methods
 */
@Log4j2
public class BaseTest {
    
    @BeforeSuite
    public void beforeSuite() {
        log.info("Test Suite Started");
        ExtentReportManager.initReports();
    }
    
    @BeforeClass
    public void beforeClass() {
        log.info("Test Class Started: {}", this.getClass().getSimpleName());
    }
    
    @BeforeMethod
    public void beforeMethod(ITestResult result) {
        log.info("Test Method Started: {}", result.getMethod().getMethodName());
        ExtentReportManager.createTest(result.getMethod().getMethodName());
    }
    
    @AfterMethod
    public void afterMethod(ITestResult result) {
        // Log test result
        if (result.getStatus() == ITestResult.FAILURE) {
            log.error("Test Failed: {}", result.getMethod().getMethodName());
            
            // Capture screenshot if browser is open
            try {
                byte[] screenshot = PlaywrightManager.takeScreenshot();
                if (screenshot != null) {
                    ExtentReportManager.attachScreenshot(screenshot);
                }
            } catch (Exception e) {
                log.warn("Could not capture screenshot", e);
            }
            
            ExtentReportManager.logFail(result.getThrowable().getMessage());
        } else if (result.getStatus() == ITestResult.SUCCESS) {
            log.info("Test Passed: {}", result.getMethod().getMethodName());
            ExtentReportManager.logPass("Test passed successfully");
        } else if (result.getStatus() == ITestResult.SKIP) {
            log.warn("Test Skipped: {}", result.getMethod().getMethodName());
            ExtentReportManager.logSkip("Test was skipped");
        }
        
        // Clean up all test data created during this test
        com.smbc.raft.core.data.TestDataRegistry.cleanup();

        // Close browser after each test
        PlaywrightManager.closeBrowser();
    }
    
    @AfterClass
    public void afterClass() {
        log.info("Test Class Completed: {}", this.getClass().getSimpleName());
    }
    
    @AfterSuite
    public void afterSuite() {
        log.info("Test Suite Completed");
        
        // Close all database connections
        DatabaseManager.closeAll();

        // Close all messaging clients (Kafka, JMS/MQ)
        MessagingManager.closeAll();

        // Flush extent reports
        ExtentReportManager.flushReports();
    }
}
