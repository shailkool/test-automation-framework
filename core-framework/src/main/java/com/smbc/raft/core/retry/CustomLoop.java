package com.smbc.raft.core.retry;

public class CustomLoop implements LoopStrategy {
  private final int iterations;
  private final long intervalMillis;

  public CustomLoop(int iterations, long intervalMillis) {
    this.iterations = iterations;
    this.intervalMillis = intervalMillis;
  }

  public int getIterations() {
    return iterations;
  }

  public long getIntervalMillis() {
    return intervalMillis;
  }
}
