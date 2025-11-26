package com.ds.gateway.application.controllers.v0;

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
 * Parcel proxy controller for V0 endpoints (simple paging)
 */
@Slf4j
@RestController
@RequestMapping("/api/v0/parcels")
@RequiredArgsConstructor
public class ParcelServiceControllerV0 {

    private static final String PARCEL_SERVICE = "parcel-service";

    private final ProxyControllerSupport proxyControllerSupport;

    @Value("${services.parcel.base-url}")
    private String baseUrl;

    private String parcelV0Url;

    @PostConstruct
    private void init() {
        this.parcelV0Url = baseUrl + "/api/v0/parcels";
    }

    @PostMapping
    public ResponseEntity<?> listParcels(@RequestBody Object request) {
        log.debug("[api-gateway] [ParcelServiceControllerV0.listParcels] POST /api/v0/parcels - proxy to Parcel Service");
        return proxyControllerSupport.forward(PARCEL_SERVICE, HttpMethod.POST, parcelV0Url, request);
    }
}
