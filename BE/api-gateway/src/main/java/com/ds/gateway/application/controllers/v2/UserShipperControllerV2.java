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
 * Proxy controller for User Service shipper (delivery man) endpoints - V2.
 *
 * Forwards requests from management UI to the User Service.
 */
@Slf4j
@RestController
@RequestMapping("/api/v2/users/shippers")
@AuthRequired
@RequiredArgsConstructor
public class UserShipperControllerV2 {

    private final RestTemplate restTemplate;

    @Value("${services.user.base-url}")
    private String userServiceBaseUrl;

    private String shipperV2Url;

    @PostConstruct
    private void init() {
        this.shipperV2Url = userServiceBaseUrl + "/api/v2/users/shippers";
    }

    /**
     * List shippers with advanced filtering (V2 payload).
     */
    @PostMapping
    public ResponseEntity<?> listShippers(@RequestBody Object requestBody) {
        log.info("POST /api/v2/users/shippers - proxy to User Service");
        try {
            Object response = restTemplate.postForObject(shipperV2Url, requestBody, Object.class);
            return ResponseEntity.ok(response);
        } catch (HttpClientErrorException | HttpServerErrorException ex) {
            log.error("Shipper V2 proxy failed: {}", ex.getMessage());
            return ResponseEntity.status(ex.getStatusCode()).body(ex.getResponseBodyAsString());
        }
    }
}
