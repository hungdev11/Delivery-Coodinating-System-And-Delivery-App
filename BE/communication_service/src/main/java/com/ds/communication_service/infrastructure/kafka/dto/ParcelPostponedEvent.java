package com.ds.communication_service.infrastructure.kafka.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDateTime;

/**
 * Event received when a parcel is postponed (out of session time)
 * This event is published by session-service and consumed by communication-service
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParcelPostponedEvent {
    private String eventId;
    private String assignmentId;
    private String parcelId;
    private String parcelCode;
    private String sessionId;
    private String deliveryManId;
    private String receiverId;
    private String receiverName;
    private String receiverPhone;
    private LocalDateTime postponeDateTime; // Requested postpone time
    private String reason;
    private Instant createdAt;
    private String sourceService;
}
