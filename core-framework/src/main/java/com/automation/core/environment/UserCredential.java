package com.automation.core.environment;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Generic user credential bundle for a named user of a website / API.
 */
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserCredential {

    private String username;
    private String password;
    private String email;
    private String role;
    private Map<String, Object> properties = new LinkedHashMap<>();
}
