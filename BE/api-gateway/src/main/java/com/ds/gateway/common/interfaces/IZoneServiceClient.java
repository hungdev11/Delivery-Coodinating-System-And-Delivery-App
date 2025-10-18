package com.ds.gateway.common.interfaces;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Contract for Zone Service REST client
 */
public interface IZoneServiceClient {
    // Zone endpoints
    CompletableFuture<Object> listZones(Map<String, String> queryParams);
    CompletableFuture<Object> getZoneById(String id);
    CompletableFuture<Object> getZoneByCode(String code);
    CompletableFuture<Object> getZonesByCenter(String centerId);
    CompletableFuture<Object> createZone(Object requestBody);
    CompletableFuture<Void> updateZone(String id, Object requestBody);
    CompletableFuture<Void> deleteZone(String id);
    
    // Center endpoints
    CompletableFuture<Object> listCenters(Map<String, String> queryParams);
    CompletableFuture<Object> getCenterById(String id);
    CompletableFuture<Object> getCenterByCode(String code);
    CompletableFuture<Object> createCenter(Object requestBody);
    CompletableFuture<Void> updateCenter(String id, Object requestBody);
    CompletableFuture<Void> deleteCenter(String id);
    
    // Routing endpoints
    CompletableFuture<Object> calculateRoute(Object requestBody);
    CompletableFuture<Object> getOsrmStatus();
    
    // Address endpoints
    CompletableFuture<Object> listAddresses(Map<String, String> queryParams);
    CompletableFuture<Object> getAddressById(String id);
    CompletableFuture<Object> getNearestAddresses(Map<String, String> queryParams);
    CompletableFuture<Object> createAddress(Object requestBody);
    CompletableFuture<Object> updateAddress(String id, Object requestBody);
    CompletableFuture<Void> deleteAddress(String id);
    CompletableFuture<Object> batchImportAddresses(Object requestBody);
    CompletableFuture<Object> getAddressesBySegment(String segmentId, Map<String, String> queryParams);
    CompletableFuture<Object> getAddressesByZone(String zoneId, Map<String, String> queryParams);
    
    // OSRM Data Management endpoints
    CompletableFuture<Object> buildOSRMInstance(String instanceId);
    CompletableFuture<Object> buildAllOSRMInstances();
    CompletableFuture<Object> startOSRMInstance(String instanceId);
    CompletableFuture<Object> stopOSRMInstance(String instanceId);
    CompletableFuture<Object> rollingRestartOSRM();
    CompletableFuture<Object> getOSRMInstanceStatus(String instanceId);
    CompletableFuture<Object> getAllOSRMInstancesStatus();
    CompletableFuture<Object> getOSRMHealthCheck();
    CompletableFuture<Object> validateOSRMData(String instanceId);
    
    // Health endpoint
    CompletableFuture<Object> health();
}
