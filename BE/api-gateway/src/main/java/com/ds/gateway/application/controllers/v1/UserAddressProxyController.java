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
 * Proxy controller for User Address endpoints - V1
 * Forwards requests to User Service
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserAddressProxyController {

    private final RestTemplate restTemplate;

    @Value("${services.user.base-url}")
    private String userServiceBaseUrl;

    private String userAddressUrl;

    @PostConstruct
    private void init() {
        this.userAddressUrl = userServiceBaseUrl + "/api/v1/users";
    }

    // Client endpoints (current user)
    @PostMapping("/me/addresses")
    public ResponseEntity<?> createMyAddress(@RequestHeader(value = "X-User-Id", required = false) String userId, @RequestBody Object requestBody) {
        log.info("POST /api/v1/users/me/addresses - proxy to User Service");
        try {
            return restTemplate.postForEntity(userAddressUrl + "/me/addresses", requestBody, Object.class);
        } catch (HttpClientErrorException | HttpServerErrorException ex) {
            log.error("User address proxy failed: {}", ex.getMessage());
            return ResponseEntity.status(ex.getStatusCode()).body(ex.getResponseBodyAsString());
        }
    }

    @GetMapping("/me/addresses")
    public ResponseEntity<?> getMyAddresses(@RequestHeader(value = "X-User-Id", required = false) String userId) {
        log.info("GET /api/v1/users/me/addresses - proxy to User Service");
        try {
            return restTemplate.getForEntity(userAddressUrl + "/me/addresses", Object.class);
        } catch (HttpClientErrorException | HttpServerErrorException ex) {
            log.error("User address proxy failed: {}", ex.getMessage());
            return ResponseEntity.status(ex.getStatusCode()).body(ex.getResponseBodyAsString());
        }
    }

    @GetMapping("/me/addresses/primary")
    public ResponseEntity<?> getMyPrimaryAddress(@RequestHeader(value = "X-User-Id", required = false) String userId) {
        log.info("GET /api/v1/users/me/addresses/primary - proxy to User Service");
        try {
            return restTemplate.getForEntity(userAddressUrl + "/me/addresses/primary", Object.class);
        } catch (HttpClientErrorException | HttpServerErrorException ex) {
            log.error("User address proxy failed: {}", ex.getMessage());
            return ResponseEntity.status(ex.getStatusCode()).body(ex.getResponseBodyAsString());
        }
    }

    @GetMapping("/me/addresses/{addressId}")
    public ResponseEntity<?> getMyAddress(@PathVariable String addressId, @RequestHeader(value = "X-User-Id", required = false) String userId) {
        log.info("GET /api/v1/users/me/addresses/{} - proxy to User Service", addressId);
        try {
            return restTemplate.getForEntity(userAddressUrl + "/me/addresses/" + addressId, Object.class);
        } catch (HttpClientErrorException | HttpServerErrorException ex) {
            log.error("User address proxy failed: {}", ex.getMessage());
            return ResponseEntity.status(ex.getStatusCode()).body(ex.getResponseBodyAsString());
        }
    }

    @PutMapping("/me/addresses/{addressId}")
    public ResponseEntity<?> updateMyAddress(@PathVariable String addressId, @RequestHeader(value = "X-User-Id", required = false) String userId, @RequestBody Object requestBody) {
        log.info("PUT /api/v1/users/me/addresses/{} - proxy to User Service", addressId);
        try {
            restTemplate.put(userAddressUrl + "/me/addresses/" + addressId, requestBody);
            return ResponseEntity.ok().build();
        } catch (HttpClientErrorException | HttpServerErrorException ex) {
            log.error("User address proxy failed: {}", ex.getMessage());
            return ResponseEntity.status(ex.getStatusCode()).body(ex.getResponseBodyAsString());
        }
    }

    @DeleteMapping("/me/addresses/{addressId}")
    public ResponseEntity<?> deleteMyAddress(@PathVariable String addressId, @RequestHeader(value = "X-User-Id", required = false) String userId) {
        log.info("DELETE /api/v1/users/me/addresses/{} - proxy to User Service", addressId);
        try {
            restTemplate.delete(userAddressUrl + "/me/addresses/" + addressId);
            return ResponseEntity.ok().build();
        } catch (HttpClientErrorException | HttpServerErrorException ex) {
            log.error("User address proxy failed: {}", ex.getMessage());
            return ResponseEntity.status(ex.getStatusCode()).body(ex.getResponseBodyAsString());
        }
    }

    @PutMapping("/me/addresses/{addressId}/set-primary")
    public ResponseEntity<?> setMyPrimaryAddress(@PathVariable String addressId, @RequestHeader(value = "X-User-Id", required = false) String userId) {
        log.info("PUT /api/v1/users/me/addresses/{}/set-primary - proxy to User Service", addressId);
        try {
            restTemplate.put(userAddressUrl + "/me/addresses/" + addressId + "/set-primary", null);
            return ResponseEntity.ok().build();
        } catch (HttpClientErrorException | HttpServerErrorException ex) {
            log.error("User address proxy failed: {}", ex.getMessage());
            return ResponseEntity.status(ex.getStatusCode()).body(ex.getResponseBodyAsString());
        }
    }

    // Admin endpoints (any user)
    @PostMapping("/{userId}/addresses")
    public ResponseEntity<?> createUserAddress(@PathVariable String userId, @RequestBody Object requestBody) {
        log.info("POST /api/v1/users/{}/addresses - proxy to User Service (Admin)", userId);
        try {
            return restTemplate.postForEntity(userAddressUrl + "/" + userId + "/addresses", requestBody, Object.class);
        } catch (HttpClientErrorException | HttpServerErrorException ex) {
            log.error("User address proxy failed: {}", ex.getMessage());
            return ResponseEntity.status(ex.getStatusCode()).body(ex.getResponseBodyAsString());
        }
    }

    @GetMapping("/{userId}/addresses")
    public ResponseEntity<?> getUserAddresses(@PathVariable String userId) {
        log.info("GET /api/v1/users/{}/addresses - proxy to User Service (Admin)", userId);
        try {
            return restTemplate.getForEntity(userAddressUrl + "/" + userId + "/addresses", Object.class);
        } catch (HttpClientErrorException | HttpServerErrorException ex) {
            log.error("User address proxy failed: {}", ex.getMessage());
            return ResponseEntity.status(ex.getStatusCode()).body(ex.getResponseBodyAsString());
        }
    }

    @GetMapping("/{userId}/addresses/primary")
    public ResponseEntity<?> getUserPrimaryAddress(@PathVariable String userId) {
        log.info("GET /api/v1/users/{}/addresses/primary - proxy to User Service (Admin)", userId);
        try {
            return restTemplate.getForEntity(userAddressUrl + "/" + userId + "/addresses/primary", Object.class);
        } catch (HttpClientErrorException | HttpServerErrorException ex) {
            log.error("User address proxy failed: {}", ex.getMessage());
            return ResponseEntity.status(ex.getStatusCode()).body(ex.getResponseBodyAsString());
        }
    }

    @GetMapping("/{userId}/addresses/{addressId}")
    public ResponseEntity<?> getUserAddress(@PathVariable String userId, @PathVariable String addressId) {
        log.info("GET /api/v1/users/{}/addresses/{} - proxy to User Service (Admin)", userId, addressId);
        try {
            return restTemplate.getForEntity(userAddressUrl + "/" + userId + "/addresses/" + addressId, Object.class);
        } catch (HttpClientErrorException | HttpServerErrorException ex) {
            log.error("User address proxy failed: {}", ex.getMessage());
            return ResponseEntity.status(ex.getStatusCode()).body(ex.getResponseBodyAsString());
        }
    }

    @PutMapping("/{userId}/addresses/{addressId}")
    public ResponseEntity<?> updateUserAddress(@PathVariable String userId, @PathVariable String addressId, @RequestBody Object requestBody) {
        log.info("PUT /api/v1/users/{}/addresses/{} - proxy to User Service (Admin)", userId, addressId);
        try {
            restTemplate.put(userAddressUrl + "/" + userId + "/addresses/" + addressId, requestBody);
            return ResponseEntity.ok().build();
        } catch (HttpClientErrorException | HttpServerErrorException ex) {
            log.error("User address proxy failed: {}", ex.getMessage());
            return ResponseEntity.status(ex.getStatusCode()).body(ex.getResponseBodyAsString());
        }
    }

    @DeleteMapping("/{userId}/addresses/{addressId}")
    public ResponseEntity<?> deleteUserAddress(@PathVariable String userId, @PathVariable String addressId) {
        log.info("DELETE /api/v1/users/{}/addresses/{} - proxy to User Service (Admin)", userId, addressId);
        try {
            restTemplate.delete(userAddressUrl + "/" + userId + "/addresses/" + addressId);
            return ResponseEntity.ok().build();
        } catch (HttpClientErrorException | HttpServerErrorException ex) {
            log.error("User address proxy failed: {}", ex.getMessage());
            return ResponseEntity.status(ex.getStatusCode()).body(ex.getResponseBodyAsString());
        }
    }
}
