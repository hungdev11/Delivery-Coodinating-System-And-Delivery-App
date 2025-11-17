package com.ds.gateway.application.controllers.v0;

import com.ds.gateway.annotations.AuthRequired;
import com.ds.gateway.application.controllers.support.ProxyControllerSupport;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * User controller V0 for simple paging operations
 */
@Slf4j
@RestController
@RequestMapping("/api/v0/users")
@RequiredArgsConstructor
@AuthRequired
public class UserControllerV0 {

    private static final String USER_SERVICE = "user-service";

    private final ProxyControllerSupport proxyControllerSupport;

    @Value("${services.user.base-url}")
    private String baseUrl;

    private String userV0Url;

    @PostConstruct
    private void init() {
        this.userV0Url = baseUrl + "/api/v0/users";
    }

    @PostMapping
    public ResponseEntity<?> listUsers(@RequestBody Object requestBody) {
        log.info("POST /api/v0/users - proxy to User Service");
        return proxyControllerSupport.forward(USER_SERVICE, HttpMethod.POST, userV0Url, requestBody);
    }
}
