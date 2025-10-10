package com.ds.project.business.v1.services;

import com.ds.project.app_context.models.Setting;
import com.ds.project.app_context.repositories.SettingRepository;
import com.ds.project.common.entities.dto.request.SettingRequest;
import com.ds.project.common.entities.dto.response.SettingResponse;
import com.ds.project.common.interfaces.ISettingService;
import com.ds.project.common.mapper.SettingMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Setting service implementation
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class SettingService implements ISettingService {
    
    private final SettingRepository settingRepository;
    private final SettingMapper settingMapper;
    
    @Override
    public SettingResponse createSetting(SettingRequest settingRequest) {
        log.info("Creating setting: {}", settingRequest.getKey());
        
        // Check if setting already exists
        if (settingRepository.existsByKey(settingRequest.getKey())) {
            throw new IllegalArgumentException("Setting with key " + settingRequest.getKey() + " already exists");
        }
        
        Setting setting = settingMapper.mapToEntity(settingRequest);
        
        Setting savedSetting = settingRepository.save(setting);
        log.info("Successfully created setting: {}", savedSetting.getKey());
        
        return settingMapper.mapToResponse(savedSetting);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Optional<SettingResponse> getSettingById(String id) {
        log.info("Getting setting by ID: {}", id);
        return settingRepository.findById(id)
            .filter(setting -> !setting.getDeleted())
            .map(settingMapper::mapToResponse);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Optional<SettingResponse> getSettingByKey(String key) {
        log.info("Getting setting by key: {}", key);
        return settingRepository.findByKey(key)
            .filter(setting -> !setting.getDeleted())
            .map(settingMapper::mapToResponse);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<SettingResponse> getSettingsByGroup(String group) {
        log.info("Getting settings by group: {}", group);
        return settingRepository.findByGroup(group).stream()
            .filter(setting -> !setting.getDeleted())
            .map(settingMapper::mapToResponse)
            .collect(Collectors.toList());
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<SettingResponse> getAllSettings() {
        log.info("Getting all settings");
        return settingRepository.findAll().stream()
            .filter(setting -> !setting.getDeleted())
            .map(settingMapper::mapToResponse)
            .collect(Collectors.toList());
    }
    
    @Override
    public SettingResponse updateSetting(String id, SettingRequest settingRequest) {
        log.info("Updating setting: {}", id);
        
        Setting existingSetting = settingRepository.findById(id)
            .filter(setting -> !setting.getDeleted())
            .orElseThrow(() -> new IllegalArgumentException("Setting not found with id: " + id));
        
        // Check if new key conflicts with existing setting
        if (!existingSetting.getKey().equals(settingRequest.getKey()) && 
            settingRepository.existsByKey(settingRequest.getKey())) {
            throw new IllegalArgumentException("Setting with key " + settingRequest.getKey() + " already exists");
        }
        
        existingSetting.setKey(settingRequest.getKey());
        existingSetting.setGroup(settingRequest.getGroup());
        existingSetting.setValue(settingRequest.getValue());
        existingSetting.setType(settingRequest.getType());
        existingSetting.setDescription(settingRequest.getDescription());
        existingSetting.setLevel(settingRequest.getLevel());
        existingSetting.setUpdatedAt(LocalDateTime.now());
        
        Setting updatedSetting = settingRepository.save(existingSetting);
        log.info("Successfully updated setting: {}", updatedSetting.getKey());
        
        return settingMapper.mapToResponse(updatedSetting);
    }
    
    @Override
    public SettingResponse updateSettingByKey(String key, String value) {
        log.info("Updating setting by key: {}", key);
        
        Setting setting = settingRepository.findByKey(key)
            .filter(s -> !s.getDeleted())
            .orElseThrow(() -> new IllegalArgumentException("Setting not found with key: " + key));
        
        setting.setValue(value);
        setting.setUpdatedAt(LocalDateTime.now());
        
        Setting updatedSetting = settingRepository.save(setting);
        log.info("Successfully updated setting by key: {}", updatedSetting.getKey());
        
        return settingMapper.mapToResponse(updatedSetting);
    }
    
    @Override
    public void deleteSetting(String id) {
        log.info("Soft deleting setting: {}", id);
        
        Setting setting = settingRepository.findById(id)
            .filter(s -> !s.getDeleted())
            .orElseThrow(() -> new IllegalArgumentException("Setting not found with id: " + id));
        
        setting.setDeleted(true);
        setting.setUpdatedAt(LocalDateTime.now());
        settingRepository.save(setting);
        
        log.info("Successfully soft deleted setting: {}", setting.getKey());
    }
    
    @Override
    public void deleteSettingByKey(String key) {
        log.info("Soft deleting setting by key: {}", key);
        
        Setting setting = settingRepository.findByKey(key)
            .filter(s -> !s.getDeleted())
            .orElseThrow(() -> new IllegalArgumentException("Setting not found with key: " + key));
        
        setting.setDeleted(true);
        setting.setUpdatedAt(LocalDateTime.now());
        settingRepository.save(setting);
        
        log.info("Successfully soft deleted setting by key: {}", setting.getKey());
    }
    
}
