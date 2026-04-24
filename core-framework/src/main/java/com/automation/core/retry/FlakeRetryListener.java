package com.automation.core.retry;

import org.testng.ITestListener;
import org.testng.ITestResult;

/**
 * Applies FlakeRetryAnalyzer globally via the TestNG listener mechanism.
 * Add to testng.xml <listeners> block to enable suite-wide retry without
 * touching individual @Test annotations.
 */
public class FlakeRetryListener implements ITestListener {

    @Override
    public void onTestStart(ITestResult result) {
        result.getMethod().setRetryAnalyzerClass(FlakeRetryAnalyzer.class);
    }
}
