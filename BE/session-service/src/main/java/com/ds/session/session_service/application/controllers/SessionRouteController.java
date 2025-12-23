package com.ds.session.session_service.application.controllers;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ds.session.session_service.application.client.zoneclient.response.RouteResponse;
import com.ds.session.session_service.business.v1.services.SessionRouteService;
import com.ds.session.session_service.common.entities.dto.common.BaseResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Controller for session routing utilities (demo/diagnostic).
 * Provides API to get actual route from tracking history.
 */
@RestController
@RequestMapping("/api/v1/sessions")
@RequiredArgsConstructor
@Slf4j
@Validated
public class SessionRouteController {

    private final SessionRouteService sessionRouteService;

    /**
     * Get actual route for a session from tracking history.
     * GET /api/v1/sessions/{sessionId}/actual-route
     */
    @GetMapping("/{sessionId}/actual-route")
    public ResponseEntity<BaseResponse<RouteResponse>> getActualRouteForSession(
            @PathVariable UUID sessionId) {
        log.debug("[session-service] [SessionRouteController.getActualRouteForSession] GET /api/v1/sessions/{}/actual-route", sessionId);
        RouteResponse route = sessionRouteService.getActualRouteForSession(sessionId);
        return ResponseEntity.ok(BaseResponse.success(route));
    }
}
