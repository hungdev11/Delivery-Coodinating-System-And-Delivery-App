package com.ds.communication_service.application.controller;

import com.ds.communication_service.common.dto.BaseResponse;
import com.ds.communication_service.common.dto.LocationTrackingEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Use Spring's validation instead of javax/jakarta directly

/**
 * REST controller receiving location tracking events from session-service
 * and broadcasting them via WebSocket.
 */
@RestController
@RequestMapping("/api/v1/location-tracking")
@RequiredArgsConstructor
@Slf4j
@Validated
public class LocationTrackingEventController {

    private final SimpMessageSendingOperations messagingTemplate;

    /**
     * Receive location tracking event and broadcast to WebSocket subscribers.
     * - /topic/sessions/{sessionId}/tracking
     */
    @PostMapping("/events")
    public ResponseEntity<BaseResponse<Void>> receiveLocationTrackingEvent(
            @RequestBody LocationTrackingEvent event) {
        try {
            if (event.getSessionId() == null || event.getSessionId().isBlank()) {
                return ResponseEntity.badRequest().body(BaseResponse.error("sessionId is required"));
            }

            String topic = String.format("/topic/sessions/%s/tracking", event.getSessionId());
            log.debug(
                "[communication-service] [LocationTrackingEventController] Broadcasting location event to {}: deliveryManId={}, lat={}, lon={}, type={}",
                topic, event.getDeliveryManId(), event.getLat(), event.getLon(), event.getEventType());

            messagingTemplate.convertAndSend(topic, event);
            return ResponseEntity.ok(BaseResponse.success(null));
        } catch (Exception e) {
            log.error("[communication-service] [LocationTrackingEventController] Failed to process location tracking event", e);
            return ResponseEntity.internalServerError()
                    .body(BaseResponse.error("Failed to process location tracking event: " + e.getMessage()));
        }
    }
}
