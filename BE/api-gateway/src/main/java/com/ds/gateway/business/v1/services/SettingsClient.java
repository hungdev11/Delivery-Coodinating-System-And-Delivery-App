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
     * Get setting value by group and key from Settings Service
     * Uses the new group/key pair API: GET /{group}/{key}/value
     */
    public String getSettingValue(String group, String key) {
        try {
            String url = settingsServiceUrl + "/api/v1/settings/" + group + "/" + key + "/value";
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            
            if (response != null && response.containsKey("result")) {
                return (String) response.get("result");
            }
            
            log.debug("[api-gateway] [SettingsClient.getSettingValue] Setting {}/{} not found or has no value", group, key);
            return null;
        } catch (Exception e) {
            log.error("[api-gateway] [SettingsClient.getSettingValue] Failed to fetch setting {}/{}", group, key, e);
            return null;
        }
    }
    
    /**
     * Get setting value by key from Settings Service (legacy method for backward compatibility)
     * Assumes group is "keycloak" for Keycloak-related settings
     */
    public String getSettingValue(String key) {
        return getSettingValue("keycloak", key);
    }
    
    /**
     * Get default realm from Settings Service
     * Uses group "keycloak" and key "KEYCLOAK_DEFAULT_REALM"
     */
    public String getDefaultRealm() {
        return getSettingValue("keycloak", "KEYCLOAK_DEFAULT_REALM");
    }
    
    /**
     * Get default client ID from Settings Service
     * Uses group "keycloak" and key "KEYCLOAK_DEFAULT_CLIENT_ID"
     */
    public String getDefaultClientId() {
        return getSettingValue("keycloak", "KEYCLOAK_DEFAULT_CLIENT_ID");
    }

    /**
     * Get client secret for a given clientId from Settings Service
     * Looks up KEY: KEYCLOAK_CLIENT_<CLIENT_ID>_SECRET (clientId hyphens replaced with underscores, uppercased)
     * Uses group "keycloak"
     */
    public String getClientSecretForClientId(String clientId) {
        if (clientId == null || clientId.isBlank()) {
            return null;
        }
        String clientKey = clientId.replace("-", "_").toUpperCase();
        return getSettingValue("keycloak", "KEYCLOAK_CLIENT_" + clientKey + "_SECRET");
    }
}
