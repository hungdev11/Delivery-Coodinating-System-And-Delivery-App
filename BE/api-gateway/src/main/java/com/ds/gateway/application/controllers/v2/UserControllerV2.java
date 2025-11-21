package com.ds.gateway.application.controllers.v2;

import com.ds.gateway.annotations.AuthRequired;
import com.ds.gateway.application.controllers.support.ProxyControllerSupport;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

    private static final String USER_SERVICE = "user-service";

    private final ProxyControllerSupport proxyControllerSupport;

    @Value("${services.user.base-url}")
    private String baseUrl;

    private String userV2Url;
    private String currentUserUrl;

    @PostConstruct
    private void init() {
        this.userV2Url = baseUrl + "/api/v2/users";
        this.currentUserUrl = this.userV2Url + "/me";
    }

    @PostMapping
    public ResponseEntity<?> listUsers(@RequestBody Object requestBody) {
        log.info("POST /api/v2/users - proxy to User Service");
        return proxyControllerSupport.forward(USER_SERVICE, HttpMethod.POST, userV2Url, requestBody);
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser() {
        log.info("GET /api/v2/users/me - proxy to User Service");
        return proxyControllerSupport.forward(USER_SERVICE, HttpMethod.GET, currentUserUrl, null);
    }
}
