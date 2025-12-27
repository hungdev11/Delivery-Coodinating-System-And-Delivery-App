package com.ds.session.session_service.application.client.zoneclient.response;

import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for VRP assignment API
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VRPAssignmentResponse {
    
    /**
     * Map of shipperId -> List of Tasks
     */
    private Map<String, List<VRPTaskDto>> assignments;
    
    /**
     * Order IDs that could not be assigned
     */
    private List<String> unassignedOrders;
    
    /**
     * Statistics about the assignment
     */
    private Statistics statistics;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VRPTaskDto {
        private String orderId;
        private Integer sequenceIndex; // 0 = first stop, 1 = second stop, etc.
        private String estimatedArrivalTime; // ISO datetime format
        private Integer travelTimeFromPreviousStop; // in seconds
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Statistics {
        private Integer totalShippers;
        private Integer totalOrders;
        private Integer assignedOrders;
        private Double averageOrdersPerShipper;
        private Double workloadVariance;
    }
}
