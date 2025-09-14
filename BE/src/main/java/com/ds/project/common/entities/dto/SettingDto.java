package com.ds.project.common.entities.dto;

import com.ds.project.app_context.models.Setting;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Setting Data Transfer Object
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SettingDto {
    private String id;
    private String key;
    private String group;
    private String value;
    private Setting.SettingType type;
    private String description;
    private Setting.SettingLevel level;
    private String createdAt;
    private String updatedAt;
}
