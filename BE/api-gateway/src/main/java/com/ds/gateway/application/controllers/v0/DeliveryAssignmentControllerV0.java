package com.ds.gateway.application.controllers.v0;

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
 * Proxy controller for Session Service delivery assignments V0 endpoints
 */
@Slf4j
@RestController
@RequestMapping("/api/v0/assignments")
@RequiredArgsConstructor
@AuthRequired
public class DeliveryAssignmentControllerV0 {

    private static final String SESSION_SERVICE = "session-service";

    private final ProxyControllerSupport proxyControllerSupport;

    @Value("${services.session.base-url}")
    private String sessionServiceUrl;

    private String assignmentsV0Url;

    @PostConstruct
    private void init() {
        this.assignmentsV0Url = sessionServiceUrl + "/api/v0/assignments";
    }

    @PostMapping
    public ResponseEntity<?> listAssignments(@RequestBody Object requestBody) {
        log.info("POST /api/v0/assignments - proxy to Session Service");
        return proxyControllerSupport.forward(SESSION_SERVICE, HttpMethod.POST, assignmentsV0Url, requestBody);
    }
}
