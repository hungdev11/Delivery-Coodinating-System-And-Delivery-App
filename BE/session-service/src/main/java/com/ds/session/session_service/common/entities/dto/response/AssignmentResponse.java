package com.ds.session.session_service.common.entities.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.ds.session.session_service.common.enums.AssignmentStatus;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AssignmentResponse {
    private UUID id;
    private String parcelId; // Deprecated: use parcelIds instead
    private List<String> parcelIds; // All parcel IDs in this assignment
    private AssignmentStatus status;
    private String failReason;
    private LocalDateTime scanedAt;
    private LocalDateTime updatedAt;
    
    // Proofs grouped by parcel ID (session>assignment>parcel>proof)
    private Map<String, List<DeliveryProofResponse>> proofsByParcel;
}
