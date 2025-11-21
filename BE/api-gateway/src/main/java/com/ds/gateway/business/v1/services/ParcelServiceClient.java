package com.ds.gateway.business.v1.services;

import com.ds.gateway.common.interfaces.IParcelServiceClient;
import com.ds.gateway.common.utils.ProxyHeaderUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Slf4j
@Component
public class ParcelServiceClient implements IParcelServiceClient {

    private final WebClient parcelServiceWebClient;

    public ParcelServiceClient(@Qualifier("parcelServiceWebClient") WebClient webClient) {
        this.parcelServiceWebClient = webClient;
    }

    @Override
    public ResponseEntity<?> createParcel(Object request) {
        Object response = parcelServiceWebClient.post()
                .uri("/api/v1/parcels")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(Object.class)
                .block();
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<?> updateParcel(UUID parcelId, Object request) {
        parcelServiceWebClient.put()
                .uri("/api/v1/parcels/{id}", parcelId)
                .bodyValue(request)
                .retrieve()
                .toBodilessEntity()
                .block();
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<?> getParcelById(UUID parcelId) {
        Object response = parcelServiceWebClient.get()
                .uri("/api/v1/parcels/{id}", parcelId)
                .retrieve()
                .bodyToMono(Object.class)
                .block();
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<?> getParcelByCode(String code) {
        Object response = parcelServiceWebClient.get()
                .uri("/api/v1/parcels/code/{code}", code)
                .retrieve()
                .bodyToMono(Object.class)
                .block();
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<?> getParcelsSent(String customerId, int page, int size) {
        String finalUrl = UriComponentsBuilder.fromUriString("/api/v1/parcels/me")
                .queryParam("customerId", customerId)
                .queryParam("page", page)
                .queryParam("size", size)
                .toUriString();

        Object response = executeGet(finalUrl);
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<?> getParcelsReceived(String customerId, int page, int size) {
        String finalUrl = UriComponentsBuilder.fromUriString("/api/v1/parcels/me/receive")
                .queryParam("customerId", customerId)
                .queryParam("page", page)
                .queryParam("size", size)
                .toUriString();

        Object response = executeGet(finalUrl);
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<?> getParcelsV2(Object request) {
        try {
            Object response = parcelServiceWebClient.post()
                    .uri("/api/v2/parcels")
                    .headers(httpHeaders -> httpHeaders.addAll(ProxyHeaderUtils.createHeadersWithUserId()))
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(Object.class)
                    .block();
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error during getParcelsV2 request: {}", e.getMessage());
            return ResponseEntity.status(500).body(null);
        }
    }

    @Override
    public ResponseEntity<?> getClientReceivedParcels(Object request) {
        try {
            Object response = parcelServiceWebClient.post()
                    .uri("/api/v1/client/parcels/received")
                    .headers(httpHeaders -> httpHeaders.addAll(ProxyHeaderUtils.createHeadersWithUserId()))
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(Object.class)
                    .block();
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error during getClientReceivedParcels request: {}", e.getMessage());
            return ResponseEntity.status(500).body(null);
        }
    }

    @Override
    public ResponseEntity<?> changeParcelStatus(UUID parcelId, String event) {
        try {
            String uri = UriComponentsBuilder.fromUriString("/api/v1/parcels/change-status/{parcelId}")
                    .queryParam("event", event)
                    .buildAndExpand(parcelId)
                    .toUriString();
            
            Object response = parcelServiceWebClient.put()
                    .uri(uri)
                    .retrieve()
                    .bodyToMono(Object.class)
                    .block();
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error during changeParcelStatus request: {}", e.getMessage());
            return ResponseEntity.status(500).body(null);
        }
    }

    @Override
    public ResponseEntity<?> confirmParcel(UUID parcelId, Object request) {
        try {
            Object response = parcelServiceWebClient.post()
                    .uri("/api/v1/client/parcels/{parcelId}/confirm", parcelId)
                    .headers(headers -> headers.addAll(ProxyHeaderUtils.createHeadersWithUserId()))
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(Object.class)
                    .block();
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error during confirmParcel request: {}", e.getMessage());
            return ResponseEntity.status(500).body(null);
        }
    }

    @Override
    public ResponseEntity<?> deleteParcel(UUID parcelId) {
        try {
            parcelServiceWebClient.delete()
                    .uri("/api/v1/parcels/{id}", parcelId)
                    .retrieve()
                    .toBodilessEntity()
                    .block();
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("Error during deleteParcel request: {}", e.getMessage());
            return ResponseEntity.status(500).body(null);
        }
    }

    private Object executeGet(String url) {
        try {
            return parcelServiceWebClient.get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(Object.class)
                    .onErrorResume(e -> {
                        log.error("Error during request to {}: {}", url, e.getMessage());
                        return Mono.empty();
                    })
                    .block();
        } catch (Exception e) {
            log.error("Unexpected error: {}", e.getMessage());
            return null;
        }
    }
}
