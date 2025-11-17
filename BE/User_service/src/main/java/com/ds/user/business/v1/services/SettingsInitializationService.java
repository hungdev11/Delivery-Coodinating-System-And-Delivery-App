package com.ds.user.business.v1.services;

import com.ds.user.application.startup.data.KeycloakInitConfig;
import com.ds.user.common.interfaces.ISettingsInitializationService;
import com.ds.user.common.interfaces.ISettingsReaderService;
import com.ds.user.common.interfaces.ISettingsWriterService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import jakarta.annotation.PostConstruct;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
public class SettingsInitializationService implements ISettingsInitializationService, ISettingsWriterService, ISettingsReaderService {

    @Value("${SETTINGS_SERVICE_URL:http://localhost:21502}")
    private String settingsServiceUrl;

    @Value("${keycloak.auth-server-url}")
    private String keycloakAuthServerUrl;

    @Value("${keycloak.realm}")
    private String keycloakRealm;
    
    @Value("${user.settings.service.max-retries:10}")
    private int maxRetries;
    
    @Value("${user.settings.service.retry-delay-seconds:10}")
    private int retryDelaySeconds;

    private final KeycloakInitConfig initConfig;
    private final RestTemplate restTemplate;
    
    public SettingsInitializationService(KeycloakInitConfig initConfig) {
        this.initConfig = initConfig;
        this.restTemplate = createRestTemplateWithTimeout();
    }
    
    /**
     * Initialize @Value fields after dependency injection
     */
    @PostConstruct
    public void init() {
        log.info("SettingsInitializationService initialized with:");
        log.info("  - Settings Service URL: {}", settingsServiceUrl);
        log.info("  - Max Retries: {}", maxRetries);
        log.info("  - Retry Delay: {} seconds", retryDelaySeconds);
    }
    
    /**
     * Create RestTemplate with timeout configuration for health checks
     */
    private RestTemplate createRestTemplateWithTimeout() {
        RestTemplate template = new RestTemplate();
        
        // Set timeout for health checks (5 seconds)
        org.springframework.http.client.SimpleClientHttpRequestFactory factory = 
            new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000); // 5 seconds
        factory.setReadTimeout(5000);    // 5 seconds
        
        template.setRequestFactory(factory);
        return template;
    }

    @Override
    public void initialize() {
        log.info("Checking Keycloak Settings in Settings Service");
        
        // Wait for Settings Service to be available
        if (!waitForSettingsService()) {
            log.error("Settings Service is not available after maximum retry attempts. Continuing without settings initialization.");
            return;
        }
        
        try {
            if (isSettingsAlreadyInitialized()) {
                log.info("Keycloak settings already initialized - skipping");
            } else {
                log.info("Initializing Keycloak settings...");
                initializeKeycloakSettings();
                log.info("Keycloak settings initialized successfully");
            }
        } catch (Exception e) {
            log.warn("Failed to check/initialize settings: {}", e.getMessage());
        }
    }
    
    /**
     * Wait for Settings Service to be available with retry mechanism
     * @return true if Settings Service is available, false if max retries exceeded
     */
    private boolean waitForSettingsService() {
        
        log.info("Waiting for Settings Service to be available...");
        
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                log.info("Attempt {}/{}: Checking Settings Service availability at {}", 
                    attempt, maxRetries, settingsServiceUrl);
                
                // Try to ping the Settings Service - try multiple endpoints
                boolean isAvailable = false;
                
                // First try health endpoint
                try {
                    String healthUrl = settingsServiceUrl + "/actuator/health";
                    restTemplate.getForObject(healthUrl, Object.class);
                    isAvailable = true;
                } catch (Exception e1) {
                    // Fallback: try the main API endpoint
                    try {
                        String apiUrl = settingsServiceUrl + "/api/v1/settings";
                        restTemplate.getForObject(apiUrl, Object.class);
                        isAvailable = true;
                    } catch (Exception e2) {
                        // Last fallback: try root endpoint
                        try {
                            restTemplate.getForObject(settingsServiceUrl, Object.class);
                            isAvailable = true;
                        } catch (Exception e3) {
                            // All endpoints failed, continue with retry
                            throw e1; // Throw the first exception for logging
                        }
                    }
                }
                
                if (isAvailable) {
                    log.info("✓ Settings Service is available after {} attempts", attempt);
                    return true;
                }
                
            } catch (Exception e) {
                log.warn("Attempt {}/{} failed: Settings Service not available - {}", 
                    attempt, maxRetries, e.getMessage());
                
                if (attempt < maxRetries) {
                    log.info("Waiting {} seconds before next attempt...", retryDelaySeconds);
                    try {
                        Thread.sleep(retryDelaySeconds * 1000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        log.error("Interrupted while waiting for Settings Service");
                        return false;
                    }
                }
            }
        }
        
        log.error("Settings Service is not available after {} attempts with {}s delay", maxRetries, retryDelaySeconds);
        return false;
    }

    private boolean isSettingsAlreadyInitialized() {
        try {
            // Check by group instead of individual keys - more efficient
            String url = settingsServiceUrl + "/api/v1/settings/keycloak";
            List<?> response = restTemplate.getForObject(url, List.class);
            return response != null && !response.isEmpty();
        } catch (Exception e) {
            log.debug("No existing settings found or service not ready: {}", e.getMessage());
            return false;
        }
    }

    private void initializeKeycloakSettings() {
        // Build list of settings to bulk upsert
        List<Map<String, Object>> settingsToUpsert = new ArrayList<>();
        
        // Add base settings
        addSettingToList(settingsToUpsert, "KEYCLOAK_AUTH_SERVER_URL", "Keycloak authentication server URL", "STRING",
                keycloakAuthServerUrl, "SYSTEM", false, "URL");
        addSettingToList(settingsToUpsert, "KEYCLOAK_REALM", "Default Keycloak realm", "STRING", 
                keycloakRealm, "SYSTEM", true, "TEXT");
        addSettingToList(settingsToUpsert, "KEYCLOAK_MASTER_REALM", "Keycloak master realm for admin operations", "STRING", 
                "master", "SYSTEM", true, "TEXT");
        addSettingToList(settingsToUpsert, "KEYCLOAK_ADMIN_USERNAME", "Keycloak admin username", "STRING", 
                "dev", "SYSTEM", true, "TEXT");
        addSettingToList(settingsToUpsert, "KEYCLOAK_ADMIN_PASSWORD", "Keycloak admin password", "STRING", 
                "dev", "SYSTEM", true, "PASSWORD");
        addSettingToList(settingsToUpsert, "KEYCLOAK_DEFAULT_REALM", "Default realm for API Gateway authentication", "STRING", 
                initConfig.getDefaultConfig().getRealm(), "SYSTEM", true, "TEXT");
        addSettingToList(settingsToUpsert, "KEYCLOAK_DEFAULT_CLIENT_ID", "Default client ID for API Gateway authentication", "STRING", 
                initConfig.getDefaultConfig().getClientId(), "SYSTEM", true, "TEXT");

        // Add realm and client settings
        for (KeycloakInitConfig.RealmConfig realmConfig : initConfig.getRealms()) {
            String realmKey = realmConfig.getName().replace("-", "_").toUpperCase();
            addSettingToList(settingsToUpsert, "KEYCLOAK_REALM_" + realmKey, "Realm: " + realmConfig.getDisplayName(), "STRING",
                    realmConfig.getName(), "SYSTEM", true, "TEXT");

            for (KeycloakInitConfig.ClientConfig clientConfig : realmConfig.getClients()) {
                String clientKey = clientConfig.getClientId().replace("-", "_").toUpperCase();
                addSettingToList(settingsToUpsert, "KEYCLOAK_CLIENT_" + clientKey + "_ID", "Client ID for " + clientConfig.getName(),
                        "STRING", clientConfig.getClientId(), "SYSTEM", true, "TEXT");
            }
        }
        
        // Bulk upsert all settings at once
        bulkUpsertSettings("keycloak", settingsToUpsert);
    }
    
    private void addSettingToList(List<Map<String, Object>> list, String key, String description,
                                   String type, String value, String level, boolean isReadOnly, String displayMode) {
        if (value == null || value.trim().isEmpty()) {
            log.warn("  Skipping creation of '{}' - value is null or empty", key);
            return;
        }
        
        Map<String, Object> setting = new HashMap<>();
        setting.put("key", key);
        setting.put("group", "keycloak");
        setting.put("description", description);
        setting.put("type", type);
        setting.put("value", value.trim());
        setting.put("level", convertLevelToOrdinal(level));
        setting.put("isReadOnly", isReadOnly);
        setting.put("displayMode", displayMode);
        list.add(setting);
    }
    
    /**
     * Bulk upsert settings using the bulk endpoint
     */
    private void bulkUpsertSettings(String group, List<Map<String, Object>> settings) {
        if (settings.isEmpty()) {
            log.warn("No settings to upsert for group: {}", group);
            return;
        }
        
        try {
            String url = settingsServiceUrl + "/api/v1/settings/" + group + "/bulk";
            Map<String, Object> request = new HashMap<>();
            request.put("group", group);
            request.put("settings", settings);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-User-Id", "user-service-init");
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);
            
            restTemplate.put(url, entity);
            log.info("  ✓ Bulk upserted {} settings in group {}", settings.size(), group);
        } catch (Exception e) {
            log.error("  Failed to bulk upsert settings in group '{}': {}", group, e.getMessage(), e);
            // Fallback to individual upserts if bulk fails
            log.warn("  Falling back to individual upserts...");
            for (Map<String, Object> setting : settings) {
                createSetting(
                    (String) setting.get("key"),
                    group,
                    (String) setting.get("description"),
                    (String) setting.get("type"),
                    (String) setting.get("value"),
                    getLevelFromOrdinal((Integer) setting.get("level")),
                    (Boolean) setting.get("isReadOnly"),
                    (String) setting.get("displayMode")
                );
                }
            }
    }
    
    private String getLevelFromOrdinal(int ordinal) {
        switch (ordinal) {
            case 0: return "SYSTEM";
            case 1: return "APPLICATION";
            case 2: return "SERVICE";
            case 3: return "FEATURE";
            case 4: return "USER";
            default: return "SYSTEM";
        }
    }

    @Override
    public void createSetting(String key, String group, String description,
                              String type, String value, String level, boolean isReadOnly, String displayMode) {
        try {
            // Validate that value is not null or empty
            if (value == null || value.trim().isEmpty()) {
                log.warn("  Skipping creation of '{}' in group '{}' - value is null or empty", key, group);
                return;
            }
            
            // Use new upsert endpoint: PUT /{group}/{key}
            String url = settingsServiceUrl + "/api/v1/settings/" + group + "/" + key;
            Map<String, Object> request = new HashMap<>();
            request.put("key", key);
            request.put("group", group);
            request.put("description", description);
            request.put("type", type);
            request.put("value", value.trim()); // Trim whitespace
            // Convert level from string to integer (ORDINAL)
            request.put("level", convertLevelToOrdinal(level));
            request.put("isReadOnly", isReadOnly);
            request.put("displayMode", displayMode);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-User-Id", "user-service-init");
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);
            restTemplate.put(url, entity);
            log.info("  ✓ Created/Updated: {} in group {}", key, group);
        } catch (Exception e) {
            log.warn("  Failed to create/update '{}' in group '{}': {}", key, group, e.getMessage());
        }
    }
    
    /**
     * Convert level string to ordinal integer
     * SYSTEM(0), APPLICATION(1), SERVICE(2), FEATURE(3), USER(4)
     */
    private int convertLevelToOrdinal(String level) {
        switch (level.toUpperCase()) {
            case "SYSTEM": return 0;
            case "APPLICATION": return 1;
            case "SERVICE": return 2;
            case "FEATURE": return 3;
            case "USER": return 4;
            default: return 0; // Default to SYSTEM
        }
    }

    @Override
    public Optional<String> getSettingValue(String key, String group) {
        try {
            String url = settingsServiceUrl + "/api/v1/settings/" + group + "/" + key + "/value";
            String value = restTemplate.getForObject(url, String.class);
            return Optional.ofNullable(value);
        } catch (Exception e) {
            log.debug("Failed to get setting '{}' in group '{}': {}", key, group, e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public String getSettingValue(String key, String group, String defaultValue) {
        return getSettingValue(key, group).orElse(defaultValue);
    }

    @Override
    public boolean settingExists(String key, String group) {
        try {
            String url = settingsServiceUrl + "/api/v1/settings/" + group + "/" + key;
            restTemplate.getForObject(url, Object.class);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
