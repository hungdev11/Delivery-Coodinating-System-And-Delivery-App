package com.ds.session.session_service.common.entities.dto.event;

import java.time.Instant;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Event published when a parcel is postponed (out of session time)
 * Communication service will send message to user
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
