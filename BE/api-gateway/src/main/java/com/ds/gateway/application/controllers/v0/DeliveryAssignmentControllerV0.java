package com.ds.gateway.application.controllers.v0;

import com.ds.gateway.annotations.AuthRequired;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

/**
 * Proxy controller for Session Service delivery assignments V0 endpoints
 */
@Slf4j
@RestController
@RequestMapping("/api/v0/assignments")
@RequiredArgsConstructor
@AuthRequired
public class DeliveryAssignmentControllerV0 {

    private final RestTemplate restTemplate;

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
        try {
            Object response = restTemplate.postForObject(assignmentsV0Url, requestBody, Object.class);
            return ResponseEntity.ok(response);
        } catch (HttpClientErrorException | HttpServerErrorException ex) {
            log.error("Assignments V0 proxy failed: {}", ex.getMessage());
            return ResponseEntity.status(ex.getStatusCode()).body(ex.getResponseBodyAsString());
        }
    }
}
