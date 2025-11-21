package com.ds.session.session_service.application.client.userclient;

import org.springframework.stereotype.Service;

import com.ds.session.session_service.app_context.models.DeliveryManSnapshot;
import com.ds.session.session_service.app_context.repositories.DeliveryManSnapshotRepository;
import com.ds.session.session_service.application.client.userclient.response.DeliveryManResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service to get delivery man information from snapshot table
 * Uses local snapshot instead of calling User Service API
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceClient {

    private final DeliveryManSnapshotRepository deliveryManSnapshotRepository;

    /**
     * Get delivery man information by user ID from snapshot table
     * @param userId The user ID
     * @return DeliveryManResponse or null if not found
     */
    public DeliveryManResponse getDeliveryManByUserId(String userId) {
        try {
            return deliveryManSnapshotRepository.findByUserId(userId)
                .map(this::mapToDeliveryManResponse)
                .orElse(null);
        } catch (Exception e) {
            log.error("Error fetching delivery man info from snapshot for userId: {}. Error: {}", userId, e.getMessage());
            return null;
        }
    }

    private DeliveryManResponse mapToDeliveryManResponse(DeliveryManSnapshot snapshot) {
        return DeliveryManResponse.builder()
            .userId(snapshot.getUserId())
            .username(snapshot.getUsername())
            .firstName(snapshot.getFirstName())
            .lastName(snapshot.getLastName())
            .email(snapshot.getEmail())
            .phone(snapshot.getPhone())
            .vehicleType(snapshot.getVehicleType())
            .capacityKg(snapshot.getCapacityKg())
            .build();
    }
}
