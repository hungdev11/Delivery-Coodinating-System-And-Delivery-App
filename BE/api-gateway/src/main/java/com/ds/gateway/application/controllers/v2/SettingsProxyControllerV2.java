package com.ds.gateway.application.controllers.v2;

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
 * Proxy controller for Settings Service V2 endpoints (enhanced filtering)
 */
@Slf4j
@RestController
@RequestMapping("/api/v2/settings")
@RequiredArgsConstructor
public class SettingsProxyControllerV2 {

    private final RestTemplate restTemplate;

    @Value("${services.settings.base-url}")
    private String settingsServiceUrl;

    private String settingsV2Url;

    @PostConstruct
    private void init() {
        this.settingsV2Url = settingsServiceUrl + "/api/v2/settings";
    }

    @PostMapping
    public ResponseEntity<?> listSettings(@RequestBody Object requestBody) {
        log.info("POST /api/v2/settings - proxy to Settings Service");
        try {
            Object response = restTemplate.postForObject(settingsV2Url, requestBody, Object.class);
            return ResponseEntity.ok(response);
        } catch (HttpClientErrorException | HttpServerErrorException ex) {
            log.error("Settings V2 proxy failed: {}", ex.getMessage());
            return ResponseEntity.status(ex.getStatusCode()).body(ex.getResponseBodyAsString());
        }
    }
}
