package com.ds.parcel_service.infrastructure.kafka.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for User events from Kafka
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
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime eventTimestamp;
}
