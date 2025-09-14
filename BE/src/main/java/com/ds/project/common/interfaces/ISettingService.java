package com.ds.project.common.interfaces;

import com.ds.project.common.entities.dto.request.SettingRequest;
import com.ds.project.common.entities.dto.response.SettingResponse;

import java.util.List;
import java.util.Optional;

/**
 * Setting service interface
 */
public interface ISettingService {
    
    /**
     * Create a new setting
     */
    SettingResponse createSetting(SettingRequest settingRequest);
    
    /**
     * Get setting by ID
     */
    Optional<SettingResponse> getSettingById(String id);
    
    /**
     * Get setting by key
     */
    Optional<SettingResponse> getSettingByKey(String key);
    
    /**
     * Get settings by group
     */
    List<SettingResponse> getSettingsByGroup(String group);
    
    /**
     * Get all settings
     */
    List<SettingResponse> getAllSettings();
    
    /**
     * Update setting
     */
    SettingResponse updateSetting(String id, SettingRequest settingRequest);
    
    /**
     * Update setting by key
     */
    SettingResponse updateSettingByKey(String key, String value);
    
    /**
     * Delete setting (soft delete)
     */
    void deleteSetting(String id);
    
    /**
     * Delete setting by key (soft delete)
     */
    void deleteSettingByKey(String key);
}
