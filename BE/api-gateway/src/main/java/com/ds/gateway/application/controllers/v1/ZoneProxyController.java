package com.ds.gateway.application.controllers.v1;

import com.ds.gateway.common.interfaces.IZoneServiceClient;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * API Gateway controller delegating to business layer for Zone Service calls
 */
@Slf4j
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ZoneProxyController {

    private final IZoneServiceClient zoneServiceClient;

    private Map<String, String> extractQueryParams(HttpServletRequest request) {
        Map<String, String> params = new HashMap<>();
        if (request.getQueryString() == null) return params;
        for (String pair : request.getQueryString().split("&")) {
            String[] kv = pair.split("=", 2);
            params.put(kv[0], kv.length > 1 ? kv[1] : "");
        }
        return params;
    }

    @GetMapping("/zone/health")
    public ResponseEntity<?> health() {
        log.info("GET /api/v1/zone/health");
        return zoneServiceClient.health().thenApply(ResponseEntity::ok).join();
    }

    @GetMapping("/zones")
    public ResponseEntity<?> listZones(HttpServletRequest request) {
        Map<String, String> params = extractQueryParams(request);
        log.info("GET /api/v1/zones with params: {}", params);
        return zoneServiceClient.listZones(params).thenApply(ResponseEntity::ok).join();
    }

    @GetMapping("/zones/{id}")
    public ResponseEntity<?> getZoneById(@PathVariable String id) {
        log.info("GET /api/v1/zones/{}", id);
        return zoneServiceClient.getZoneById(id).thenApply(ResponseEntity::ok).join();
    }

    @GetMapping("/zones/code/{code}")
    public ResponseEntity<?> getZoneByCode(@PathVariable String code) {
        log.info("GET /api/v1/zones/code/{}", code);
        return zoneServiceClient.getZoneByCode(code).thenApply(ResponseEntity::ok).join();
    }

    @GetMapping("/zones/center/{centerId}")
    public ResponseEntity<?> getZonesByCenter(@PathVariable String centerId) {
        log.info("GET /api/v1/zones/center/{}", centerId);
        return zoneServiceClient.getZonesByCenter(centerId).thenApply(ResponseEntity::ok).join();
    }

    @PostMapping("/zones")
    public ResponseEntity<?> createZone(@RequestBody Object requestBody) {
        log.info("POST /api/v1/zones");
        return zoneServiceClient.createZone(requestBody).thenApply(ResponseEntity::ok).join();
    }

    @PutMapping("/zones/{id}")
    public ResponseEntity<?> updateZone(@PathVariable String id, @RequestBody Object requestBody) {
        log.info("PUT /api/v1/zones/{}", id);
        zoneServiceClient.updateZone(id, requestBody).join();
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/zones/{id}")
    public ResponseEntity<?> deleteZone(@PathVariable String id) {
        log.info("DELETE /api/v1/zones/{}", id);
        zoneServiceClient.deleteZone(id).join();
        return ResponseEntity.noContent().build();
    }
}
