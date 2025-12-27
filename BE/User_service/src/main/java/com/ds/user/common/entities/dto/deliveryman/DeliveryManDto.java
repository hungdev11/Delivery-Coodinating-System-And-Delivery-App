package com.ds.user.common.entities.dto.deliveryman;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryManDto {
    private UUID id; // DeliveryMan ID is still UUID
    private String userId; // Changed from UUID to String (User.id is now String)
    private String vehicleType;
    private Double capacityKg;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // User information
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String phone;
    private String status;
    
    // Session information (enriched from session-service)
    private Boolean hasActiveSession;
    private LocalDateTime lastSessionStartTime;
    
    // Zone information (primary zone from working zones - zone with order = 1)
    private String zoneId;
}
