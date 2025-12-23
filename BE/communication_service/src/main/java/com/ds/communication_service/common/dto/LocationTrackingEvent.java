package com.ds.communication_service.common.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Location tracking event received from session-service.
 * Broadcast via WebSocket to interested clients.
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
