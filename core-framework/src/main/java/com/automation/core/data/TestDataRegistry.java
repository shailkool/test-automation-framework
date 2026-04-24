package com.automation.core.data;

import lombok.extern.log4j.Log4j2;
import java.util.*;

/**
 * Thread-safe registry for tracking test data created during a test.
 * Registered teardown actions are executed in LIFO order (last created, first deleted).
 */
@Log4j2
public class TestDataRegistry {

    private static final ThreadLocal<Deque<Runnable>> teardowns =
            ThreadLocal.withInitial(ArrayDeque::new);

    /** Register a teardown action to run at end of test. */
    public static void register(Runnable teardown) {
        teardowns.get().push(teardown);   // push = LIFO
    }

    /** Run all registered teardowns for this thread, then clear the registry. */
    public static void cleanup() {
        Deque<Runnable> actions = teardowns.get();
        int count = actions.size();
        while (!actions.isEmpty()) {
            try {
                actions.pop().run();
            } catch (Exception e) {
                log.warn("Teardown action failed — continuing cleanup", e);
            }
        }
        teardowns.remove();
        log.debug("TestDataRegistry: ran {} teardown actions", count);
    }
}
