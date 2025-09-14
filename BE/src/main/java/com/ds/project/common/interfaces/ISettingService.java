package com.ds.project.common.interfaces;

import com.ds.project.common.entities.base.BaseResponse;
import com.ds.project.common.entities.base.Page;
import com.ds.project.common.entities.base.PagedData;
import com.ds.project.common.entities.dto.SettingDto;
import com.ds.project.common.entities.dto.request.SettingRequest;

import java.util.List;
import java.util.Optional;

/**
 * Setting service interface
 */
public interface ISettingService {
    
    /**
     * Create a new setting
     */
    BaseResponse<SettingDto> createSetting(SettingRequest settingRequest);
    
    /**
     * Get setting by ID
     */
    Optional<BaseResponse<SettingDto>> getSettingById(String id);
    
    /**
     * Get setting by key
     */
    Optional<BaseResponse<SettingDto>> getSettingByKey(String key);
    
    /**
     * Get settings by group
     */
    List<BaseResponse<SettingDto>> getSettingsByGroup(String group);
    
    /**
     * Get all settings
     */
    BaseResponse<PagedData<Page, SettingDto>> getAllSettings();
    
    /**
     * Update setting
     */
    BaseResponse<SettingDto> updateSetting(String id, SettingRequest settingRequest);
    
    /**
     * Update setting by key
     */
    BaseResponse<SettingDto> updateSettingByKey(String key, String value);
    
    /**
     * Delete setting (soft delete)
     */
    void deleteSetting(String id);
    
    /**
     * Delete setting by key (soft delete)
     */
    void deleteSettingByKey(String key);
}
