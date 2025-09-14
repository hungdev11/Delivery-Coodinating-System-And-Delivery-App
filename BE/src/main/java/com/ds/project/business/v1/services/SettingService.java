package com.ds.project.business.v1.services;

import com.ds.project.app_context.models.Setting;
import com.ds.project.app_context.repositories.SettingRepository;
import com.ds.project.common.entities.base.BaseResponse;
import com.ds.project.common.entities.base.Page;
import com.ds.project.common.entities.base.PagedData;
import com.ds.project.common.entities.dto.SettingDto;
import com.ds.project.common.entities.dto.request.SettingRequest;
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
    public BaseResponse<SettingDto> createSetting(SettingRequest settingRequest) {
        try {
            log.info("Creating setting: {}", settingRequest.getKey());
            
            // Check if setting already exists
            if (settingRepository.existsByKey(settingRequest.getKey())) {
                return BaseResponse.<SettingDto>builder()
                    .message(Optional.of("Setting with key " + settingRequest.getKey() + " already exists"))
                    .build();
            }
            
            Setting setting = settingMapper.mapToEntity(settingRequest);
            
            Setting savedSetting = settingRepository.save(setting);
            log.info("Successfully created setting: {}", savedSetting.getKey());
            
            SettingDto settingDto = settingMapper.mapToDto(savedSetting);
            return BaseResponse.<SettingDto>builder()
                .result(Optional.of(settingDto))
                .build();
        } catch (Exception e) {
            log.error("Error creating setting: {}", e.getMessage(), e);
            return BaseResponse.<SettingDto>builder()
                .message(Optional.of("Failed to create setting: " + e.getMessage()))
                .build();
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public Optional<BaseResponse<SettingDto>> getSettingById(String id) {
        try {
            log.info("Getting setting by ID: {}", id);
            return settingRepository.findById(id)
                .filter(setting -> !setting.getDeleted())
                .map(setting -> {
                    SettingDto settingDto = settingMapper.mapToDto(setting);
                    return BaseResponse.<SettingDto>builder()
                        .result(Optional.of(settingDto))
                        .build();
                });
        } catch (Exception e) {
            log.error("Error getting setting by id {}: {}", id, e.getMessage(), e);
            return Optional.of(BaseResponse.<SettingDto>builder()
                .message(Optional.of("Failed to get setting: " + e.getMessage()))
                .build());
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public Optional<BaseResponse<SettingDto>> getSettingByKey(String key) {
        try {
            log.info("Getting setting by key: {}", key);
            return settingRepository.findByKey(key)
                .filter(setting -> !setting.getDeleted())
                .map(setting -> {
                    SettingDto settingDto = settingMapper.mapToDto(setting);
                    return BaseResponse.<SettingDto>builder()
                        .result(Optional.of(settingDto))
                        .build();
                });
        } catch (Exception e) {
            log.error("Error getting setting by key {}: {}", key, e.getMessage(), e);
            return Optional.of(BaseResponse.<SettingDto>builder()
                .message(Optional.of("Failed to get setting: " + e.getMessage()))
                .build());
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<BaseResponse<SettingDto>> getSettingsByGroup(String group) {
        try {
            log.info("Getting settings by group: {}", group);
            return settingRepository.findByGroup(group).stream()
                .filter(setting -> !setting.getDeleted())
                .map(setting -> {
                    SettingDto settingDto = settingMapper.mapToDto(setting);
                    return BaseResponse.<SettingDto>builder()
                        .result(Optional.of(settingDto))
                        .build();
                })
                .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error getting settings by group {}: {}", group, e.getMessage(), e);
            return List.of(BaseResponse.<SettingDto>builder()
                .message(Optional.of("Failed to get settings: " + e.getMessage()))
                .build());
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public BaseResponse<PagedData<Page, SettingDto>> getAllSettings() {
        try {
            log.info("Getting all settings");
            List<Setting> settings = settingRepository.findAll().stream()
                .filter(setting -> !setting.getDeleted())
                .collect(Collectors.toList());
            
            List<SettingDto> settingDtos = settings.stream()
                .map(settingMapper::mapToDto)
                .collect(Collectors.toList());
            
            Page page = Page.builder()
                .page(0)
                .size(settingDtos.size())
                .totalElements((long) settingDtos.size())
                .totalPages(1)
                .build();
            
            PagedData<Page, SettingDto> pagedData = PagedData.<Page, SettingDto>builder()
                .data(settingDtos)
                .page(page)
                .build();
            
            return BaseResponse.<PagedData<Page, SettingDto>>builder()
                .result(Optional.of(pagedData))
                .build();
        } catch (Exception e) {
            log.error("Error getting all settings: {}", e.getMessage(), e);
            return BaseResponse.<PagedData<Page, SettingDto>>builder()
                .message(Optional.of("Failed to get settings: " + e.getMessage()))
                .build();
        }
    }
    
    @Override
    public BaseResponse<SettingDto> updateSetting(String id, SettingRequest settingRequest) {
        try {
            log.info("Updating setting: {}", id);
            
            Setting existingSetting = settingRepository.findById(id)
                .filter(setting -> !setting.getDeleted())
                .orElse(null);
            
            if (existingSetting == null) {
                return BaseResponse.<SettingDto>builder()
                    .message(Optional.of("Setting not found with id: " + id))
                    .build();
            }
            
            // Check if new key conflicts with existing setting
            if (!existingSetting.getKey().equals(settingRequest.getKey()) && 
                settingRepository.existsByKey(settingRequest.getKey())) {
                return BaseResponse.<SettingDto>builder()
                    .message(Optional.of("Setting with key " + settingRequest.getKey() + " already exists"))
                    .build();
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
            
            SettingDto settingDto = settingMapper.mapToDto(updatedSetting);
            return BaseResponse.<SettingDto>builder()
                .result(Optional.of(settingDto))
                .build();
        } catch (Exception e) {
            log.error("Error updating setting {}: {}", id, e.getMessage(), e);
            return BaseResponse.<SettingDto>builder()
                .message(Optional.of("Failed to update setting: " + e.getMessage()))
                .build();
        }
    }
    
    @Override
    public BaseResponse<SettingDto> updateSettingByKey(String key, String value) {
        try {
            log.info("Updating setting by key: {}", key);
            
            Setting setting = settingRepository.findByKey(key)
                .filter(s -> !s.getDeleted())
                .orElse(null);
            
            if (setting == null) {
                return BaseResponse.<SettingDto>builder()
                    .message(Optional.of("Setting not found with key: " + key))
                    .build();
            }
            
            setting.setValue(value);
            setting.setUpdatedAt(LocalDateTime.now());
            
            Setting updatedSetting = settingRepository.save(setting);
            log.info("Successfully updated setting by key: {}", updatedSetting.getKey());
            
            SettingDto settingDto = settingMapper.mapToDto(updatedSetting);
            return BaseResponse.<SettingDto>builder()
                .result(Optional.of(settingDto))
                .build();
        } catch (Exception e) {
            log.error("Error updating setting by key {}: {}", key, e.getMessage(), e);
            return BaseResponse.<SettingDto>builder()
                .message(Optional.of("Failed to update setting: " + e.getMessage()))
                .build();
        }
    }
    
    @Override
    public void deleteSetting(String id) {
        try {
            log.info("Soft deleting setting: {}", id);
            
            Setting setting = settingRepository.findById(id)
                .filter(s -> !s.getDeleted())
                .orElse(null);
            
            if (setting == null) {
                log.warn("Setting not found with id: {}", id);
                return;
            }
            
            setting.setDeleted(true);
            setting.setUpdatedAt(LocalDateTime.now());
            settingRepository.save(setting);
            
            log.info("Successfully soft deleted setting: {}", setting.getKey());
        } catch (Exception e) {
            log.error("Error deleting setting {}: {}", id, e.getMessage(), e);
        }
    }
    
    @Override
    public void deleteSettingByKey(String key) {
        try {
            log.info("Soft deleting setting by key: {}", key);
            
            Setting setting = settingRepository.findByKey(key)
                .filter(s -> !s.getDeleted())
                .orElse(null);
            
            if (setting == null) {
                log.warn("Setting not found with key: {}", key);
                return;
            }
            
            setting.setDeleted(true);
            setting.setUpdatedAt(LocalDateTime.now());
            settingRepository.save(setting);
            
            log.info("Successfully soft deleted setting by key: {}", setting.getKey());
        } catch (Exception e) {
            log.error("Error deleting setting by key {}: {}", key, e.getMessage(), e);
        }
    }
    
}
