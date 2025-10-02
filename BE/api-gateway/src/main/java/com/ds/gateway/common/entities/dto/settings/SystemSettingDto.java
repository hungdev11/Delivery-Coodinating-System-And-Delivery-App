package com.ds.gateway.common.entities.dto.settings;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * System Setting DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemSettingDto {
    
    private String key;
    private String group;
    private String description;
    private String type;
    private String value;
    private String level;
    private Boolean isReadOnly;
    private String displayMode;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String updatedBy;
}
