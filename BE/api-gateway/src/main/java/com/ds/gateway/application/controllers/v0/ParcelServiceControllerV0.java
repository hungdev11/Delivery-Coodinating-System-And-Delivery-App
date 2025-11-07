package com.ds.gateway.application.controllers.v0;

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
 * Parcel proxy controller for V0 endpoints (simple paging)
 */
@Slf4j
@RestController
@RequestMapping("/api/v0/parcels")
@RequiredArgsConstructor
public class ParcelServiceControllerV0 {

    private final RestTemplate restTemplate;

    @Value("${services.parcel.base-url}")
    private String baseUrl;

    private String parcelV0Url;

    @PostConstruct
    private void init() {
        this.parcelV0Url = baseUrl + "/api/v0/parcels";
    }

    @PostMapping
    public ResponseEntity<?> listParcels(@RequestBody Object request) {
        log.info("POST /api/v0/parcels - proxy to Parcel Service");
        try {
            Object response = restTemplate.postForObject(parcelV0Url, request, Object.class);
            return ResponseEntity.ok(response);
        } catch (HttpClientErrorException | HttpServerErrorException ex) {
            log.error("Parcel V0 proxy failed: {}", ex.getMessage());
            return ResponseEntity.status(ex.getStatusCode()).body(ex.getResponseBodyAsString());
        }
    }
}
