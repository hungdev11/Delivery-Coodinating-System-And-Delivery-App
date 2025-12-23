package com.ds.deliveryapp.clients.res;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
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
    private String createdAt;
    private String updatedAt;
    
    /**
     * Delivery man information (only populated for shippers)
     */
    private DeliveryManInfo deliveryMan;
    
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DeliveryManInfo {
        private String id;
        private String vehicleType; // BIKE or CAR
        private Double capacityKg;
        private String createdAt;
        private String updatedAt;
    }
}
