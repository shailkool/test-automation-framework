package com.smbc.raft.core.utils;

import java.util.ArrayList;
import java.util.List;
import lombok.extern.log4j.Log4j2;
import org.testng.IMethodInstance;
import org.testng.IMethodInterceptor;
import org.testng.ITestContext;

/**
 * TestNG Method Interceptor that enables dynamic test sharding at runtime.
 *
 * <p>Usage: Set the system properties {@code shardIndex} and {@code shardTotal}. For example, to
 * run the first shard of a 3-way split: {@code mvn test -DshardIndex=0 -DshardTotal=3}
 *
 * <p>This allows the same TestNG XML suite to be distributed across multiple Docker containers or
 * CI nodes without manual XML partitioning.
 */
@Log4j2
public class ShardingListener implements IMethodInterceptor {

  @Override
  public List<IMethodInstance> intercept(List<IMethodInstance> methods, ITestContext context) {
    String indexStr = System.getProperty("shardIndex");
    String totalStr = System.getProperty("shardTotal");

    if (indexStr == null || totalStr == null) {
      log.debug(
          "Sharding not enabled (shardIndex/shardTotal not set). Running all {} methods.",
          methods.size());
      return methods;
    }

    try {
      int shardIndex = Integer.parseInt(indexStr);
      int shardTotal = Integer.parseInt(totalStr);

      if (shardTotal <= 0 || shardIndex < 0 || shardIndex >= shardTotal) {
        log.error(
            "Invalid sharding parameters: index={}, total={}. Falling back to all tests.",
            shardIndex,
            shardTotal);
        return methods;
      }

      List<IMethodInstance> result = new ArrayList<>();
      for (int i = 0; i < methods.size(); i++) {
        // Round-robin distribution of test methods across shards
        if (i % shardTotal == shardIndex) {
          result.add(methods.get(i));
        }
      }

      log.info(
          "Dynamic Sharding: Running {}/{} methods in shard {}/{}",
          result.size(),
          methods.size(),
          shardIndex + 1,
          shardTotal);

      return result;

    } catch (NumberFormatException e) {
      log.error("Failed to parse sharding parameters. Falling back to all tests.", e);
      return methods;
    }
  }
}
