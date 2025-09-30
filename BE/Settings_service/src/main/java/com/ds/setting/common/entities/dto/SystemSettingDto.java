package com.ds.setting.common.entities.dto;

import com.ds.setting.app_context.models.SystemSetting.DisplayMode;
import com.ds.setting.app_context.models.SystemSetting.SettingLevel;
import com.ds.setting.app_context.models.SystemSetting.SettingType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for System Setting
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemSettingDto {
    private String key;
    private String group;
    private String description;
    private SettingType type;
    private String value;
    private SettingLevel level;
    private Boolean isReadOnly;
    private DisplayMode displayMode;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String updatedBy;
}
