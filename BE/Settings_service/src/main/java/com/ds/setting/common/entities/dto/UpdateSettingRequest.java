package com.ds.setting.common.entities.dto;

import com.ds.setting.app_context.models.SystemSetting.DisplayMode;
import com.ds.setting.app_context.models.SystemSetting.SettingType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for updating a setting
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateSettingRequest {
    
    private String description;
    private SettingType type;
    private String value;
    private DisplayMode displayMode;
}
