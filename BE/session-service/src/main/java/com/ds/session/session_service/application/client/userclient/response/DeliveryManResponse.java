package com.ds.session.session_service.application.client.userclient.response;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryManResponse {
    private UUID id;
    private String userId;
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
}
