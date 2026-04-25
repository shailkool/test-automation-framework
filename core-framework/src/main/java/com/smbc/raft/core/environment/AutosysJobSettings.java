package com.smbc.raft.core.environment;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
import java.util.Map;

/**
 * Configuration for AutoSys job monitoring in a named environment.
 * Declared under the "autosys" key in the environment JSON.
 *
 * Supports three query modes: CLI (autorep), REST API, or direct DB.
 * Set "queryMode" to "CLI", "REST", or "DB" to select.
 */
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AutosysJobSettings {

    /** How to query AutoSys: CLI | REST | DB */
    private String queryMode = "CLI";

    // ── CLI mode ──────────────────────────────────────────────────────────
    /** Full path to autorep binary, e.g. "/opt/autosys/bin/autorep" */
    private String autorepPath = "autorep";

    // ── REST mode ─────────────────────────────────────────────────────────
    /** AutoSys REST API base URL, e.g. "https://autosys-wcc.internal:8443" */
    private String restBaseUrl;
    private String restUsername;
    private String restPassword;

    // ── DB mode ───────────────────────────────────────────────────────────
    /** Named database key in this environment's "databases" map */
    private String databaseKey;

    /** Jobs to check — each entry is a named group of jobs to monitor */
    private Map<String, AutosysJobGroup> jobGroups;

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AutosysJobGroup {
        private String description;
        /** Individual job names to check */
        private List<String> jobs;
        /**
         * Expected terminal status after the job runs.
         * SUCCESS (default) | FAILURE | INACTIVE | RUNNING | ON_HOLD | ON_ICE
         */
        private String expectedStatus = "SUCCESS";
        /**
         * If true, a non-SUCCESS status is a hard failure.
         * If false, it is a warning (DEGRADED) that does not fail the suite.
         */
        private boolean critical = true;
    }
}
