package com.ds.gateway.application.controllers.v2;

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
 * API Gateway controller for Zone Service V2 endpoints
 */
@Slf4j
@RestController
@RequestMapping("/api/v2/zones")
@RequiredArgsConstructor
@AuthRequired
public class ZoneProxyControllerV2 {

    private static final String ZONE_SERVICE = "zone-service";

    private final ProxyControllerSupport proxyControllerSupport;

    @Value("${services.zone.base-url}")
    private String baseUrl;

    private String zoneV2Url;

    @PostConstruct
    private void init() {
        this.zoneV2Url = baseUrl + "/api/v2/zones";
    }

    @PostMapping
    public ResponseEntity<?> listZones(@RequestBody Object requestBody) {
        log.info("POST /api/v2/zones - proxy to Zone Service");
        return proxyControllerSupport.forward(ZONE_SERVICE, HttpMethod.POST, zoneV2Url, requestBody);
    }
}
