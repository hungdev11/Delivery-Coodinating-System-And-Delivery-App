package com.ds.session.session_service.common.entities.dto.response;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.ds.session.session_service.common.enums.AssignmentStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for auto assignment creation
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AutoAssignmentResponse {
    
    /**
     * Map of shipperId -> List of created assignment IDs
     */
    private Map<String, List<AssignmentInfo>> assignments;
    
    /**
     * Parcel IDs that could not be assigned
     */
    private List<String> unassignedParcels;
    
    /**
     * Statistics about the assignment
     */
    private Statistics statistics;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AssignmentInfo {
        private UUID assignmentId;
        private String deliveryAddressId;
        private List<String> parcelIds;
        private AssignmentStatus status;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Statistics {
        private Integer totalShippers;
        private Integer totalParcels;
        private Integer assignedParcels;
        private Double averageParcelsPerShipper;
        private Double workloadVariance;
    }
}
