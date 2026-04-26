package com.smbc.raft.core.health;

/**
 * AutoSys job status values as returned by autorep -j <name> -s. Maps to both the CLI single-letter
 * codes and the REST/DB string values.
 */
public enum AutosysJobStatus {
  SUCCESS("SU", "SUCCESS"),
  RUNNING("RU", "RUNNING"),
  FAILURE("FA", "FAILURE"),
  INACTIVE("IN", "INACTIVE"),
  ON_HOLD("OH", "ON_HOLD"),
  ON_ICE("OI", "ON_ICE"),
  STARTING("ST", "STARTING"),
  WAITING("WA", "WAITING"),
  ACTIVATED("AC", "ACTIVATED"),
  UNKNOWN("??", "UNKNOWN");

  private final String cliCode;
  private final String label;

  AutosysJobStatus(String cliCode, String label) {
    this.cliCode = cliCode;
    this.label = label;
  }

  public static AutosysJobStatus fromCliCode(String code) {
    if (code == null) return UNKNOWN;
    String upper = code.trim().toUpperCase();
    for (AutosysJobStatus s : values()) {
      if (s.cliCode.equals(upper)) return s;
    }
    // autorep also outputs the full word on some versions
    return fromLabel(upper);
  }

  public static AutosysJobStatus fromLabel(String label) {
    if (label == null) return UNKNOWN;
    String upper = label.trim().toUpperCase();
    for (AutosysJobStatus s : values()) {
      if (s.label.equals(upper) || s.name().equals(upper)) return s;
    }
    return UNKNOWN;
  }

  public boolean isTerminal() {
    return this == SUCCESS || this == FAILURE || this == ON_HOLD || this == ON_ICE;
  }

  public boolean isActive() {
    return this == RUNNING || this == STARTING || this == ACTIVATED || this == WAITING;
  }

  @Override
  public String toString() {
    return label;
  }
}
