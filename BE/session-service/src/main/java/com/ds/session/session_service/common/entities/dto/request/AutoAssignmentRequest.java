package com.ds.session.session_service.common.entities.dto.request;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for auto assignment creation by admin
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AutoAssignmentRequest {
    
    /**
     * List of shipper IDs to consider for assignment
     * If empty, all available shippers will be considered
     */
    private List<String> shipperIds;
    
    /**
     * List of parcel IDs to assign
     * If empty, all unassigned parcels will be considered
     */
    private List<String> parcelIds;
    
    /**
     * Vehicle type for routing calculation
     * Default: "motorbike"
     */
    private String vehicle; // "car" or "motorbike"
    
    /**
     * OSRM routing mode
     * Default: "v2-full"
     */
    private String mode; // "v2-full", "v2-rating-only", etc.
}
