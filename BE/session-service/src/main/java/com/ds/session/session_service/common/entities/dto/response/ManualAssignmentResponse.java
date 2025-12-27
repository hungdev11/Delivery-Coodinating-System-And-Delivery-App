package com.ds.session.session_service.common.entities.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.ds.session.session_service.common.enums.AssignmentStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for manual assignment creation
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ManualAssignmentResponse {
    
    /**
     * Assignment ID
     */
    private UUID assignmentId;
    
    /**
     * Shipper ID
     */
    private String shipperId;
    
    /**
     * Delivery address ID (all parcels share this address)
     */
    private String deliveryAddressId;
    
    /**
     * List of parcel IDs in this assignment
     */
    private List<String> parcelIds;
    
    /**
     * Assignment status (should be PENDING)
     */
    private AssignmentStatus status;
    
    /**
     * When the assignment was created
     */
    private LocalDateTime assignedAt;
    
    /**
     * Zone ID used for filtering (if provided)
     */
    private String zoneId;
    
    /**
     * Session ID that this assignment belongs to
     */
    private String sessionId;
}
