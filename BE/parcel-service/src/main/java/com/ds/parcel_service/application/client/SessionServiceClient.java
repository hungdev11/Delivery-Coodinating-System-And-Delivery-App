package com.ds.parcel_service.application.client;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.core.ParameterizedTypeReference;

import com.ds.parcel_service.common.entities.dto.common.BaseResponse;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class SessionServiceClient {

    private final RestTemplate restTemplate;

    @Value("${services.session.base-url}")
    private String sessionServiceBaseUrl;

    public LatestAssignmentInfo getLatestAssignmentForParcel(UUID parcelId) {
        String uri = UriComponentsBuilder.fromHttpUrl(sessionServiceBaseUrl)
                .path("/api/v1/assignments/parcel/{parcelId}/latest-assignment")
                .buildAndExpand(parcelId.toString())
                .toUriString();

        log.info("Calling Session Service for latest assignment of parcel {}", parcelId);
        ParameterizedTypeReference<BaseResponse<LatestAssignmentInfo>> type =
                new ParameterizedTypeReference<>() {};
        ResponseEntity<BaseResponse<LatestAssignmentInfo>> response =
                restTemplate.exchange(uri, HttpMethod.GET, null, type);
        BaseResponse<LatestAssignmentInfo> body = response.getBody();
        if (body != null && body.getResult() != null) {
            return body.getResult();
        }
        throw new IllegalStateException("Cannot resolve latest assignment for parcel " + parcelId);
    }

    public void markAssignmentSuccess(UUID sessionId, UUID assignmentId, String note) {
        String uri = UriComponentsBuilder.fromHttpUrl(sessionServiceBaseUrl)
                .path("/api/v1/sessions/{sessionId}/assignments/{assignmentId}/status")
                .buildAndExpand(sessionId, assignmentId)
                .toUriString();

        UpdateAssignmentStatusPayload payload = new UpdateAssignmentStatusPayload();
        payload.setAssignmentStatus("SUCCESS");
        payload.setParcelEvent("CUSTOMER_RECEIVED");
        payload.setFailReason(note);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        log.info("Marking assignment {} in session {} as SUCCESS", assignmentId, sessionId);
        restTemplate.exchange(uri, HttpMethod.PUT, new HttpEntity<>(payload, headers), Void.class);
    }

    @Data
    public static class LatestAssignmentInfo {
        private UUID assignmentId;
        private UUID sessionId;
        private String status;
        private String deliveryManId;
    }

    @Data
    private static class UpdateAssignmentStatusPayload {
        private String assignmentStatus;
        private String parcelEvent;
        private String failReason;
    }
}

