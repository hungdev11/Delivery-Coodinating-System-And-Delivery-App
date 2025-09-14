package com.ds.project.common.mapper;

import com.ds.project.app_context.models.Setting;
import com.ds.project.common.entities.dto.SettingDto;
import com.ds.project.common.entities.dto.request.SettingRequest;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Mapper for Setting entity and DTOs
 */
@Component
public class SettingMapper {
    
    /**
     * Maps SettingRequest to Setting entity
     */
    public Setting mapToEntity(SettingRequest settingRequest) {
        return Setting.builder()
            .key(settingRequest.getKey())
            .group(settingRequest.getGroup())
            .value(settingRequest.getValue())
            .type(settingRequest.getType())
            .description(settingRequest.getDescription())
            .level(settingRequest.getLevel())
            .deleted(false)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();
    }
    
    /**
     * Maps Setting entity to SettingDto
     */
    public SettingDto mapToDto(Setting setting) {
        return SettingDto.builder()
            .id(setting.getId())
            .key(setting.getKey())
            .group(setting.getGroup())
            .value(setting.getValue())
            .type(setting.getType())
            .description(setting.getDescription())
            .level(setting.getLevel())
            .createdAt(setting.getCreatedAt().toString())
            .updatedAt(setting.getUpdatedAt().toString())
            .build();
    }
}
