package com.automation.core.retry;

public interface LoopStrategy {
    int getIterations();
    long getIntervalMillis();
}
