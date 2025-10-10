package com.ds.gateway.business.v1.services;

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
    public CompletableFuture<Object> listZones(Map<String, String> queryParams) {
        log.debug("Listing zones with params: {}", queryParams);
        WebClient.RequestHeadersUriSpec<?> req = zoneServiceWebClient.get();
        WebClient.RequestHeadersSpec<?> spec = req.uri(uriBuilder -> {
            var builder = uriBuilder.path("/api/v1/zones");
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
                .uri("/api/v1/zones")
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(requestBody))
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Object>() {})
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

    @Override
    public CompletableFuture<Object> health() {
        return zoneServiceWebClient.get()
                .uri("/health")
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Object>() {})
                .onErrorMap(ex -> new ServiceUnavailableException("Zone service unavailable: " + ex.getMessage(), ex))
                .toFuture();
    }
}
