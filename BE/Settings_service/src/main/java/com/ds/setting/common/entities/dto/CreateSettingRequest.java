package com.ds.setting.common.entities.dto;

import com.ds.setting.app_context.models.SystemSetting.DisplayMode;
import com.ds.setting.app_context.models.SystemSetting.SettingLevel;
import com.ds.setting.app_context.models.SystemSetting.SettingType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for creating a new setting
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateSettingRequest {

    @NotBlank(message = "Key is required")
    private String key;

    @NotBlank(message = "Group is required")
    private String group;

    private String description;

    @NotNull(message = "Type is required")
    private SettingType type;

    @NotBlank(message = "Value is required")
    private String value;

    @NotNull(message = "Level is required")
    private SettingLevel level;

    @Builder.Default
    private Boolean isReadOnly = false;

    @Builder.Default
    private DisplayMode displayMode = DisplayMode.TEXT;
}
