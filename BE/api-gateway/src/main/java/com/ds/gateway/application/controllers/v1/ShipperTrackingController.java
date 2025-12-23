package com.ds.gateway.application.controllers.v1;

import com.ds.gateway.common.entities.dto.common.BaseResponse;
import com.ds.gateway.common.interfaces.ISessionServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/sessions")
@RequiredArgsConstructor
@Slf4j
@Validated
public class ShipperTrackingController {

    private final ISessionServiceClient sessionServiceClient;

    /**
     * Proxy tracking endpoint to session-service
     * POST /api/v1/sessions/{sessionId}/tracking
     */
    @PostMapping("/{sessionId}/tracking")
    public ResponseEntity<?> sendLocationUpdate(
            @PathVariable("sessionId") String sessionId,
            @RequestBody Object locationUpdateRequest) {
        log.debug("[api-gateway] [ShipperTrackingController.sendLocationUpdate] Proxying location update for session {}", sessionId);
        return sessionServiceClient.sendLocationUpdate(sessionId, locationUpdateRequest);
    }
}
