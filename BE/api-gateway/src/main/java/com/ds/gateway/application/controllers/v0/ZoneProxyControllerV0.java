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
 * API Gateway controller for Zone Service V0 endpoints
 */
@Slf4j
@RestController
@RequestMapping("/api/v0/zones")
@RequiredArgsConstructor
@AuthRequired
public class ZoneProxyControllerV0 {

    private final RestTemplate restTemplate;

    @Value("${services.zone.base-url}")
    private String baseUrl;

    private String zoneV0Url;

    @PostConstruct
    private void init() {
        this.zoneV0Url = baseUrl + "/api/v0/zones";
    }

    @PostMapping
    public ResponseEntity<?> listZones(@RequestBody Object requestBody) {
        log.info("POST /api/v0/zones - proxy to Zone Service");
        try {
            Object response = restTemplate.postForObject(zoneV0Url, requestBody, Object.class);
            return ResponseEntity.ok(response);
        } catch (HttpClientErrorException | HttpServerErrorException ex) {
            log.error("Zone V0 proxy failed: {}", ex.getMessage());
            return ResponseEntity.status(ex.getStatusCode()).body(ex.getResponseBodyAsString());
        }
    }
}
