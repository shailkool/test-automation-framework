package com.smbc.raft.core.runprofile;

import java.util.Locale;

/** Controls when a browser step should emit a screenshot. */
public enum ScreenshotMode {

  /** Never capture screenshots. */
  OFF,

  /** Capture a screenshot only when the scenario ends in failure. */
  ON_FAILURE,

  /** Capture a screenshot after every browser step plus on failure. */
  EACH_STEP;

  /**
   * Parse a user-supplied mode name (case-insensitive, tolerant of hyphens and spaces). Returns
   * {@link #ON_FAILURE} for unknown or blank input so the framework defaults to the cheaper
   * behaviour.
   */
  public static ScreenshotMode parse(String raw) {
    if (raw == null || raw.isBlank()) {
      return ON_FAILURE;
    }
    String normalised = raw.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
    switch (normalised) {
      case "OFF":
      case "NONE":
      case "NEVER":
        return OFF;
      case "EACH_STEP":
      case "EVERY_STEP":
      case "ALL":
      case "ALWAYS":
        return EACH_STEP;
      case "ON_FAILURE":
      case "FAILURE":
      case "ONLY_ON_FAILURE":
      case "FAIL":
        return ON_FAILURE;
      default:
        return ON_FAILURE;
    }
  }
}
