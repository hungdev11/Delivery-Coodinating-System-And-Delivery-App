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
 * User controller V2 for managing user operations
 * Routes to /api/v2/users endpoints on backend services
 * Requires authentication for all routes
 */
@Slf4j
@RestController
@RequestMapping("/api/v2/users")
@AuthRequired
@RequiredArgsConstructor
public class UserControllerV2 {

    private final RestTemplate restTemplate;

    @Value("${services.user.base-url}")
    private String baseUrl;

    private String userV2Url;

    @PostConstruct
    private void init() {
        this.userV2Url = baseUrl + "/api/v2/users";
    }

    @PostMapping
    public ResponseEntity<?> listUsers(@RequestBody Object requestBody) {
        log.info("POST /api/v2/users - proxy to User Service");
        try {
            Object response = restTemplate.postForObject(userV2Url, requestBody, Object.class);
            return ResponseEntity.ok(response);
        } catch (HttpClientErrorException | HttpServerErrorException ex) {
            log.error("User V2 proxy failed: {}", ex.getMessage());
            return ResponseEntity.status(ex.getStatusCode()).body(ex.getResponseBodyAsString());
        }
    }
}
