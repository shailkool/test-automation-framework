package com.smbc.raft.core.environment;

import lombok.Data;
import lombok.NoArgsConstructor;

/** Configuration for a Windows service health check. */
@Data
@NoArgsConstructor
public class ServiceSettings {
  private String displayName;
  private String name;
  private String host;
}
