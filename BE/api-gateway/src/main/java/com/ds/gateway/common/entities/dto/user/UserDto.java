package com.ds.gateway.common.entities.dto.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * User DTO for API Gateway
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {
    private String id;
    private String keycloakId;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String phone;
    private String address;
    private String identityNumber;
    private List<String> roles;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    /**
     * Delivery man information (only populated for shippers)
     */
    private DeliveryManInfo deliveryMan;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DeliveryManInfo {
        private String id;
        private String vehicleType; // BIKE or CAR
        private Double capacityKg;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }
}
