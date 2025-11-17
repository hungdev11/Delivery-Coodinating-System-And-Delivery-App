package com.ds.setting.common.entities.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request DTO for bulk upserting multiple settings
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BulkUpsertSettingsRequest {

    @NotBlank(message = "Group is required")
    private String group;

    @NotEmpty(message = "At least one setting is required")
    @Valid
    private List<CreateSettingRequest> settings;
}
