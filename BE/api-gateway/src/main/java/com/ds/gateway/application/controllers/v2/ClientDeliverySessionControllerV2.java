package com.ds.gateway.application.controllers.v2;

import com.ds.gateway.annotations.AuthRequired;
import com.ds.gateway.application.controllers.support.ProxyControllerSupport;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Client-specific proxy controller for Session Service delivery sessions V2 endpoints
 * This endpoint is for client users and will have guards/authorization in the future
 */
@Slf4j
@RestController
@RequestMapping("/api/v2/client/delivery-sessions")
@RequiredArgsConstructor
@AuthRequired
public class ClientDeliverySessionControllerV2 {

    private static final String SESSION_SERVICE = "session-service";

    private final ProxyControllerSupport proxyControllerSupport;

    @Value("${services.session.base-url}")
    private String sessionServiceUrl;

    private String deliverySessionsV2Url;

    @PostConstruct
    private void init() {
        this.deliverySessionsV2Url = sessionServiceUrl + "/api/v2/delivery-sessions";
    }

    @PostMapping
    public ResponseEntity<?> listDeliverySessions(@RequestBody Object requestBody) {
        log.debug("[api-gateway] [ClientDeliverySessionControllerV2.listDeliverySessions] POST /api/v2/client/delivery-sessions - proxy to Session Service for client");
        return proxyControllerSupport.forward(SESSION_SERVICE, HttpMethod.POST, deliverySessionsV2Url, requestBody);
    }
}
