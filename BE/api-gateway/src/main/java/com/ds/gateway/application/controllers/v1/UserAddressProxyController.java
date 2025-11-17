package com.ds.gateway.application.controllers.v1;

import com.ds.gateway.annotations.AuthRequired;
import com.ds.gateway.application.controllers.support.ProxyControllerSupport;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Proxy controller for User Address endpoints - V1
 * Forwards requests to User Service
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@AuthRequired
public class UserAddressProxyController {

    private static final String USER_SERVICE = "user-service";

    private final ProxyControllerSupport proxyControllerSupport;

    @Value("${services.user.base-url}")
    private String userServiceBaseUrl;

    private String userAddressUrl;

    @PostConstruct
    private void init() {
        this.userAddressUrl = userServiceBaseUrl + "/api/v1/users";
    }

    // Client endpoints (current user)
    @PostMapping("/me/addresses")
    public ResponseEntity<?> createMyAddress(@RequestBody Object requestBody) {
        log.info("POST /api/v1/users/me/addresses - proxy to User Service");
        return proxyCurrentUser(HttpMethod.POST, "/me/addresses", requestBody);
    }

    @GetMapping("/me/addresses")
    public ResponseEntity<?> getMyAddresses() {
        log.info("GET /api/v1/users/me/addresses - proxy to User Service");
        return proxyCurrentUser(HttpMethod.GET, "/me/addresses", null);
    }

    @GetMapping("/me/addresses/primary")
    public ResponseEntity<?> getMyPrimaryAddress() {
        log.info("GET /api/v1/users/me/addresses/primary - proxy to User Service");
        return proxyCurrentUser(HttpMethod.GET, "/me/addresses/primary", null);
    }

    @GetMapping("/me/addresses/{addressId}")
    public ResponseEntity<?> getMyAddress(@PathVariable String addressId) {
        log.info("GET /api/v1/users/me/addresses/{} - proxy to User Service", addressId);
        return proxyCurrentUser(HttpMethod.GET, "/me/addresses/" + addressId, null);
    }

    @PutMapping("/me/addresses/{addressId}")
    public ResponseEntity<?> updateMyAddress(@PathVariable String addressId, @RequestBody Object requestBody) {
        log.info("PUT /api/v1/users/me/addresses/{} - proxy to User Service", addressId);
        return proxyCurrentUser(HttpMethod.PUT, "/me/addresses/" + addressId, requestBody);
    }

    @DeleteMapping("/me/addresses/{addressId}")
    public ResponseEntity<?> deleteMyAddress(@PathVariable String addressId) {
        log.info("DELETE /api/v1/users/me/addresses/{} - proxy to User Service", addressId);
        return proxyCurrentUser(HttpMethod.DELETE, "/me/addresses/" + addressId, null);
    }

    @PutMapping("/me/addresses/{addressId}/set-primary")
    public ResponseEntity<?> setMyPrimaryAddress(@PathVariable String addressId) {
        log.info("PUT /api/v1/users/me/addresses/{}/set-primary - proxy to User Service", addressId);
        return proxyCurrentUser(HttpMethod.PUT, "/me/addresses/" + addressId + "/set-primary", null);
    }

    // Admin endpoints (any user)
    @PostMapping("/{userId}/addresses")
    public ResponseEntity<?> createUserAddress(@PathVariable String userId, @RequestBody Object requestBody) {
        log.info("POST /api/v1/users/{}/addresses - proxy to User Service (Admin)", userId);
        return proxyControllerSupport.forward(USER_SERVICE, HttpMethod.POST, userAddressUrl + "/" + userId + "/addresses", requestBody);
    }

    @GetMapping("/{userId}/addresses")
    public ResponseEntity<?> getUserAddresses(@PathVariable String userId) {
        log.info("GET /api/v1/users/{}/addresses - proxy to User Service (Admin)", userId);
        return proxyControllerSupport.forward(USER_SERVICE, HttpMethod.GET, userAddressUrl + "/" + userId + "/addresses", null);
    }

    @GetMapping("/{userId}/addresses/primary")
    public ResponseEntity<?> getUserPrimaryAddress(@PathVariable String userId) {
        log.info("GET /api/v1/users/{}/addresses/primary - proxy to User Service (Admin)", userId);
        return proxyControllerSupport.forward(USER_SERVICE, HttpMethod.GET, userAddressUrl + "/" + userId + "/addresses/primary", null);
    }

    @GetMapping("/{userId}/addresses/{addressId}")
    public ResponseEntity<?> getUserAddress(@PathVariable String userId, @PathVariable String addressId) {
        log.info("GET /api/v1/users/{}/addresses/{} - proxy to User Service (Admin)", userId, addressId);
        return proxyControllerSupport.forward(USER_SERVICE, HttpMethod.GET, userAddressUrl + "/" + userId + "/addresses/" + addressId, null);
    }

    @PutMapping("/{userId}/addresses/{addressId}")
    public ResponseEntity<?> updateUserAddress(@PathVariable String userId, @PathVariable String addressId, @RequestBody Object requestBody) {
        log.info("PUT /api/v1/users/{}/addresses/{} - proxy to User Service (Admin)", userId, addressId);
        return proxyControllerSupport.forward(USER_SERVICE, HttpMethod.PUT, userAddressUrl + "/" + userId + "/addresses/" + addressId, requestBody);
    }

    @DeleteMapping("/{userId}/addresses/{addressId}")
    public ResponseEntity<?> deleteUserAddress(@PathVariable String userId, @PathVariable String addressId) {
        log.info("DELETE /api/v1/users/{}/addresses/{} - proxy to User Service (Admin)", userId, addressId);
        return proxyControllerSupport.forward(USER_SERVICE, HttpMethod.DELETE, userAddressUrl + "/" + userId + "/addresses/" + addressId, null);
    }

    private ResponseEntity<?> proxyCurrentUser(HttpMethod method, String pathSuffix, Object body) {
        return proxyControllerSupport.forward(USER_SERVICE, method, userAddressUrl + pathSuffix, body);
    }
}
