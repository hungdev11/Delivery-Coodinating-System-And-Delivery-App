package com.ds.session.session_service.common.entities.dto.request;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for manual assignment creation by admin
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ManualAssignmentRequest {
    
    /**
     * Shipper ID (DeliveryMan ID) to assign parcels to
     */
    @NotNull(message = "Shipper ID is required")
    private String shipperId;
    
    /**
     * List of parcel IDs to assign
     * All parcels must have the same delivery address
     */
    @NotEmpty(message = "At least one parcel ID is required")
    private List<String> parcelIds;
    
    /**
     * Optional: Zone ID to filter parcels
     * If provided, only parcels in this zone will be assigned
     */
    private String zoneId;
}
