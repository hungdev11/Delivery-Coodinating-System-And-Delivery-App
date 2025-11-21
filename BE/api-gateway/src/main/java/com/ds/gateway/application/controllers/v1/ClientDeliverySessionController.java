package com.ds.gateway.application.controllers.v1;

import com.ds.gateway.annotations.AuthRequired;
import jakarta.annotation.PostConstruct;
import com.ds.gateway.application.controllers.support.ProxyControllerSupport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Client-specific controller for delivery session operations (V1)
 * This endpoint is for client users and will have guards/authorization in the future
 * Note: V1 uses RestTemplate to proxy to session-service v2 endpoint
 */
@RestController
@RequestMapping("/api/v1/client/delivery-sessions")
@RequiredArgsConstructor
@Slf4j
@Validated
@AuthRequired
public class ClientDeliverySessionController {

    private static final String SESSION_SERVICE = "session-service";

    private final ProxyControllerSupport proxyControllerSupport;

    @Value("${services.session.base-url}")
    private String sessionServiceUrl;

    private String deliverySessionsV2Url;

    @PostConstruct
    private void init() {
        this.deliverySessionsV2Url = sessionServiceUrl + "/api/v2/delivery-sessions";
    }

    /**
     * Search delivery sessions for client users
     * This endpoint will have client-specific guards/authorization in the future
     */
    @PostMapping
    public ResponseEntity<?> searchDeliverySessions(@RequestBody Object searchRequest) {
        log.info("POST /api/v1/client/delivery-sessions - proxy to Session Service for client");
        return proxyControllerSupport.forward(SESSION_SERVICE, HttpMethod.POST, deliverySessionsV2Url, searchRequest);
    }
}
