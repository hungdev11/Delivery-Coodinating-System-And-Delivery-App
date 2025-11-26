package com.ds.session.session_service.common.entities.dto.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDateTime;

/**
 * Event published when a delivery assignment is completed
 * This event is consumed by communication-service to send notifications
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignmentCompletedEvent {
    /**
     * Unique event ID
     */
    private String eventId;
    
    /**
     * Assignment ID
     */
    private String assignmentId;
    
    /**
     * Parcel ID
     */
    private String parcelId;
    
    /**
     * Parcel code
     */
    private String parcelCode;
    
    /**
     * Session ID
     */
    private String sessionId;
    
    /**
     * Delivery man (shipper) ID
     */
    private String deliveryManId;
    
    /**
     * Delivery man name
     */
    private String deliveryManName;
    
    /**
     * Receiver (client) ID
     */
    private String receiverId;
    
    /**
     * Receiver name
     */
    private String receiverName;
    
    /**
     * Receiver phone number
     */
    private String receiverPhone;
    
    /**
     * Completion time
     */
    private LocalDateTime completedAt;
    
    /**
     * Source service
     */
    private String sourceService;
    
    /**
     * Event timestamp
     */
    private Instant createdAt;
}
