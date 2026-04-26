package com.smbc.raft.core.runprofile;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Filesystem locations used by a run for emitted artefacts.
 *
 * <p>Each field is optional; callers fall back to sensible defaults if a given profile doesn't set
 * a value.
 */
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class OutputDirs {

  private String reports;
  private String logs;
  private String screenshots;
  private String artifacts;
}
