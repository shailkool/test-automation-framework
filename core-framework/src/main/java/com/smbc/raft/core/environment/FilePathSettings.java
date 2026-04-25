package com.smbc.raft.core.environment;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Configuration for a file system path health check.
 */
@Data
@NoArgsConstructor
public class FilePathSettings {
    private String path;
    private List<String> expectedFormats;
    private int maxAgeMinutes;
    private int minFileCount;
}
