package com.ds.gateway.common.interfaces;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Contract for Zone Service REST client
 */
public interface IZoneServiceClient {
    // Zone endpoints
    CompletableFuture<Object> listZonesV0(Object requestBody);
    CompletableFuture<Object> listZones(Object requestBody);
    CompletableFuture<Object> listZonesV2(Object requestBody);
    CompletableFuture<Object> getZoneById(String id);
    CompletableFuture<Object> getZoneByCode(String code);
    CompletableFuture<Object> getZonesByCenter(String centerId);
    CompletableFuture<Object> createZone(Object requestBody);
    CompletableFuture<Void> updateZone(String id, Object requestBody);
    CompletableFuture<Void> deleteZone(String id);
    CompletableFuture<Object> getFilterableFields();
    CompletableFuture<Object> getSortableFields();
    
    // Center endpoints
    CompletableFuture<Object> listCenters(Map<String, String> queryParams);
    CompletableFuture<Object> getCenterById(String id);
    CompletableFuture<Object> getCenterByCode(String code);
    CompletableFuture<Object> createCenter(Object requestBody);
    CompletableFuture<Void> updateCenter(String id, Object requestBody);
    CompletableFuture<Void> deleteCenter(String id);
    
    // Routing endpoints
    CompletableFuture<Object> calculateRoute(Object requestBody);
    CompletableFuture<Object> calculateDemoRoute(Object requestBody);
    CompletableFuture<Object> getOsrmStatus();
    
    // Address endpoints
    CompletableFuture<Object> listAddresses(Map<String, String> queryParams);
    CompletableFuture<Object> getAddressById(String id);
    CompletableFuture<Object> getNearestAddresses(Map<String, String> queryParams);
    CompletableFuture<Object> getAddressesByPoint(Map<String, String> queryParams);
    CompletableFuture<Object> searchAddresses(Map<String, String> queryParams);
    CompletableFuture<Object> createAddress(Object requestBody);
    CompletableFuture<Object> getOrCreateAddress(Object requestBody, Map<String, String> queryParams);
    CompletableFuture<Object> updateAddress(String id, Object requestBody);
    CompletableFuture<Void> deleteAddress(String id);
    CompletableFuture<Object> batchImportAddresses(Object requestBody);
    CompletableFuture<Object> getAddressesBySegment(String segmentId, Map<String, String> queryParams);
    CompletableFuture<Object> getAddressesByZone(String zoneId, Map<String, String> queryParams);
    
    // OSRM Data Management endpoints (V2 - simplified)
    CompletableFuture<Object> generateV2OSRM();
    CompletableFuture<Object> getOSRMStatus();
    
    // Health endpoint
    CompletableFuture<Object> health();
}
