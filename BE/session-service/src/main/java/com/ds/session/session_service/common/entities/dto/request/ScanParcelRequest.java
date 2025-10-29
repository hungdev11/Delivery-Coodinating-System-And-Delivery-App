package com.ds.session.session_service.common.entities.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ScanParcelRequest {
    @NotBlank(message = "parcelId is required")
    private String parcelId;
}