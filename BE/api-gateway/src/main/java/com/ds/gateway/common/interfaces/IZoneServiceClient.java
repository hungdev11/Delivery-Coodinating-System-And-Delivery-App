package com.ds.gateway.common.interfaces;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Contract for Zone Service REST client
 */
public interface IZoneServiceClient {
    CompletableFuture<Object> listZones(Map<String, String> queryParams);
    CompletableFuture<Object> getZoneById(String id);
    CompletableFuture<Object> getZoneByCode(String code);
    CompletableFuture<Object> getZonesByCenter(String centerId);
    CompletableFuture<Object> createZone(Object requestBody);
    CompletableFuture<Void> updateZone(String id, Object requestBody);
    CompletableFuture<Void> deleteZone(String id);
    CompletableFuture<Object> health();
}
