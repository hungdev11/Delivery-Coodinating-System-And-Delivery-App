package com.ds.session.session_service.application.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ds.session.session_service.common.entities.dto.common.BaseResponse;
import com.ds.session.session_service.common.entities.dto.request.LocationUpdateRequest;
import com.ds.session.session_service.common.interfaces.IShipperLocationTrackingService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Controller nhận location updates từ Android app (mỗi 1 giây)
 */
@RestController
@RequestMapping("/api/v1/sessions")
@RequiredArgsConstructor
@Slf4j
@Validated
public class ShipperTrackingController {

    private final IShipperLocationTrackingService trackingService;

    /**
     * Endpoint nhận location updates từ Android app
     * POST /api/v1/sessions/{sessionId}/tracking
     */
    @PostMapping("/{sessionId}/tracking")
    public ResponseEntity<BaseResponse<Void>> sendLocationUpdate(
            @PathVariable("sessionId") String sessionId,
            @Valid @RequestBody LocationUpdateRequest request) {
        log.debug("[session-service] [ShipperTrackingController.sendLocationUpdate] Received location update for session {}", sessionId);
        
        try {
            trackingService.addTrackingPoint(sessionId, request);
            return ResponseEntity.ok(BaseResponse.success(null));
        } catch (Exception e) {
            log.error("[session-service] [ShipperTrackingController.sendLocationUpdate] Error processing location update", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(BaseResponse.error("Failed to process location update: " + e.getMessage()));
        }
    }
}
