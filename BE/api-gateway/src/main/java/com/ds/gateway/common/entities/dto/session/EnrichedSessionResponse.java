package com.ds.gateway.common.entities.dto.session;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Enriched session response with full assignment details including parcel info and proofs
 * This is returned by API Gateway after aggregating data from multiple services
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnrichedSessionResponse {
    private UUID id;
    private String deliveryManId;
    private String status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer totalTasks;
    private Integer completedTasks;
    private Integer failedTasks;
    
    // Enriched delivery man information (from session-service)
    private DeliveryManInfo deliveryMan;
    
    // Enriched assignments with parcel info and proofs
    private List<EnrichedAssignmentResponse> assignments;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DeliveryManInfo {
        private String name;
        private String vehicleType;
        private Double capacityKg;
        private String phone;
        private String email;
    }
}
