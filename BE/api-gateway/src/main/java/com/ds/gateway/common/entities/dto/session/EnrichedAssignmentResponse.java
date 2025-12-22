package com.ds.gateway.common.entities.dto.session;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Enriched assignment response with full parcel information and proofs
 * This is returned by API Gateway after aggregating data from session-service, parcel-service, and delivery-proofs
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnrichedAssignmentResponse {
    // Assignment basic info
    private UUID id;
    private String assignmentId; // String version for compatibility
    private String sessionId;
    private String parcelId;
    private String status;
    private String failReason;
    private LocalDateTime scanedAt;
    private LocalDateTime updatedAt;
    private LocalDateTime completedAt;
    
    // Parcel information (enriched from parcel-service)
    private String parcelCode;
    private String deliveryType;
    private String receiverName;
    private String receiverId;
    private String receiverPhone;
    private String deliveryLocation;
    private BigDecimal value;
    private Double weight;
    private BigDecimal lat;
    private BigDecimal lon;
    
    // Proofs (enriched from delivery-proofs service)
    private List<DeliveryProofResponse> proofs;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DeliveryProofResponse {
        private UUID id;
        private String type; // DELIVERED, etc.
        private String mediaUrl;
        private String confirmedBy;
        private LocalDateTime createdAt;
    }
}
