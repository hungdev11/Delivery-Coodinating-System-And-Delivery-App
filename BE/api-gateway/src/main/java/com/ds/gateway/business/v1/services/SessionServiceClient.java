package com.ds.gateway.business.v1.services;

import com.ds.gateway.common.interfaces.ISessionServiceClient;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class SessionServiceClient implements ISessionServiceClient {

    private final WebClient webClient;

    public SessionServiceClient(@Qualifier("sessionServiceWebClient") WebClient webClient) {
        this.webClient = webClient;
    }

    @Override
    public ResponseEntity<?> acceptParcelToSession(String deliveryManId, Object scanParcelRequest) {
        String uri = String.format("/api/v1/sessions/drivers/%s/accept-parcel", deliveryManId);
        log.info("WebClient: POST -> {}", uri);
        return callPost(uri, scanParcelRequest);
    }

    @Override
    public ResponseEntity<?> getSessionById(UUID sessionId) {
        String uri = String.format("/api/v1/sessions/%s", sessionId);
        log.info("WebClient: GET -> {}", uri);
        return callGet(uri);
    }

    @Override
    public ResponseEntity<?> completeSession(UUID sessionId) {
        String uri = String.format("/api/v1/sessions/%s/complete", sessionId);
        log.info("WebClient: POST -> {}", uri);
        return callPost(uri, null);
    }

    @Override
    public ResponseEntity<?> failSession(UUID sessionId, Object sessionFailRequest) {
        String uri = String.format("/api/v1/sessions/%s/fail", sessionId);
        log.warn("WebClient: POST -> {}", uri);
        return callPost(uri, sessionFailRequest);
    }

    @Override
    public ResponseEntity<?> createSessionBatch(Object createSessionRequest) {
        String uri = "/api/v1/sessions";
        log.info("WebClient: POST -> {}", uri);
        return callPost(uri, createSessionRequest);
    }


    @Override
    public ResponseEntity<?> getDailyTasks(UUID deliveryManId, List<String> status, int page, int size) {
        String uri = UriComponentsBuilder
                .fromPath("/api/v1/assignments/session/delivery-man/{id}/tasks/today")
                .queryParam("page", page)
                .queryParam("size", size)
                .queryParamIfPresent("status", status == null ? null : java.util.Optional.of(status))
                .build(deliveryManId)
                .toString();

        log.info("WebClient: GET -> {}", uri);
        return callGet(uri);
    }

    @Override
    public ResponseEntity<?> getTasksHistory(UUID deliveryManId, List<String> status,
                                             String createdAtStart, String createdAtEnd,
                                             String completedAtStart, String completedAtEnd,
                                             int page, int size) {
        UriComponentsBuilder builder = UriComponentsBuilder
                .fromPath("/api/v1/assignments/session/delivery-man/{id}/tasks")
                .queryParam("page", page)
                .queryParam("size", size);

        if (status != null && !status.isEmpty()) builder.queryParam("status", status.toArray());
        if (createdAtStart != null) builder.queryParam("createdAtStart", createdAtStart);
        if (createdAtEnd != null) builder.queryParam("createdAtEnd", createdAtEnd);
        if (completedAtStart != null) builder.queryParam("completedAtStart", completedAtStart);
        if (completedAtEnd != null) builder.queryParam("completedAtEnd", completedAtEnd);

        String uri = builder.build(deliveryManId).toString();
        log.info("WebClient: GET -> {}", uri);
        return callGet(uri);
    }

    @Override
    public ResponseEntity<?> completeTask(UUID deliveryManId, UUID parcelId, Object routeInfo) {
        String uri = String.format("/api/v1/assignments/drivers/%s/parcels/%s/complete", deliveryManId, parcelId);
        return callPost(uri, routeInfo);
    }

    @Override
    public ResponseEntity<?> failTask(UUID deliveryManId, UUID parcelId, Object taskFailRequest) {
        String uri = String.format("/api/v1/assignments/drivers/%s/parcels/%s/fail", deliveryManId, parcelId);
        return callPost(uri, taskFailRequest);
    }

    @Override
    public ResponseEntity<?> refuseTask(UUID deliveryManId, UUID parcelId) {
        String uri = String.format("/api/v1/assignments/drivers/%s/parcels/%s/refuse", deliveryManId, parcelId);
        return callPost(uri, null);
    }

    @Override
    public ResponseEntity<?> postponeTask(UUID deliveryManId, UUID parcelId, String addInfo) {
        String uri = String.format("/api/v1/assignments/drivers/%s/parcels/%s/postpone", deliveryManId, parcelId);
        return callPost(uri, addInfo);
    }

    @Override
    public ResponseEntity<?> lastestShipperForParcel(UUID parcelId) {
        String uri = String.format("/api/v1/assignments/current-shipper/parcels/%s", parcelId);
        return callGet(uri);
    }

    // --------------------------------------------------
    // Generic WebClient handlers
    // --------------------------------------------------

    private ResponseEntity<?> callGet(String uri) {
        try {
            Object body = webClient.get()
                    .uri(uri)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .bodyToMono(Object.class)
                    .block();

            return ResponseEntity.ok(body);

        } catch (WebClientResponseException e) {
            log.error("GET {} failed: {} {}", uri, e.getStatusCode(), e.getResponseBodyAsString());
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        }
    }

    private ResponseEntity<?> callPost(String uri, Object body) {
        try {
            Object responseBody = webClient.post()
                    .uri(uri)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .bodyValue(body != null ? body : new Object())
                    .retrieve()
                    .bodyToMono(Object.class)
                    .block();

            return ResponseEntity.ok(responseBody);

        } catch (WebClientResponseException e) {
            log.error("POST {} failed: {} {}", uri, e.getStatusCode(), e.getResponseBodyAsString());
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        }
    }
}
