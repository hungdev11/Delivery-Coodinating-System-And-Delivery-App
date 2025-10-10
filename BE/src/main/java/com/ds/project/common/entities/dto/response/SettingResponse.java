package com.ds.project.common.entities.dto.response;

import com.ds.project.app_context.models.Setting;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Setting response DTO for API responses
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SettingResponse {
    private String id;
    private String key;
    private String group;
    private String value;
    private Setting.SettingType type;
    private String description;
    private Setting.SettingLevel level;
    private Boolean deleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
