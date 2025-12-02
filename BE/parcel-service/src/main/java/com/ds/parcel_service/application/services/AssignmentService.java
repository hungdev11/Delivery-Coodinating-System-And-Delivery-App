package com.ds.parcel_service.application.services;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.ds.parcel_service.application.client.SessionServiceClient;
import com.ds.parcel_service.application.client.SessionServiceClient.LatestAssignmentInfo;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service to get assignment information from Session Service
 * Calls external service directly instead of storing locally
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AssignmentService {

    private final SessionServiceClient sessionServiceClient;

    /**
     * Get latest assignment info for a parcel from Session Service
     * @param parcelId The parcel ID
     * @return AssignmentInfo or null if not found
     */
    public AssignmentInfo getOrFetch(UUID parcelId) {
        try {
            LatestAssignmentInfo info = sessionServiceClient.getLatestAssignmentForParcel(parcelId);
            if (info == null) {
                return null;
            }
            return AssignmentInfo.builder()
                .assignmentId(info.getAssignmentId())
                .parcelId(parcelId)
                .sessionId(info.getSessionId())
                .deliveryManId(info.getDeliveryManId())
                .status(info.getStatus())
                .build();
        } catch (Exception e) {
            log.debug("[parcel-service] [AssignmentService.getOrFetch] Could not get assignment info for parcel {}: {}", parcelId, e.getMessage());
            return null;
        }
    }

    /**
     * Refresh assignment info from Session Service
     * @param parcelId The parcel ID
     * @return AssignmentInfo or null if not found
     */
    public AssignmentInfo refreshFromRemote(UUID parcelId) {
        return getOrFetch(parcelId);
    }

    /**
     * DTO for assignment information
     */
    @Data
    @lombok.Builder
    public static class AssignmentInfo {
        private UUID assignmentId;
        private UUID parcelId;
        private UUID sessionId;
        private String deliveryManId;
        private String status;
    }
}
