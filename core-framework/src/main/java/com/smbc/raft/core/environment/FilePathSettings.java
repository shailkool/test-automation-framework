package com.smbc.raft.core.environment;

import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Configuration for a file system path health check. */
@Data
@NoArgsConstructor
public class FilePathSettings {
  private String path;
  private List<String> expectedFormats;
  private int maxAgeMinutes;
  private int minFileCount;
}
