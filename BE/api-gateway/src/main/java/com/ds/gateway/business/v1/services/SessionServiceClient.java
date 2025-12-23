package com.ds.gateway.business.v1.services;

import java.util.List;
import java.util.UUID;

import java.time.Duration;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import com.ds.gateway.common.interfaces.ISessionServiceClient;
import lombok.extern.slf4j.Slf4j;

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
        log.debug("[api-gateway] [SessionServiceClient.acceptParcelToSession] WebClient: POST -> {}", uri);
        return callPost(uri, scanParcelRequest);
    }

    @Override
    public ResponseEntity<?> getSessionById(UUID sessionId) {
        String uri = String.format("/api/v1/sessions/%s", sessionId);
        log.debug("[api-gateway] [SessionServiceClient.getSessionById] WebClient: GET -> {}", uri);
        return callGet(uri);
    }

    @Override
    public ResponseEntity<?> completeSession(UUID sessionId) {
        String uri = String.format("/api/v1/sessions/%s/complete", sessionId);
        log.debug("[api-gateway] [SessionServiceClient.completeSession] WebClient: POST -> {}", uri);
        return callPost(uri, null);
    }

    @Override
    public ResponseEntity<?> failSession(UUID sessionId, Object sessionFailRequest) {
        String uri = String.format("/api/v1/sessions/%s/fail", sessionId);
        log.debug("[api-gateway] [SessionServiceClient.failSession] WebClient: POST -> {}", uri);
        return callPost(uri, sessionFailRequest);
    }

    @Override
    public ResponseEntity<?> createSessionBatch(Object createSessionRequest) {
        String uri = "/api/v1/sessions";
        log.debug("[api-gateway] [SessionServiceClient.createSessionBatch] WebClient: POST -> {}", uri);
        return callPost(uri, createSessionRequest);
    }

    @Override
    public ResponseEntity<?> getDailyTasks(UUID deliveryManId, List<String> status, int page, int size) {
        String uri = UriComponentsBuilder
                .fromPath("/api/v1/assignments/session/delivery-man/{id}/tasks/today")
                .queryParam("page", page)
                .queryParam("size", size)
                .queryParamIfPresent("status", Optional.ofNullable(status))
                .build(deliveryManId)
                .toString();

        // String url =
        // String.format("%s/api/v1/assignments/session/delivery-man/%s/tasks/today",
        // sessionServiceUrl, deliveryManId);

        // // Xây dựng URL với các query param
        // UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url)
        // .queryParam("page", page)
        // .queryParam("size", size);

        // if (status != null && !status.isEmpty()) {
        // // Gửi 'status' lặp lại (ví dụ: ?status=PENDING&status=PROCESSING)
        // builder.queryParam("status", status.toArray());
        // }

        // String fullUrl = builder.toUriString();
        // log.info("Gateway: Proxying 'getDailyTasks' to {}", fullUrl);

        // // Fully deserialize response to avoid chunked encoding issues with
        // Cloudflare
        // ResponseEntity<Object> response = restTemplate.exchange(fullUrl,
        // HttpMethod.GET, null, Object.class);

        // // Remove Transfer-Encoding header to prevent duplicate headers
        // // Spring Boot will calculate Content-Length from the body
        // org.springframework.http.HttpHeaders headers = new
        // org.springframework.http.HttpHeaders();
        // headers.putAll(response.getHeaders());
        // headers.remove("Transfer-Encoding"); // Remove to prevent duplicate headers

        // // Create new ResponseEntity with cleaned headers
        // return ResponseEntity.status(response.getStatusCode())
        // .headers(headers)
        // .body(response.getBody());

        log.debug("[api-gateway] [SessionServiceClient.getDailyTasks] WebClient: GET -> {}", uri);
        return callGet(uri);
    }

    @Override
    public ResponseEntity<?> getTasksBySessionId(UUID sessionId, int page, int size) {
        String uri = UriComponentsBuilder
                .fromPath("/api/v1/assignments/session/{sessionId}/tasks")
                .queryParam("page", page)
                .queryParam("size", size)
                .build(sessionId)
                .toString();

        log.debug("[api-gateway] [SessionServiceClient.getTasksBySessionId] WebClient: GET -> {}", uri);
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

        if (status != null && !status.isEmpty())
            builder.queryParam("status", status.toArray());
        if (createdAtStart != null)
            builder.queryParam("createdAtStart", createdAtStart);
        if (createdAtEnd != null)
            builder.queryParam("createdAtEnd", createdAtEnd);
        if (completedAtStart != null)
            builder.queryParam("completedAtStart", completedAtStart);
        if (completedAtEnd != null)
            builder.queryParam("completedAtEnd", completedAtEnd);

        String uri = builder.buildAndExpand(deliveryManId).toUriString();

        log.debug("[api-gateway] [SessionServiceClient.getTasksHistory] WebClient: GET -> {}", uri);
        return callGet(uri);
    }

    @Override
    public ResponseEntity<?> completeTask(UUID deliveryManId, UUID parcelId, Object routeInfo) {
        String uri = String.format("/api/v1/assignments/drivers/%s/parcels/%s/complete", deliveryManId, parcelId);
        return callPost(uri, routeInfo);
    }

    @Override
    public ResponseEntity<?> completeTaskByAssignmentId(UUID assignmentId, Object request) {
        String uri = String.format("/api/v1/assignments/%s/complete", assignmentId);
        log.debug("[api-gateway] [SessionServiceClient.completeTaskByAssignmentId] POST -> {}", uri);
        return callPost(uri, request);
    }

    @Override
    public ResponseEntity<?> returnToWarehouse(UUID assignmentId, Object request) {
        String uri = String.format("/api/v1/assignments/%s/return-to-warehouse", assignmentId);
        log.debug("[api-gateway] [SessionServiceClient.returnToWarehouse] POST -> {}", uri);
        return callPost(uri, request);
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

    @Override
    public ResponseEntity<?> updateAssignmentStatus(UUID sessionId, UUID assignmentId, Object statusUpdateRequest) {
        String uri = String.format("/api/v1/sessions/%s/assignments/%s/status", sessionId, assignmentId);
        log.debug("[api-gateway] [SessionServiceClient.updateAssignmentStatus] WebClient: PUT -> {}", uri);
        return callPut(uri, statusUpdateRequest);
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
                    .block(Duration.ofSeconds(30)); // 30 second timeout for GET requests

            return ResponseEntity.ok(body);

        } catch (WebClientResponseException e) {
            log.error("[api-gateway] [SessionServiceClient.callGet] GET {} failed: {} {}", uri, e.getStatusCode(), e.getResponseBodyAsString());
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        } catch (WebClientException e) {
            log.error("[api-gateway] [SessionServiceClient.callGet] GET {} request failed (timeout/connection error)", uri, e);
            return ResponseEntity.status(504).body("{\"error\":\"Gateway timeout: Service did not respond in time\"}");
        } catch (Exception e) {
            log.error("[api-gateway] [SessionServiceClient.callGet] GET {} unexpected error", uri, e);
            return ResponseEntity.status(500).body("{\"error\":\"Internal server error: " + e.getMessage() + "\"}");
        }
    }

    private ResponseEntity<?> callPost(String uri, Object body) {
        try {
            WebClient.RequestBodySpec requestSpec = webClient.post()
                    .uri(uri)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
            
            // Set body - use empty JSON object if body is null
            if (body != null) {
                requestSpec.bodyValue(body);
            } else {
                // For endpoints that don't require body, send empty JSON object
                requestSpec.bodyValue("{}");
            }
            
            Object responseBody = requestSpec
                    .retrieve()
                    .bodyToMono(Object.class)
                    .block(Duration.ofSeconds(60)); // 60 second timeout

            return ResponseEntity.ok(responseBody);

        } catch (WebClientResponseException e) {
            log.error("[api-gateway] [SessionServiceClient.callPost] POST {} failed: {} {}", uri, e.getStatusCode(), e.getResponseBodyAsString());
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        } catch (WebClientException e) {
            log.error("[api-gateway] [SessionServiceClient.callPost] POST {} request failed (timeout/connection error)", uri, e);
            return ResponseEntity.status(504).body("{\"error\":\"Gateway timeout: Service did not respond in time\"}");
        } catch (Exception e) {
            log.error("[api-gateway] [SessionServiceClient.callPost] POST {} unexpected error", uri, e);
            return ResponseEntity.status(500).body("{\"error\":\"Internal server error: " + e.getMessage() + "\"}");
        }
    }

    private ResponseEntity<?> callPut(String uri, Object body) {
        try {
            Object responseBody = webClient.put()
                    .uri(uri)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .bodyValue(body != null ? body : new Object())
                    .retrieve()
                    .bodyToMono(Object.class)
                    .block(Duration.ofSeconds(30)); // 30 second timeout for PUT requests

            return ResponseEntity.ok(responseBody);

        } catch (WebClientResponseException e) {
            log.error("[api-gateway] [SessionServiceClient.callPut] PUT {} failed: {} {}", uri, e.getStatusCode(), e.getResponseBodyAsString());
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        } catch (WebClientException e) {
            log.error("[api-gateway] [SessionServiceClient.callPut] PUT {} request failed (timeout/connection error)", uri, e);
            return ResponseEntity.status(504).body("{\"error\":\"Gateway timeout: Service did not respond in time\"}");
        } catch (Exception e) {
            log.error("[api-gateway] [SessionServiceClient.callPut] PUT {} unexpected error", uri, e);
            return ResponseEntity.status(500).body("{\"error\":\"Internal server error: " + e.getMessage() + "\"}");
        }
    }

    @Override
    public ResponseEntity<?> generateQR(String data) {
        String uri = UriComponentsBuilder
                .fromPath("/api/v1/qr/generate")
                .queryParam("data", data)
                .build()
                .toUriString();
        log.debug("[api-gateway] [SessionServiceClient.generateQR] WebClient: GET -> {}", uri);
        try {
            byte[] qrImage = webClient.get()
                    .uri(uri)
                    .accept(MediaType.IMAGE_PNG)
                    .retrieve()
                    .bodyToMono(byte[].class)
                    .block();
            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_PNG)
                    .body(qrImage);
        } catch (WebClientResponseException e) {
            log.error("[api-gateway] [SessionServiceClient.generateQR] GET {} failed: {} {}", uri, e.getStatusCode(), e.getResponseBodyAsString());
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        }
    }

    @Override
    public ResponseEntity<?> createSessionPrepared(String deliveryManId) {
        String uri = String.format("/api/v1/sessions/drivers/%s/prepare", deliveryManId);
        log.debug("[api-gateway] [SessionServiceClient.createSessionPrepared] WebClient: POST -> {}", uri);
        return callPost(uri, null);
    }

    @Override
    public ResponseEntity<?> startSession(UUID sessionId, Object startSessionRequest) {
        String uri = String.format("/api/v1/sessions/%s/start", sessionId);
        log.debug("[api-gateway] [SessionServiceClient.startSession] WebClient: POST -> {}", uri);
        return callPost(uri, startSessionRequest);
    }

    @Override
    public ResponseEntity<?> sendLocationUpdate(String sessionId, Object locationUpdateRequest) {
        String uri = String.format("/api/v1/sessions/%s/tracking", sessionId);
        log.debug("[api-gateway] [SessionServiceClient.sendLocationUpdate] WebClient: POST -> {}", uri);
        return callPost(uri, locationUpdateRequest);
    }

    @Override
    public ResponseEntity<?> getActiveSession(String deliveryManId) {
        String uri = String.format("/api/v1/sessions/drivers/%s/active", deliveryManId);
        log.debug("[api-gateway] [SessionServiceClient.getActiveSession] WebClient: GET -> {}", uri);
        try {
            Object body = webClient.get()
                    .uri(uri)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .bodyToMono(Object.class)
                    .block();
            
            // Handle 204 No Content response
            if (body == null) {
                return ResponseEntity.noContent().build();
            }
            
            return ResponseEntity.ok(body);
        } catch (WebClientResponseException e) {
            // Handle 204 No Content as success
            if (e.getStatusCode().value() == 204) {
                return ResponseEntity.noContent().build();
            }
            log.error("[api-gateway] [SessionServiceClient.getActiveSession] GET {} failed: {} {}", uri, e.getStatusCode(), e.getResponseBodyAsString());
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        }
    }

    @Override
    public ResponseEntity<?> getAllSessionsForDeliveryMan(String deliveryManId, String excludeParcelId) {
        String uri = UriComponentsBuilder
                .fromPath("/api/v1/sessions/drivers/{id}/sessions")
                .queryParamIfPresent("excludeParcelId", Optional.ofNullable(excludeParcelId))
                .build(deliveryManId)
                .toString();
        log.debug("[api-gateway] [SessionServiceClient.getAllSessionsForDeliveryMan] WebClient: GET -> {}", uri);
        return callGet(uri);
    }

    @Override
    public ResponseEntity<?> getProofsByAssignment(UUID assignmentId) {
        String uri = String.format("/api/v1/delivery-proofs/assignments/%s", assignmentId);
        log.debug("[api-gateway] [SessionServiceClient.getProofsByAssignment] WebClient: GET -> {}", uri);
        return callGet(uri);
    }

    @Override
    public ResponseEntity<?> getProofsByParcel(String parcelId) {
        String uri = String.format("/api/v1/delivery-proofs/parcels/%s", parcelId);
        log.debug("[api-gateway] [SessionServiceClient.getProofsByParcel] WebClient: GET -> {}", uri);
        return callGet(uri);
    }

    @Override
    public ResponseEntity<?> getLatestAssignmentForParcel(String parcelId) {
        String uri = String.format("/api/v1/assignments/parcel/%s/latest-assignment", parcelId);
        log.debug("[api-gateway] [SessionServiceClient.getLatestAssignmentForParcel] WebClient: GET -> {}", uri);
        return callGet(uri);
    }

    @Override
    public ResponseEntity<?> getActualRouteForSession(UUID sessionId) {
        String uri = String.format("/api/v1/sessions/%s/actual-route", sessionId);
        log.debug("[api-gateway] [SessionServiceClient.getActualRouteForSession] WebClient: GET -> {}", uri);
        return callGet(uri);
    }
}
