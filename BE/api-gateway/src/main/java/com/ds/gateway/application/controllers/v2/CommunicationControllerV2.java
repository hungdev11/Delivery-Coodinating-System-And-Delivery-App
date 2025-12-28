package com.ds.gateway.application.controllers.v2;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;

import com.ds.gateway.annotations.AuthRequired;
import com.ds.gateway.infrastructure.http.ProxyHttpClient;
import com.ds.gateway.infrastructure.logging.ProxyLogContext;
import com.ds.gateway.infrastructure.logging.ProxyRequestLogger;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * API Gateway proxy for Communication Service V2 endpoints
 */
@Slf4j
@RestController
@RequestMapping("/api/v2")
@RequiredArgsConstructor
public class CommunicationControllerV2 {

    private static final String COMMUNICATION_SERVICE = "communication-service";

    private final ProxyHttpClient proxyHttpClient;
    private final ProxyRequestLogger proxyRequestLogger;

    @Value("${services.communication.base-url}")
    private String communicationServiceUrl;

    /**
     * Get tickets with V2 filter system (POST endpoint) - Admin only
     */
    @PostMapping("/tickets")
    @AuthRequired
    public ResponseEntity<?> getTicketsV2(@RequestBody Object request) {
        log.debug("[api-gateway] [CommunicationControllerV2.getTicketsV2] POST /api/v2/tickets - Proxying to Communication Service");
        return proxyCommunication(HttpMethod.POST, "/api/v2/tickets", request);
    }

    private ResponseEntity<Object> proxyCommunication(HttpMethod method, String path, Object body) {
        String url = communicationServiceUrl + path;
        ProxyLogContext context = proxyRequestLogger.start(method, COMMUNICATION_SERVICE, url, body);
        try {
            ResponseEntity<Object> response = proxyHttpClient.exchange(method, url, body, Object.class);
            proxyRequestLogger.success(context, response.getStatusCode().value());
            return response;
        } catch (ResourceAccessException e) {
            proxyRequestLogger.failure(context, 502, e.getMessage(), e);
            return ResponseEntity.status(502).body("{\"error\":\"Bad Gateway: Communication Service unavailable\"}");
        } catch (HttpStatusCodeException e) {
            proxyRequestLogger.failure(context, e.getStatusCode().value(), e.getResponseBodyAsString(), e);
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        } catch (Exception e) {
            proxyRequestLogger.failure(context, 500, e.getMessage(), e);
            return ResponseEntity.status(500).body("{\"error\":\"Internal Server Error: " + e.getMessage() + "\"}");
        }
    }
}
