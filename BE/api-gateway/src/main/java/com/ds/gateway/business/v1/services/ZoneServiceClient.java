package com.ds.gateway.business.v1.services;

import com.ds.gateway.common.entities.dto.common.BaseResponse;
import com.ds.gateway.common.entities.dto.common.PagedData;
import com.ds.gateway.common.exceptions.ServiceUnavailableException;
import com.ds.gateway.common.interfaces.IZoneServiceClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * REST client implementation for Zone Service
 */
@Slf4j
@Service
public class ZoneServiceClient implements IZoneServiceClient {

    @Autowired
    @Qualifier("zoneServiceWebClient")
    private WebClient zoneServiceWebClient;

    @Override
    public CompletableFuture<Object> listZonesV0(Object requestBody) {
        log.debug("Listing zones V0 with simple paging request body: {}", requestBody);

        return zoneServiceWebClient.post()
                .uri("/api/v0/zones")
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(requestBody))
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<BaseResponse<PagedData<Object>>>() {})
                .map(response -> (Object) response.getResult())
                .onErrorMap(ex -> new ServiceUnavailableException("Zone service unavailable: " + ex.getMessage(), ex))
                .toFuture();
    }

    @Override
    public CompletableFuture<Object> listZones(Object requestBody) {
        log.debug("Listing zones V1 with request body: {}", requestBody);

        return zoneServiceWebClient.post()
                .uri("/api/v1/zones")
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(requestBody))
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<BaseResponse<PagedData<Object>>>() {})
                .map(response -> (Object) response.getResult())
                .onErrorMap(ex -> new ServiceUnavailableException("Zone service unavailable: " + ex.getMessage(), ex))
                .toFuture();
    }

    @Override
    public CompletableFuture<Object> listZonesV2(Object requestBody) {
        log.debug("Listing zones V2 with enhanced filtering body: {}", requestBody);

        return zoneServiceWebClient.post()
                .uri("/api/v2/zones")
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(requestBody))
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<BaseResponse<PagedData<Object>>>() {})
                .map(response -> (Object) response.getResult())
                .onErrorMap(ex -> new ServiceUnavailableException("Zone service unavailable: " + ex.getMessage(), ex))
                .toFuture();
    }

    @Override
    public CompletableFuture<Object> getZoneById(String id) {
        return zoneServiceWebClient.get()
                .uri("/api/v1/zones/{id}", id)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Object>() {})
                .onErrorMap(ex -> new ServiceUnavailableException("Zone service unavailable: " + ex.getMessage(), ex))
                .toFuture();
    }

    @Override
    public CompletableFuture<Object> getZoneByCode(String code) {
        return zoneServiceWebClient.get()
                .uri("/api/v1/zones/code/{code}", code)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Object>() {})
                .onErrorMap(ex -> new ServiceUnavailableException("Zone service unavailable: " + ex.getMessage(), ex))
                .toFuture();
    }

    @Override
    public CompletableFuture<Object> getZonesByCenter(String centerId) {
        return zoneServiceWebClient.get()
                .uri("/api/v1/zones/center/{centerId}", centerId)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Object>() {})
                .onErrorMap(ex -> new ServiceUnavailableException("Zone service unavailable: " + ex.getMessage(), ex))
                .toFuture();
    }

    @Override
    public CompletableFuture<Object> createZone(Object requestBody) {
        return zoneServiceWebClient.post()
                .uri("/api/v1/zones/create")
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(requestBody))
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<BaseResponse<Object>>() {})
                .map(BaseResponse::getResult)
                .onErrorMap(ex -> new ServiceUnavailableException("Zone service unavailable: " + ex.getMessage(), ex))
                .toFuture();
    }

    @Override
    public CompletableFuture<Void> updateZone(String id, Object requestBody) {
        return zoneServiceWebClient.put()
                .uri("/api/v1/zones/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(requestBody))
                .retrieve()
                .bodyToMono(Void.class)
                .onErrorMap(ex -> new ServiceUnavailableException("Zone service unavailable: " + ex.getMessage(), ex))
                .toFuture();
    }

    @Override
    public CompletableFuture<Void> deleteZone(String id) {
        return zoneServiceWebClient.delete()
                .uri("/api/v1/zones/{id}", id)
                .retrieve()
                .bodyToMono(Void.class)
                .onErrorMap(ex -> new ServiceUnavailableException("Zone service unavailable: " + ex.getMessage(), ex))
                .toFuture();
    }

    // Center endpoints
    @Override
    public CompletableFuture<Object> listCenters(Map<String, String> queryParams) {
        log.debug("Listing centers with params: {}", queryParams);
        WebClient.RequestHeadersUriSpec<?> req = zoneServiceWebClient.get();
        WebClient.RequestHeadersSpec<?> spec = req.uri(uriBuilder -> {
            var builder = uriBuilder.path("/api/v1/centers");
            if (queryParams != null) {
                queryParams.forEach(builder::queryParam);
            }
            return builder.build();
        });
        return spec.retrieve()
                .bodyToMono(new ParameterizedTypeReference<Object>() {})
                .onErrorMap(ex -> new ServiceUnavailableException("Zone service unavailable: " + ex.getMessage(), ex))
                .toFuture();
    }

    @Override
    public CompletableFuture<Object> getCenterById(String id) {
        return zoneServiceWebClient.get()
                .uri("/api/v1/centers/{id}", id)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Object>() {})
                .onErrorMap(ex -> new ServiceUnavailableException("Zone service unavailable: " + ex.getMessage(), ex))
                .toFuture();
    }

    @Override
    public CompletableFuture<Object> getCenterByCode(String code) {
        return zoneServiceWebClient.get()
                .uri("/api/v1/centers/code/{code}", code)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Object>() {})
                .onErrorMap(ex -> new ServiceUnavailableException("Zone service unavailable: " + ex.getMessage(), ex))
                .toFuture();
    }

    @Override
    public CompletableFuture<Object> createCenter(Object requestBody) {
        return zoneServiceWebClient.post()
                .uri("/api/v1/centers")
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(requestBody))
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Object>() {})
                .onErrorMap(ex -> new ServiceUnavailableException("Zone service unavailable: " + ex.getMessage(), ex))
                .toFuture();
    }

    @Override
    public CompletableFuture<Void> updateCenter(String id, Object requestBody) {
        return zoneServiceWebClient.put()
                .uri("/api/v1/centers/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(requestBody))
                .retrieve()
                .bodyToMono(Void.class)
                .onErrorMap(ex -> new ServiceUnavailableException("Zone service unavailable: " + ex.getMessage(), ex))
                .toFuture();
    }

    @Override
    public CompletableFuture<Void> deleteCenter(String id) {
        return zoneServiceWebClient.delete()
                .uri("/api/v1/centers/{id}", id)
                .retrieve()
                .bodyToMono(Void.class)
                .onErrorMap(ex -> new ServiceUnavailableException("Zone service unavailable: " + ex.getMessage(), ex))
                .toFuture();
    }

    // Routing endpoints
    @Override
    public CompletableFuture<Object> calculateRoute(Object requestBody) {
        return zoneServiceWebClient.post()
                .uri("/api/v1/routing/route")
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(requestBody))
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Object>() {})
                .onErrorMap(ex -> new ServiceUnavailableException("Zone service unavailable: " + ex.getMessage(), ex))
                .toFuture();
    }

    @Override
    public CompletableFuture<Object> calculateDemoRoute(Object requestBody) {
        return zoneServiceWebClient.post()
                .uri("/api/v1/routing/demo-route")
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(requestBody))
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Object>() {})
                .onErrorMap(ex -> new ServiceUnavailableException("Zone service unavailable: " + ex.getMessage(), ex))
                .toFuture();
    }

    @Override
    public CompletableFuture<Object> getOsrmStatus() {
        return zoneServiceWebClient.get()
                .uri("/api/v1/routing/osrm-status")
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Object>() {})
                .onErrorMap(ex -> new ServiceUnavailableException("Zone service unavailable: " + ex.getMessage(), ex))
                .toFuture();
    }

    // Address endpoints
    @Override
    public CompletableFuture<Object> listAddresses(Map<String, String> queryParams) {
        log.debug("Listing addresses with params: {}", queryParams);
        WebClient.RequestHeadersUriSpec<?> req = zoneServiceWebClient.get();
        WebClient.RequestHeadersSpec<?> spec = req.uri(uriBuilder -> {
            var builder = uriBuilder.path("/api/v1/addresses");
            if (queryParams != null) {
                queryParams.forEach(builder::queryParam);
            }
            return builder.build();
        });
        return spec.retrieve()
                .bodyToMono(new ParameterizedTypeReference<Object>() {})
                .onErrorMap(ex -> new ServiceUnavailableException("Zone service unavailable: " + ex.getMessage(), ex))
                .toFuture();
    }

    @Override
    public CompletableFuture<Object> getAddressById(String id) {
        return zoneServiceWebClient.get()
                .uri("/api/v1/addresses/{id}", id)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Object>() {})
                .onErrorMap(ex -> new ServiceUnavailableException("Zone service unavailable: " + ex.getMessage(), ex))
                .toFuture();
    }

    @Override
    public CompletableFuture<Object> getNearestAddresses(Map<String, String> queryParams) {
        log.debug("Getting nearest addresses with params: {}", queryParams);
        WebClient.RequestHeadersUriSpec<?> req = zoneServiceWebClient.get();
        WebClient.RequestHeadersSpec<?> spec = req.uri(uriBuilder -> {
            var builder = uriBuilder.path("/api/v1/addresses/nearest");
            if (queryParams != null) {
                queryParams.forEach(builder::queryParam);
            }
            return builder.build();
        });
        return spec.retrieve()
                .bodyToMono(new ParameterizedTypeReference<Object>() {})
                .onErrorMap(ex -> new ServiceUnavailableException("Zone service unavailable: " + ex.getMessage(), ex))
                .toFuture();
    }

    @Override
    public CompletableFuture<Object> getAddressesByPoint(Map<String, String> queryParams) {
        log.debug("Getting addresses by point with params: {}", queryParams);
        WebClient.RequestHeadersUriSpec<?> req = zoneServiceWebClient.get();
        WebClient.RequestHeadersSpec<?> spec = req.uri(uriBuilder -> {
            var builder = uriBuilder.path("/api/v1/addresses/by-point");
            if (queryParams != null) {
                queryParams.forEach(builder::queryParam);
            }
            return builder.build();
        });
        return spec.retrieve()
                .bodyToMono(new ParameterizedTypeReference<Object>() {})
                .onErrorMap(ex -> new ServiceUnavailableException("Zone service unavailable: " + ex.getMessage(), ex))
                .toFuture();
    }

    @Override
    public CompletableFuture<Object> searchAddresses(Map<String, String> queryParams) {
        log.debug("Searching addresses with params: {}", queryParams);
        WebClient.RequestHeadersUriSpec<?> req = zoneServiceWebClient.get();
        WebClient.RequestHeadersSpec<?> spec = req.uri(uriBuilder -> {
            var builder = uriBuilder.path("/api/v1/addresses/search");
            if (queryParams != null) {
                queryParams.forEach(builder::queryParam);
            }
            return builder.build();
        });
        return spec.retrieve()
                .bodyToMono(new ParameterizedTypeReference<Object>() {})
                .onErrorMap(ex -> new ServiceUnavailableException("Zone service unavailable: " + ex.getMessage(), ex))
                .toFuture();
    }

    @Override
    public CompletableFuture<Object> createAddress(Object requestBody) {
        return zoneServiceWebClient.post()
                .uri("/api/v1/addresses")
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(requestBody))
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Object>() {})
                .onErrorMap(ex -> new ServiceUnavailableException("Zone service unavailable: " + ex.getMessage(), ex))
                .toFuture();
    }

    @Override
    public CompletableFuture<Object> getOrCreateAddress(Object requestBody, Map<String, String> queryParams) {
        log.debug("Getting or creating address with params: {}", queryParams);
        WebClient.RequestBodyUriSpec req = zoneServiceWebClient.post();
        WebClient.RequestBodySpec spec = req.uri(uriBuilder -> {
            var builder = uriBuilder.path("/api/v1/addresses/get-or-create");
            if (queryParams != null) {
                queryParams.forEach(builder::queryParam);
            }
            return builder.build();
        });
        return spec.contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(requestBody))
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Object>() {})
                .onErrorMap(ex -> new ServiceUnavailableException("Zone service unavailable: " + ex.getMessage(), ex))
                .toFuture();
    }

    @Override
    public CompletableFuture<Object> updateAddress(String id, Object requestBody) {
        return zoneServiceWebClient.put()
                .uri("/api/v1/addresses/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(requestBody))
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Object>() {})
                .onErrorMap(ex -> new ServiceUnavailableException("Zone service unavailable: " + ex.getMessage(), ex))
                .toFuture();
    }

    @Override
    public CompletableFuture<Void> deleteAddress(String id) {
        return zoneServiceWebClient.delete()
                .uri("/api/v1/addresses/{id}", id)
                .retrieve()
                .bodyToMono(Void.class)
                .onErrorMap(ex -> new ServiceUnavailableException("Zone service unavailable: " + ex.getMessage(), ex))
                .toFuture();
    }

    @Override
    public CompletableFuture<Object> batchImportAddresses(Object requestBody) {
        return zoneServiceWebClient.post()
                .uri("/api/v1/addresses/batch")
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(requestBody))
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Object>() {})
                .onErrorMap(ex -> new ServiceUnavailableException("Zone service unavailable: " + ex.getMessage(), ex))
                .toFuture();
    }

    @Override
    public CompletableFuture<Object> getAddressesBySegment(String segmentId, Map<String, String> queryParams) {
        log.debug("Getting addresses by segment {} with params: {}", segmentId, queryParams);
        return zoneServiceWebClient.get()
                .uri(uriBuilder -> {
                    var builder = uriBuilder.path("/api/v1/addresses/segments/{segmentId}");
                    if (queryParams != null) {
                        queryParams.forEach(builder::queryParam);
                    }
                    return builder.build(segmentId);
                })
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Object>() {})
                .onErrorMap(ex -> new ServiceUnavailableException("Zone service unavailable: " + ex.getMessage(), ex))
                .toFuture();
    }

    @Override
    public CompletableFuture<Object> getAddressesByZone(String zoneId, Map<String, String> queryParams) {
        log.debug("Getting addresses by zone {} with params: {}", zoneId, queryParams);
        return zoneServiceWebClient.get()
                .uri(uriBuilder -> {
                    var builder = uriBuilder.path("/api/v1/addresses/zones/{zoneId}");
                    if (queryParams != null) {
                        queryParams.forEach(builder::queryParam);
                    }
                    return builder.build(zoneId);
                })
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Object>() {})
                .onErrorMap(ex -> new ServiceUnavailableException("Zone service unavailable: " + ex.getMessage(), ex))
                .toFuture();
    }

    // OSRM Data Management endpoints (V2 - simplified)
    @Override
    public CompletableFuture<Object> generateV2OSRM() {
        log.debug("Generating OSRM V2 data (all 4 models) from database");
        return zoneServiceWebClient.post()
                .uri("/api/v1/osrm/generate-v2")
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Object>() {})
                .onErrorMap(ex -> new ServiceUnavailableException("Zone service unavailable: " + ex.getMessage(), ex))
                .toFuture();
    }

    @Override
    public CompletableFuture<Object> getOSRMStatus() {
        log.debug("Getting OSRM status");
        return zoneServiceWebClient.get()
                .uri("/api/v1/osrm/status")
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Object>() {})
                .onErrorMap(ex -> new ServiceUnavailableException("Zone service unavailable: " + ex.getMessage(), ex))
                .toFuture();
    }

    @Override
    public CompletableFuture<Object> health() {
        return zoneServiceWebClient.get()
                .uri("/health")
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Object>() {})
                .onErrorMap(ex -> new ServiceUnavailableException("Zone service unavailable: " + ex.getMessage(), ex))
                .toFuture();
    }
    
    @Override
    public CompletableFuture<Object> getFilterableFields() {
        log.debug("Getting filterable fields for zones");
        return zoneServiceWebClient.get()
                .uri("/api/v1/zones/filterable-fields")
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<BaseResponse<List<String>>>() {})
                .map(response -> (Object) response.getResult())
                .onErrorMap(ex -> new ServiceUnavailableException("Zone service unavailable: " + ex.getMessage(), ex))
                .toFuture();
    }
    
    @Override
    public CompletableFuture<Object> getSortableFields() {
        log.debug("Getting sortable fields for zones");
        return zoneServiceWebClient.get()
                .uri("/api/v1/zones/sortable-fields")
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<BaseResponse<List<String>>>() {})
                .map(response -> (Object) response.getResult())
                .onErrorMap(ex -> new ServiceUnavailableException("Zone service unavailable: " + ex.getMessage(), ex))
                .toFuture();
    }
}
