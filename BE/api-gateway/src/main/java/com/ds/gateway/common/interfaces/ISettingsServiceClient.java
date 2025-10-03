package com.ds.gateway.common.interfaces;

import com.ds.gateway.common.entities.dto.settings.SystemSettingDto;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Interface for Settings Service REST client
 * Defines contract for calling Settings microservice
 */
public interface ISettingsServiceClient {
    
    /**
     * Get setting by key
     */
    CompletableFuture<SystemSettingDto> getSettingByKey(String key);
    
    /**
     * Get setting value by key
     */
    CompletableFuture<String> getSettingValue(String key);
    
    /**
     * Get setting value by key with default value
     */
    CompletableFuture<String> getSettingValue(String key, String defaultValue);
    
    /**
     * Get all settings by group
     */
    CompletableFuture<List<SystemSettingDto>> getSettingsByGroup(String group);
    
    /**
     * Get all Keycloak settings
     */
    CompletableFuture<List<SystemSettingDto>> getAllKeycloakSettings();
}
