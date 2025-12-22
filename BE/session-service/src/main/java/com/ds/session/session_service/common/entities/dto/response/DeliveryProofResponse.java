package com.ds.session.session_service.common.entities.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

import com.ds.session.session_service.app_context.models.DeliveryProof;
import com.ds.session.session_service.common.enums.ProofType;

public record DeliveryProofResponse(
    UUID id,
    ProofType type,
    String mediaUrl,
    String confirmedBy,
    LocalDateTime createdAt
) {
    public static DeliveryProofResponse from(DeliveryProof p) {
        return new DeliveryProofResponse(
            p.getId(),
            p.getType(),
            p.getMediaUrl(),
            p.getConfirmedBy(),
            p.getCreatedAt()
        );
    }
}

