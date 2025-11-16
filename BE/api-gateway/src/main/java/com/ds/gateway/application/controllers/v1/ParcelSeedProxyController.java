package com.ds.gateway.application.controllers.v1;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

/**
 * Proxy controller for Parcel Seed endpoints - V1
 * Forwards requests to User Service
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/parcels/seed")
@RequiredArgsConstructor
public class ParcelSeedProxyController {

    private final RestTemplate restTemplate;

    @Value("${services.user.base-url}")
    private String userServiceBaseUrl;

    private String parcelSeedUrl;

    @PostConstruct
    private void init() {
        this.parcelSeedUrl = userServiceBaseUrl + "/api/v1/parcels/seed";
    }

    /**
     * POST /api/v1/parcels/seed
     * Seed parcels randomly or with specific shop/client
     */
    @PostMapping
    public ResponseEntity<?> seedParcels(@RequestBody Object requestBody) {
        log.info("POST /api/v1/parcels/seed - proxy to User Service");
        try {
            return restTemplate.postForEntity(parcelSeedUrl, requestBody, Object.class);
        } catch (HttpClientErrorException | HttpServerErrorException ex) {
            log.error("Parcel seed proxy failed: {}", ex.getMessage());
            return ResponseEntity.status(ex.getStatusCode()).body(ex.getResponseBodyAsString());
        }
    }
}
