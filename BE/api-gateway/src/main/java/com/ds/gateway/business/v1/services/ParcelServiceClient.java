package com.ds.gateway.business.v1.services;

import com.ds.gateway.common.interfaces.IParcelServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

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


