package com.ds.gateway.application.controllers.v1;

import com.ds.gateway.annotations.AuthRequired;
import com.ds.gateway.infrastructure.http.ProxyHttpClient;
import com.ds.gateway.infrastructure.logging.ProxyLogContext;
import com.ds.gateway.infrastructure.logging.ProxyRequestLogger;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;

/**
 * API Gateway proxy for Admin Session APIs
 * Exposes admin session creation endpoint
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/sessions")
@RequiredArgsConstructor
public class AdminSessionController {

    private static final String SESSION_SERVICE = "session-service";

    private final ProxyHttpClient proxyHttpClient;
    private final ProxyRequestLogger proxyRequestLogger;

    @Value("${services.session.base-url}")
    private String sessionServiceUrl;

    /**
     * Create session prepared (CREATED status) for a delivery man
     * POST /api/v1/admin/sessions/prepared/{deliveryManId}
     */
    @PostMapping("/prepared/{deliveryManId}")
    @AuthRequired
    public ResponseEntity<?> createSessionPrepared(@PathVariable String deliveryManId) {
        log.debug("[api-gateway] [AdminSessionController.createSessionPrepared] POST /api/v1/admin/sessions/prepared/{} - Proxying to Session Service", deliveryManId);
        return proxySession(HttpMethod.POST, "/api/v1/delivery-sessions/prepared/" + deliveryManId, null);
    }

    /**
     * Create session with assignments (admin workflow)
     * POST /api/v1/admin/sessions
     */
    @PostMapping
    @AuthRequired
    public ResponseEntity<?> createSessionWithAssignments(@RequestBody Object request) {
        log.debug("[api-gateway] [AdminSessionController.createSessionWithAssignments] POST /api/v1/admin/sessions - Proxying to Session Service");
        return proxySession(HttpMethod.POST, "/api/v1/admin/sessions", request);
    }

    private ResponseEntity<Object> proxySession(HttpMethod method, String path, Object body) {
        String url = sessionServiceUrl + path;
        ProxyLogContext context = proxyRequestLogger.start(method, SESSION_SERVICE, url, body);
        try {
            ResponseEntity<Object> response = proxyHttpClient.exchange(method, url, body, Object.class);
            proxyRequestLogger.success(context, response.getStatusCode().value());
            return response;
        } catch (ResourceAccessException e) {
            proxyRequestLogger.failure(context, 502, e.getMessage(), e);
            return ResponseEntity.status(502).body("{\"error\":\"Bad Gateway: Session Service unavailable\"}");
        } catch (HttpStatusCodeException e) {
            proxyRequestLogger.failure(context, e.getStatusCode().value(), e.getResponseBodyAsString(), e);
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        } catch (Exception e) {
            proxyRequestLogger.failure(context, 500, e.getMessage(), e);
            return ResponseEntity.status(500).body("{\"error\":\"Internal Server Error: " + e.getMessage() + "\"}");
        }
    }
}
