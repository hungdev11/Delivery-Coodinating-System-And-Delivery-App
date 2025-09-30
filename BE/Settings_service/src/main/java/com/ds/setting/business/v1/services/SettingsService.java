package com.ds.setting.business.v1.services;

import com.ds.setting.app_context.models.SystemSetting;
import com.ds.setting.app_context.models.SystemSetting.SettingLevel;
import com.ds.setting.app_context.repositories.SystemSettingRepository;
import com.ds.setting.common.entities.dto.CreateSettingRequest;
import com.ds.setting.common.entities.dto.SystemSettingDto;
import com.ds.setting.common.entities.dto.UpdateSettingRequest;
import com.ds.setting.common.exceptions.ReadOnlySettingException;
import com.ds.setting.common.exceptions.SettingNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for managing system settings
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SettingsService {

    private final SystemSettingRepository settingRepository;

    /**
     * Get setting by key
     */
    @Cacheable(value = "settings", key = "#key")
    public SystemSettingDto getByKey(String key) {
        SystemSetting setting = settingRepository.findById(key)
                .orElseThrow(() -> new SettingNotFoundException("Setting not found: " + key));
        return toDto(setting);
    }

    /**
     * Get setting value by key (convenient method)
     */
    @Cacheable(value = "settings", key = "'value_' + #key")
    public String getValue(String key) {
        return getByKey(key).getValue();
    }

    /**
     * Get setting value with default
     */
    public String getValue(String key, String defaultValue) {
        try {
            return getValue(key);
        } catch (SettingNotFoundException e) {
            return defaultValue;
        }
    }

    /**
     * Get all settings by group
     */
    @Cacheable(value = "settingsByGroup", key = "#group")
    public List<SystemSettingDto> getByGroup(String group) {
        return settingRepository.findByGroup(group).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Get all settings by level
     */
    public List<SystemSettingDto> getByLevel(SettingLevel level) {
        return settingRepository.findByLevel(level).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Get all settings
     */
    public List<SystemSettingDto> getAllSettings() {
        return settingRepository.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Search settings
     */
    public List<SystemSettingDto> searchSettings(String search) {
        return settingRepository.searchSettings(search).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Create a new setting
     */
    @Transactional
    @CacheEvict(value = {"settings", "settingsByGroup"}, allEntries = true)
    public SystemSettingDto createSetting(CreateSettingRequest request) {
        // Check if setting already exists
        if (settingRepository.existsById(request.getKey())) {
            throw new IllegalArgumentException("Setting already exists: " + request.getKey());
        }

        SystemSetting setting = SystemSetting.builder()
                .key(request.getKey())
                .group(request.getGroup())
                .description(request.getDescription())
                .type(request.getType())
                .value(request.getValue())
                .level(request.getLevel())
                .isReadOnly(request.getIsReadOnly())
                .displayMode(request.getDisplayMode())
                .build();

        SystemSetting saved = settingRepository.save(setting);
        log.info("Created setting: key={}, group={}, level={}", saved.getKey(), saved.getGroup(), saved.getLevel());

        return toDto(saved);
    }

    /**
     * Update a setting
     */
    @Transactional
    @CacheEvict(value = {"settings", "settingsByGroup"}, allEntries = true)
    public SystemSettingDto updateSetting(String key, UpdateSettingRequest request, String updatedBy) {
        SystemSetting setting = settingRepository.findById(key)
                .orElseThrow(() -> new SettingNotFoundException("Setting not found: " + key));

        // Check if read-only
        if (Boolean.TRUE.equals(setting.getIsReadOnly())) {
            throw new ReadOnlySettingException("Cannot update read-only setting: " + key);
        }

        // Update fields if provided
        if (request.getDescription() != null) {
            setting.setDescription(request.getDescription());
        }

        if (request.getType() != null) {
            setting.setType(request.getType());
        }

        if (request.getValue() != null) {
            setting.setValue(request.getValue());
        }

        if (request.getDisplayMode() != null) {
            setting.setDisplayMode(request.getDisplayMode());
        }

        setting.setUpdatedBy(updatedBy);

        SystemSetting updated = settingRepository.save(setting);
        log.info("Updated setting: key={}, updatedBy={}", key, updatedBy);

        return toDto(updated);
    }

    /**
     * Delete a setting
     */
    @Transactional
    @CacheEvict(value = {"settings", "settingsByGroup"}, allEntries = true)
    public void deleteSetting(String key) {
        SystemSetting setting = settingRepository.findById(key)
                .orElseThrow(() -> new SettingNotFoundException("Setting not found: " + key));

        // Check if read-only
        if (Boolean.TRUE.equals(setting.getIsReadOnly())) {
            throw new ReadOnlySettingException("Cannot delete read-only setting: " + key);
        }

        settingRepository.delete(setting);
        log.info("Deleted setting: key={}", key);
    }

    /**
     * Convert entity to DTO
     */
    private SystemSettingDto toDto(SystemSetting setting) {
        return SystemSettingDto.builder()
                .key(setting.getKey())
                .group(setting.getGroup())
                .description(setting.getDescription())
                .type(setting.getType())
                .value(setting.getValue())
                .level(setting.getLevel())
                .isReadOnly(setting.getIsReadOnly())
                .displayMode(setting.getDisplayMode())
                .createdAt(setting.getCreatedAt())
                .updatedAt(setting.getUpdatedAt())
                .updatedBy(setting.getUpdatedBy())
                .build();
    }
}
