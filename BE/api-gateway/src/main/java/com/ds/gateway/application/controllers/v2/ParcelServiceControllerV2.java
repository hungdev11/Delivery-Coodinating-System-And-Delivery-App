package com.ds.gateway.application.controllers.v2;

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
 * Parcel proxy controller for V2 endpoints (enhanced filtering)
 */
@Slf4j
@RestController
@RequestMapping("/api/v2/parcels")
@RequiredArgsConstructor
@AuthRequired
public class ParcelServiceControllerV2 {

    private final RestTemplate restTemplate;

    @Value("${services.parcel.base-url}")
    private String baseUrl;

    private String parcelV2Url;

    @PostConstruct
    private void init() {
        this.parcelV2Url = baseUrl + "/api/v2/parcels";
    }

    @PostMapping
    public ResponseEntity<?> listParcels(@RequestBody Object request) {
        log.info("POST /api/v2/parcels - proxy to Parcel Service");
        try {
            Object response = restTemplate.postForObject(parcelV2Url, request, Object.class);
            return ResponseEntity.ok(response);
        } catch (HttpClientErrorException | HttpServerErrorException ex) {
            log.error("Parcel V2 proxy failed: {}", ex.getMessage());
            return ResponseEntity.status(ex.getStatusCode()).body(ex.getResponseBodyAsString());
        }
    }
}
