package com.ds.user.infrastructure.kafka.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for User events published to Kafka
 * Used to notify other services when user data changes
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserEventDto {
    public enum EventType {
        USER_CREATED,
        USER_UPDATED,
        USER_DELETED,
        DELIVERY_MAN_CREATED,
        DELIVERY_MAN_UPDATED,
        DELIVERY_MAN_DELETED,
        USER_SERVICE_READY,
        SCAN_QR
    }

    private EventType eventType;
    private String userId;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String phone;
    private String address;
    private String identityNumber;
    private String status; // UserStatus as String
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime eventTimestamp;
    
    // DeliveryMan specific fields (only populated for DELIVERY_MAN events)
    private UUID deliveryManId;
    private String vehicleType;
    private Double capacityKg;
}
