package com.smbc.raft.core.retry;

public interface LoopStrategy {
  int getIterations();

  long getIntervalMillis();
}
