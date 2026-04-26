package com.smbc.raft.core.utils;

import com.smbc.raft.core.retry.FlakeRetryAnalyzer;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import org.testng.IAnnotationTransformer;
import org.testng.annotations.ITestAnnotation;

/**
 * TestNG Annotation Transformer that interprets @RaftTest meta-annotations.
 *
 * <p>It dynamically updates TestNG @Test attributes at runtime based on the values provided in
 * the @RaftTest annotation, effectively allowing it to act as a simplified wrapper for complex test
 * configurations.
 */
public class RaftTestTransformer implements IAnnotationTransformer {

  @Override
  public void transform(
      ITestAnnotation annotation, Class testClass, Constructor testConstructor, Method testMethod) {

    RaftTest raftTest = null;

    // Check method level first, then class level
    if (testMethod != null && testMethod.isAnnotationPresent(RaftTest.class)) {
      raftTest = testMethod.getAnnotation(RaftTest.class);
    } else if (testClass != null && testClass.isAnnotationPresent(RaftTest.class)) {
      raftTest = (RaftTest) testClass.getAnnotation(RaftTest.class);
    }

    if (raftTest != null) {
      // Apply Timeout
      if (raftTest.timeout() != -1) {
        annotation.setTimeOut(raftTest.timeout());
      }

      // Apply Groups/Categories
      if (raftTest.categories().length > 0) {
        annotation.setGroups(raftTest.categories());
      }

      // Apply Retry Analyzer
      if (raftTest.retry()) {
        annotation.setRetryAnalyzer(FlakeRetryAnalyzer.class);
      }

      // Set description if provided
      if (!raftTest.value().isEmpty()) {
        annotation.setDescription(raftTest.value());
      }
    }
  }
}
