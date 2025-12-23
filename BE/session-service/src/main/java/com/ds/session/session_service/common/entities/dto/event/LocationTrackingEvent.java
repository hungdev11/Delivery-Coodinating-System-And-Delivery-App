package com.ds.session.session_service.common.entities.dto.event;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Location tracking event sent from session-service to communication-service.
 * Used for real-time WebSocket updates of shipper location.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocationTrackingEvent {
    private String sessionId;
    private String deliveryManId;
    private Double lat;
    private Double lon;
    private LocalDateTime timestamp;
    private String nearestNodeId;
    /**
     * LOCATION_UPDATE or NODE_PASSED
     */
    private String eventType;
}
