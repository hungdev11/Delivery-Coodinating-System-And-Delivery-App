package com.ds.session.session_service.common.entities.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for transferring a parcel from one shipper to another
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransferParcelRequest {
    @NotBlank(message = "Parcel ID is required")
    private String parcelId;
    
    @NotBlank(message = "Target session ID is required")
    private String targetSessionId; // Session ID of the receiving shipper
}
