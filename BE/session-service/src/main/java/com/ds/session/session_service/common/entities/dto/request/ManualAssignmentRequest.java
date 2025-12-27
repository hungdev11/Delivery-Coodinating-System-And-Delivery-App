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
 * 
 * IMPORTANT: Session must be created first (status CREATED), then assignments are created and linked to the session.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ManualAssignmentRequest {
    
    /**
     * Session ID - Session must be created first (status CREATED) before creating assignments
     */
    @NotNull(message = "Session ID is required. Session must be created first.")
    private String sessionId;
    
    /**
     * Shipper ID (DeliveryMan ID) - must match the session's deliveryManId
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
