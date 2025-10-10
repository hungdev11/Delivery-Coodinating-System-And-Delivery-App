package com.ds.project.common.entities.dto.request;

import com.ds.project.app_context.models.Setting;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Setting request DTO for creating/updating settings
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SettingRequest {
    
    @NotBlank(message = "Setting key is required")
    @Size(max = 100, message = "Setting key must not exceed 100 characters")
    private String key;
    
    @NotBlank(message = "Setting group is required")
    @Size(max = 50, message = "Setting group must not exceed 50 characters")
    private String group;
    
    @NotBlank(message = "Setting value is required")
    private String value;
    
    @NotNull(message = "Setting type is required")
    private Setting.SettingType type;
    
    @Size(max = 500, message = "Setting description must not exceed 500 characters")
    private String description;
    
    @NotNull(message = "Setting level is required")
    private Setting.SettingLevel level;
}
