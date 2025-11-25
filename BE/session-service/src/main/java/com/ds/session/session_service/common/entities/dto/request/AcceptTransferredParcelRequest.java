package com.ds.session.session_service.common.entities.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for accepting a transferred parcel by scanning session QR
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AcceptTransferredParcelRequest {
    @NotBlank(message = "Source session ID is required")
    private String sourceSessionId; // Session ID of the shipper transferring the parcel
    
    @NotBlank(message = "Parcel ID is required")
    private String parcelId;
}
