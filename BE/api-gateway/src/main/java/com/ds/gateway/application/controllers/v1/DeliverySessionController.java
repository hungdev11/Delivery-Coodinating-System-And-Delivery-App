package com.ds.gateway.application.controllers.v1;

import com.ds.gateway.annotations.AuthRequired;
import com.ds.gateway.business.v1.services.DeliverySessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Controller for delivery session operations with nested queries
 */
@RestController
@RequestMapping("/api/v1/delivery-sessions")
@RequiredArgsConstructor
@Slf4j
@Validated
@AuthRequired
public class DeliverySessionController {

    private final DeliverySessionService deliverySessionService;

    /**
     * API 1: Get delivery_session and all delivery_assignments in that session
     * This is a nesting query that combines session and assignment data
     */
    @GetMapping("/{sessionId}/with-assignments")
    public ResponseEntity<?> getSessionWithAssignments(@PathVariable UUID sessionId) {
        log.debug("[api-gateway] [DeliverySessionController.getSessionWithAssignments] Getting session {} with all assignments", sessionId);
        return deliverySessionService.getSessionWithAssignments(sessionId);
    }

    /**
     * API 2: Get demo-route by data from API 1
     * This must be done in api gateway, service layer
     * It takes session data and calculates a demo route for all assignments
     */
    @GetMapping("/{sessionId}/demo-route")
    public ResponseEntity<?> getDemoRouteForSession(
            @PathVariable UUID sessionId,
            @RequestParam(value = "startLat", required = false) Double startLat,
            @RequestParam(value = "startLon", required = false) Double startLon,
            @RequestParam(value = "vehicle", required = false, defaultValue = "bicycle") String vehicle,
            @RequestParam(value = "routingType", required = false, defaultValue = "full") String routingType) {
        log.debug("[api-gateway] [DeliverySessionController.getDemoRouteForSession] Getting demo route for session {} (startLat={}, startLon={}, vehicle={}, routingType={})", sessionId, startLat, startLon, vehicle, routingType);
        return deliverySessionService.getDemoRouteForSession(sessionId, startLat, startLon, vehicle, routingType);
    }

    /**
     * API 2b: Get actual route for a delivery session
     * Proxies to session-service /api/v1/sessions/{sessionId}/actual-route which is built
     * from shipper_location_tracking history.
     */
    @GetMapping("/{sessionId}/actual-route")
    public ResponseEntity<?> getActualRouteForSession(@PathVariable UUID sessionId) {
        log.debug("[api-gateway] [DeliverySessionController.getActualRouteForSession] Getting actual route for session {}", sessionId);
        return deliverySessionService.getActualRouteForSession(sessionId);
    }

    /**
     * API 3: Set delivery_assignments status and parcel status
     * This is a nesting query that updates both assignment and parcel status
     */
    @PutMapping("/{sessionId}/assignments/{assignmentId}/status")
    public ResponseEntity<?> updateAssignmentAndParcelStatus(
            @PathVariable UUID sessionId,
            @PathVariable UUID assignmentId,
            @RequestBody Object statusUpdateRequest) {
        log.debug("[api-gateway] [DeliverySessionController.updateAssignmentAndParcelStatus] Updating assignment {} and parcel status for session {}", assignmentId, sessionId);
        return deliverySessionService.updateAssignmentAndParcelStatus(sessionId, assignmentId, statusUpdateRequest);
    }
}
