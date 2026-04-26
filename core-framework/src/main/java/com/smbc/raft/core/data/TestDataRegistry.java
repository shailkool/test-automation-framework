package com.smbc.raft.core.data;

import java.util.ArrayDeque;
import java.util.Deque;
import lombok.extern.log4j.Log4j2;

/**
 * Thread-safe registry for tracking test data created during a test. Registered teardown actions
 * are executed in LIFO order (last created, first deleted).
 */
@Log4j2
public class TestDataRegistry {

  private static final ThreadLocal<Deque<Runnable>> TEARDOWNS =
      ThreadLocal.withInitial(ArrayDeque::new);

  /** Register a teardown action to run at end of test. */
  public static void register(Runnable teardown) {
    TEARDOWNS.get().push(teardown); // push = LIFO
  }

  /** Run all registered teardowns for this thread, then clear the registry. */
  public static void cleanup() {
    Deque<Runnable> actions = TEARDOWNS.get();
    int count = actions.size();
    while (!actions.isEmpty()) {
      try {
        actions.pop().run();
      } catch (Exception e) {
        log.warn("Teardown action failed — continuing cleanup", e);
      }
    }
    TEARDOWNS.remove();
    log.debug("TestDataRegistry: ran {} teardown actions", count);
  }
}
