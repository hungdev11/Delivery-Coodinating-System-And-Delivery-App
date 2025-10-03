package com.ds.gateway.business.v1.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Client for fetching settings from Settings Service
 */
@Slf4j
@Service
public class SettingsClient {
    
    @Value("${services.settings.base-url}")
    private String settingsServiceUrl;
    
    private final RestTemplate restTemplate;
    
    public SettingsClient() {
        this.restTemplate = new RestTemplate();
    }
    
    /**
     * Get setting value by key from Settings Service
     */
    public String getSettingValue(String key) {
        try {
            String url = settingsServiceUrl + "/api/v1/settings/" + key;
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            
            if (response != null && response.containsKey("value")) {
                return (String) response.get("value");
            }
            
            log.warn("Setting {} not found or has no value", key);
            return null;
        } catch (Exception e) {
            log.error("Failed to fetch setting {}: {}", key, e.getMessage());
            return null;
        }
    }
    
    /**
     * Get default realm from Settings Service
     */
    public String getDefaultRealm() {
        return getSettingValue("KEYCLOAK_DEFAULT_REALM");
    }
    
    /**
     * Get default client ID from Settings Service
     */
    public String getDefaultClientId() {
        return getSettingValue("KEYCLOAK_DEFAULT_CLIENT_ID");
    }

    /**
     * Get client secret for a given clientId from Settings Service
     * Looks up KEY: KEYCLOAK_CLIENT_<CLIENT_ID>_SECRET (clientId hyphens replaced with underscores, uppercased)
     */
    public String getClientSecretForClientId(String clientId) {
        if (clientId == null || clientId.isBlank()) {
            return null;
        }
        String clientKey = clientId.replace("-", "_").toUpperCase();
        return getSettingValue("KEYCLOAK_CLIENT_" + clientKey + "_SECRET");
    }
}
