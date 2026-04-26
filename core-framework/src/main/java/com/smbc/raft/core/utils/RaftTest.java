package com.smbc.raft.core.utils;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Meta-annotation for RAFT test methods and classes.
 *
 * <p>Reduces boilerplate by bundling common TestNG configurations into a single, descriptive
 * annotation. This ensures consistency in tagging, timeouts, and retry policies across multiple
 * teams using the framework.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface RaftTest {

  /** TestNG groups/categories (e.g., {"smoke", "regression"}). */
  String[] categories() default {};

  /** Maximum execution time for the test method in milliseconds. Overrides global default. */
  long timeout() default -1;

  /** Whether to enable automatic flake retries for this test. */
  boolean retry() default true;

  /** Descriptive name of the test for reporting purposes. */
  String value() default "";
}
