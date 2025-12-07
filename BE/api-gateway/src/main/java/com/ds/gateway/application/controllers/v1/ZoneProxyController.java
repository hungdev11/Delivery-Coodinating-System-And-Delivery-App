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
    public ResponseEntity<?> zoneHealth() {
        log.debug("[api-gateway] [ZoneProxyController.zoneHealth] GET /api/v1/zone/health");
        return zoneServiceClient.health().thenApply(ResponseEntity::ok).join();
    }

    @PostMapping("/zones")
    public ResponseEntity<?> listZones(@RequestBody Object requestBody) {
        log.debug("[api-gateway] [ZoneProxyController.listZones] POST /api/v1/zones");
        return zoneServiceClient.listZones(requestBody).thenApply(ResponseEntity::ok).join();
    }

    @GetMapping("/zones/{id}")
    public ResponseEntity<?> getZoneById(@PathVariable String id) {
        log.debug("[api-gateway] [ZoneProxyController.getZoneById] GET /api/v1/zones/{}", id);
        return zoneServiceClient.getZoneById(id).thenApply(ResponseEntity::ok).join();
    }

    @GetMapping("/zones/code/{code}")
    public ResponseEntity<?> getZoneByCode(@PathVariable String code) {
        log.debug("[api-gateway] [ZoneProxyController.getZoneByCode] GET /api/v1/zones/code/{}", code);
        return zoneServiceClient.getZoneByCode(code).thenApply(ResponseEntity::ok).join();
    }

    @GetMapping("/zones/center/{centerId}")
    public ResponseEntity<?> getZonesByCenter(@PathVariable String centerId) {
        log.debug("[api-gateway] [ZoneProxyController.getZonesByCenter] GET /api/v1/zones/center/{}", centerId);
        return zoneServiceClient.getZonesByCenter(centerId).thenApply(ResponseEntity::ok).join();
    }

    @PostMapping("/zones/create")
    public ResponseEntity<?> createZone(@RequestBody Object requestBody) {
        log.debug("[api-gateway] [ZoneProxyController.createZone] POST /api/v1/zones/create");
        return zoneServiceClient.createZone(requestBody).thenApply(ResponseEntity::ok).join();
    }

    @PutMapping("/zones/{id}")
    public ResponseEntity<?> updateZone(@PathVariable String id, @RequestBody Object requestBody) {
        log.debug("[api-gateway] [ZoneProxyController.updateZone] PUT /api/v1/zones/{}", id);
        zoneServiceClient.updateZone(id, requestBody).join();
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/zones/{id}")
    public ResponseEntity<?> deleteZone(@PathVariable String id) {
        log.debug("[api-gateway] [ZoneProxyController.deleteZone] DELETE /api/v1/zones/{}", id);
        zoneServiceClient.deleteZone(id).join();
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/zones/filterable-fields")
    public ResponseEntity<?> getFilterableFields() {
        log.debug("[api-gateway] [ZoneProxyController.getFilterableFields] GET /api/v1/zones/filterable-fields");
        return zoneServiceClient.getFilterableFields().thenApply(ResponseEntity::ok).join();
    }

    @GetMapping("/zones/sortable-fields")
    public ResponseEntity<?> getSortableFields() {
        log.debug("[api-gateway] [ZoneProxyController.getSortableFields] GET /api/v1/zones/sortable-fields");
        return zoneServiceClient.getSortableFields().thenApply(ResponseEntity::ok).join();
    }

    // Center endpoints
    @GetMapping("/centers")
    public ResponseEntity<?> listCenters(HttpServletRequest request) {
        Map<String, String> params = extractQueryParams(request);
        log.debug("[api-gateway] [ZoneProxyController.listCenters] GET /api/v1/centers with params: {}", params);
        return zoneServiceClient.listCenters(params).thenApply(ResponseEntity::ok).join();
    }

    @GetMapping("/centers/{id}")
    public ResponseEntity<?> getCenterById(@PathVariable String id) {
        log.debug("[api-gateway] [ZoneProxyController.getCenterById] GET /api/v1/centers/{}", id);
        return zoneServiceClient.getCenterById(id).thenApply(ResponseEntity::ok).join();
    }

    @GetMapping("/centers/code/{code}")
    public ResponseEntity<?> getCenterByCode(@PathVariable String code) {
        log.debug("[api-gateway] [ZoneProxyController.getCenterByCode] GET /api/v1/centers/code/{}", code);
        return zoneServiceClient.getCenterByCode(code).thenApply(ResponseEntity::ok).join();
    }

    @PostMapping("/centers")
    public ResponseEntity<?> createCenter(@RequestBody Object requestBody) {
        log.debug("[api-gateway] [ZoneProxyController.createCenter] POST /api/v1/centers");
        return zoneServiceClient.createCenter(requestBody).thenApply(ResponseEntity::ok).join();
    }

    @PutMapping("/centers/{id}")
    public ResponseEntity<?> updateCenter(@PathVariable String id, @RequestBody Object requestBody) {
        log.debug("[api-gateway] [ZoneProxyController.updateCenter] PUT /api/v1/centers/{}", id);
        zoneServiceClient.updateCenter(id, requestBody).join();
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/centers/{id}")
    public ResponseEntity<?> deleteCenter(@PathVariable String id) {
        log.debug("[api-gateway] [ZoneProxyController.deleteCenter] DELETE /api/v1/centers/{}", id);
        zoneServiceClient.deleteCenter(id).join();
        return ResponseEntity.noContent().build();
    }

    // Routing endpoints
    @PostMapping("/routing/route")
    public ResponseEntity<?> calculateRoute(@RequestBody Object requestBody) {
        log.debug("[api-gateway] [ZoneProxyController.calculateRoute] POST /api/v1/routing/route");
        return zoneServiceClient.calculateRoute(requestBody).thenApply(ResponseEntity::ok).join();
    }

    @PostMapping("/routing/demo-route")
    public ResponseEntity<?> calculateDemoRoute(@RequestBody Object requestBody) {
        log.debug("[api-gateway] [ZoneProxyController.calculateDemoRoute] POST /api/v1/routing/demo-route");
        return zoneServiceClient.calculateDemoRoute(requestBody).thenApply(ResponseEntity::ok).join();
    }

    @GetMapping("/routing/osrm-status")
    public ResponseEntity<?> getOsrmStatus() {
        log.debug("[api-gateway] [ZoneProxyController.getOsrmStatus] GET /api/v1/routing/osrm-status");
        return zoneServiceClient.getOsrmStatus().thenApply(ResponseEntity::ok).join();
    }

    // Address endpoints
    @GetMapping("/addresses")
    public ResponseEntity<?> listAddresses(HttpServletRequest request) {
        Map<String, String> params = extractQueryParams(request);
        log.debug("[api-gateway] [ZoneProxyController.listAddresses] GET /api/v1/addresses with params: {}", params);
        return zoneServiceClient.listAddresses(params).thenApply(ResponseEntity::ok).join();
    }

    @GetMapping("/addresses/{id}")
    public ResponseEntity<?> getAddressById(@PathVariable String id) {
        log.debug("[api-gateway] [ZoneProxyController.getAddressById] GET /api/v1/addresses/{}", id);
        return zoneServiceClient.getAddressById(id).thenApply(ResponseEntity::ok).join();
    }

    @GetMapping("/addresses/nearest")
    public ResponseEntity<?> getNearestAddresses(HttpServletRequest request) {
        Map<String, String> params = extractQueryParams(request);
        log.debug("[api-gateway] [ZoneProxyController.getNearestAddresses] GET /api/v1/addresses/nearest with params: {}", params);
        return zoneServiceClient.getNearestAddresses(params).thenApply(ResponseEntity::ok).join();
    }

    @GetMapping("/addresses/by-point")
    public ResponseEntity<?> getAddressesByPoint(HttpServletRequest request) {
        Map<String, String> params = extractQueryParams(request);
        log.debug("[api-gateway] [ZoneProxyController.getAddressesByPoint] GET /api/v1/addresses/by-point with params: {}", params);
        return zoneServiceClient.getAddressesByPoint(params).thenApply(ResponseEntity::ok).join();
    }

    @GetMapping("/addresses/search")
    public ResponseEntity<?> searchAddresses(HttpServletRequest request) {
        Map<String, String> params = extractQueryParams(request);
        log.debug("[api-gateway] [ZoneProxyController.searchAddresses] GET /api/v1/addresses/search with params: {}", params);
        return zoneServiceClient.searchAddresses(params).thenApply(ResponseEntity::ok).join();
    }

    @PostMapping("/addresses")
    public ResponseEntity<?> createAddress(@RequestBody Object requestBody) {
        log.debug("[api-gateway] [ZoneProxyController.createAddress] POST /api/v1/addresses");
        return zoneServiceClient.createAddress(requestBody).thenApply(ResponseEntity::ok).join();
    }

    @PostMapping("/addresses/get-or-create")
    public ResponseEntity<?> getOrCreateAddress(@RequestBody Object requestBody, HttpServletRequest request) {
        Map<String, String> params = extractQueryParams(request);
        log.debug("[api-gateway] [ZoneProxyController.getOrCreateAddress] POST /api/v1/addresses/get-or-create with params: {}", params);
        return zoneServiceClient.getOrCreateAddress(requestBody, params).thenApply(ResponseEntity::ok).join();
    }

    @PutMapping("/addresses/{id}")
    public ResponseEntity<?> updateAddress(@PathVariable String id, @RequestBody Object requestBody) {
        log.debug("[api-gateway] [ZoneProxyController.updateAddress] PUT /api/v1/addresses/{}", id);
        return zoneServiceClient.updateAddress(id, requestBody).thenApply(ResponseEntity::ok).join();
    }

    @DeleteMapping("/addresses/{id}")
    public ResponseEntity<?> deleteAddress(@PathVariable String id) {
        log.debug("[api-gateway] [ZoneProxyController.deleteAddress] DELETE /api/v1/addresses/{}", id);
        zoneServiceClient.deleteAddress(id).join();
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/addresses/batch")
    public ResponseEntity<?> batchImportAddresses(@RequestBody Object requestBody) {
        log.debug("[api-gateway] [ZoneProxyController.batchImportAddresses] POST /api/v1/addresses/batch");
        return zoneServiceClient.batchImportAddresses(requestBody).thenApply(ResponseEntity::ok).join();
    }

    @GetMapping("/addresses/segments/{segmentId}")
    public ResponseEntity<?> getAddressesBySegment(@PathVariable String segmentId, HttpServletRequest request) {
        Map<String, String> params = extractQueryParams(request);
        log.debug("[api-gateway] [ZoneProxyController.getAddressesBySegment] GET /api/v1/addresses/segments/{} with params: {}", segmentId, params);
        return zoneServiceClient.getAddressesBySegment(segmentId, params).thenApply(ResponseEntity::ok).join();
    }

    @GetMapping("/addresses/zones/{zoneId}")
    public ResponseEntity<?> getAddressesByZone(@PathVariable String zoneId, HttpServletRequest request) {
        Map<String, String> params = extractQueryParams(request);
        log.debug("[api-gateway] [ZoneProxyController.getAddressesByZone] GET /api/v1/addresses/zones/{} with params: {}", zoneId, params);
        return zoneServiceClient.getAddressesByZone(zoneId, params).thenApply(ResponseEntity::ok).join();
    }

    // OSRM Data Management endpoints
    @PostMapping("/osrm/build/{instanceId}")
    public ResponseEntity<?> buildOSRMInstance(@PathVariable String instanceId) {
        log.debug("[api-gateway] [ZoneProxyController.buildOSRMInstance] POST /api/v1/osrm/build/{}", instanceId);
        return zoneServiceClient.buildOSRMInstance(instanceId).thenApply(ResponseEntity::ok).join();
    }

    @PostMapping("/osrm/build-all")
    public ResponseEntity<?> buildAllOSRMInstances() {
        log.debug("[api-gateway] [ZoneProxyController.buildAllOSRMInstances] POST /api/v1/osrm/build-all");
        try {
            return zoneServiceClient.buildAllOSRMInstances().thenApply(ResponseEntity::ok).join();
        } catch (Exception e) {
            log.error("[api-gateway] [ZoneProxyController.buildAllOSRMInstances] Error building OSRM instances", e);
            throw e; // Re-throw to be handled by GlobalExceptionHandler
        }
    }

    @PostMapping("/osrm/start/{instanceId}")
    public ResponseEntity<?> startOSRMInstance(@PathVariable String instanceId) {
        log.debug("[api-gateway] [ZoneProxyController.startOSRMInstance] POST /api/v1/osrm/start/{}", instanceId);
        return zoneServiceClient.startOSRMInstance(instanceId).thenApply(ResponseEntity::ok).join();
    }

    @PostMapping("/osrm/stop/{instanceId}")
    public ResponseEntity<?> stopOSRMInstance(@PathVariable String instanceId) {
        log.debug("[api-gateway] [ZoneProxyController.stopOSRMInstance] POST /api/v1/osrm/stop/{}", instanceId);
        return zoneServiceClient.stopOSRMInstance(instanceId).thenApply(ResponseEntity::ok).join();
    }

    @PostMapping("/osrm/rolling-restart")
    public ResponseEntity<?> rollingRestartOSRM() {
        log.debug("[api-gateway] [ZoneProxyController.rollingRestartOSRM] POST /api/v1/osrm/rolling-restart");
        return zoneServiceClient.rollingRestartOSRM().thenApply(ResponseEntity::ok).join();
    }

    @GetMapping("/osrm/status")
    public ResponseEntity<?> getAllOSRMInstancesStatus() {
        log.debug("[api-gateway] [ZoneProxyController.getAllOSRMInstancesStatus] GET /api/v1/osrm/status");
        return zoneServiceClient.getAllOSRMInstancesStatus().thenApply(ResponseEntity::ok).join();
    }

    @GetMapping("/osrm/status/{instanceId}")
    public ResponseEntity<?> getOSRMInstanceStatus(@PathVariable String instanceId) {
        log.debug("[api-gateway] [ZoneProxyController.getOSRMInstanceStatus] GET /api/v1/osrm/status/{}", instanceId);
        return zoneServiceClient.getOSRMInstanceStatus(instanceId).thenApply(ResponseEntity::ok).join();
    }

    @GetMapping("/osrm/health")
    public ResponseEntity<?> getOSRMHealthCheck() {
        log.debug("[api-gateway] [ZoneProxyController.getOSRMHealthCheck] GET /api/v1/osrm/health");
        return zoneServiceClient.getOSRMHealthCheck().thenApply(ResponseEntity::ok).join();
    }

    @GetMapping("/osrm/validate/{instanceId}")
    public ResponseEntity<?> validateOSRMData(@PathVariable String instanceId) {
        log.debug("[api-gateway] [ZoneProxyController.validateOSRMData] GET /api/v1/osrm/validate/{}", instanceId);
        return zoneServiceClient.validateOSRMData(instanceId).thenApply(ResponseEntity::ok).join();
    }

    @GetMapping("/osrm/history/{instanceId}")
    public ResponseEntity<?> getOSRMBuildHistory(@PathVariable String instanceId) {
        log.debug("[api-gateway] [ZoneProxyController.getOSRMBuildHistory] GET /api/v1/osrm/history/{}", instanceId);
        return zoneServiceClient.getOSRMBuildHistory(instanceId).thenApply(ResponseEntity::ok).join();
    }

    @GetMapping("/osrm/history")
    public ResponseEntity<?> getAllOSRMBuildHistory() {
        log.debug("[api-gateway] [ZoneProxyController.getAllOSRMBuildHistory] GET /api/v1/osrm/history");
        return zoneServiceClient.getAllOSRMBuildHistory().thenApply(ResponseEntity::ok).join();
    }

    @GetMapping("/osrm/deployment")
    public ResponseEntity<?> getOSRMDeploymentStatus() {
        log.debug("[api-gateway] [ZoneProxyController.getOSRMDeploymentStatus] GET /api/v1/osrm/deployment");
        return zoneServiceClient.getOSRMDeploymentStatus().thenApply(ResponseEntity::ok).join();
    }
}
