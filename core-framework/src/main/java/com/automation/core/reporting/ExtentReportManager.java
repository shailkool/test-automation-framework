package com.automation.core.reporting;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.MediaEntityBuilder;
import com.aventstack.extentreports.Status;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import com.aventstack.extentreports.reporter.configuration.Theme;
import lombok.extern.log4j.Log4j2;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

/**
 * Manages Extent Reports for test execution
 */
@Log4j2
public class ExtentReportManager {
    
    private static ExtentReports extent;
    private static ThreadLocal<ExtentTest> test = new ThreadLocal<>();
    private static String reportPath;
    
    /**
     * Initialize extent reports
     */
    public static void initReports() {
        if (extent == null) {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
            reportPath = "test-output/extent-reports/TestReport_" + timestamp + ".html";
            
            // Create directory if it doesn't exist
            File reportDir = new File("test-output/extent-reports");
            if (!reportDir.exists()) {
                reportDir.mkdirs();
            }
            
            ExtentSparkReporter sparkReporter = new ExtentSparkReporter(reportPath);
            sparkReporter.config().setTheme(Theme.STANDARD);
            sparkReporter.config().setDocumentTitle("Test Automation Report");
            sparkReporter.config().setReportName("Test Execution Report");
            sparkReporter.config().setTimeStampFormat("yyyy-MM-dd HH:mm:ss");
            
            extent = new ExtentReports();
            extent.attachReporter(sparkReporter);
            
            // Set system information
            extent.setSystemInfo("OS", System.getProperty("os.name"));
            extent.setSystemInfo("Java Version", System.getProperty("java.version"));
            extent.setSystemInfo("User", System.getProperty("user.name"));
            
            log.info("Extent Reports initialized: {}", reportPath);
        }
    }
    
    /**
     * Create a new test
     */
    public static void createTest(String testName) {
        ExtentTest extentTest = extent.createTest(testName);
        test.set(extentTest);
    }
    
    /**
     * Create a new test with description
     */
    public static void createTest(String testName, String description) {
        ExtentTest extentTest = extent.createTest(testName, description);
        test.set(extentTest);
    }
    
    /**
     * Get current test
     */
    public static ExtentTest getTest() {
        return test.get();
    }
    
    /**
     * Log info message
     */
    public static void logInfo(String message) {
        if (test.get() != null) {
            test.get().log(Status.INFO, message);
        }
    }
    
    /**
     * Log pass message
     */
    public static void logPass(String message) {
        if (test.get() != null) {
            test.get().log(Status.PASS, message);
        }
    }
    
    /**
     * Log fail message
     */
    public static void logFail(String message) {
        if (test.get() != null) {
            test.get().log(Status.FAIL, message);
        }
    }
    
    /**
     * Log skip message
     */
    public static void logSkip(String message) {
        if (test.get() != null) {
            test.get().log(Status.SKIP, message);
        }
    }
    
    /**
     * Log warning message
     */
    public static void logWarning(String message) {
        if (test.get() != null) {
            test.get().log(Status.WARNING, message);
        }
    }
    
    /**
     * Attach screenshot
     */
    public static void attachScreenshot(byte[] screenshot) {
        if (test.get() != null && screenshot != null) {
            try {
                String base64 = Base64.getEncoder().encodeToString(screenshot);
                test.get().fail("Screenshot on failure",
                        MediaEntityBuilder.createScreenCaptureFromBase64String(base64).build());
            } catch (Exception e) {
                log.error("Error attaching screenshot to report", e);
            }
        }
    }
    
    /**
     * Attach screenshot with message
     */
    public static void attachScreenshot(byte[] screenshot, String message) {
        if (test.get() != null && screenshot != null) {
            try {
                String base64 = Base64.getEncoder().encodeToString(screenshot);
                test.get().info(message,
                        MediaEntityBuilder.createScreenCaptureFromBase64String(base64).build());
            } catch (Exception e) {
                log.error("Error attaching screenshot to report", e);
            }
        }
    }
    
    /**
     * Add category to test
     */
    public static void assignCategory(String... categories) {
        if (test.get() != null) {
            test.get().assignCategory(categories);
        }
    }
    
    /**
     * Add author to test
     */
    public static void assignAuthor(String... authors) {
        if (test.get() != null) {
            test.get().assignAuthor(authors);
        }
    }
    
    /**
     * Flush reports
     */
    public static void flushReports() {
        if (extent != null) {
            extent.flush();
            log.info("Extent Reports flushed: {}", reportPath);
        }
    }
}
