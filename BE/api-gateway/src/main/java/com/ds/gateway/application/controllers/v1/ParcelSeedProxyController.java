package com.ds.gateway.application.controllers.v1;

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
 * Proxy controller for Parcel Seed endpoints - V1
 * Forwards requests to User Service
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/parcels/seed")
@RequiredArgsConstructor
public class ParcelSeedProxyController {

    private static final String USER_SERVICE = "user-service";

    private final ProxyControllerSupport proxyControllerSupport;

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
        log.debug("[api-gateway] [ParcelSeedProxyController.seedParcels] POST /api/v1/parcels/seed - proxy to User Service");
        return proxyControllerSupport.forward(USER_SERVICE, HttpMethod.POST, parcelSeedUrl, requestBody);
    }
}
