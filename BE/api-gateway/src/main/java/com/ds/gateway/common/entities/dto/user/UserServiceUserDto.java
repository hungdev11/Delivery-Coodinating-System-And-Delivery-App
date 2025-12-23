package com.ds.gateway.common.entities.dto.user;

import com.ds.gateway.common.entities.dto.deliveryman.DeliveryManDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * User DTO from User Service (for deserialization)
 * This is separate from API Gateway UserDto to avoid conflicts
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserServiceUserDto {
    private String id; // Keycloak ID
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String address;
    private String identityNumber;
    private String username;
    private String status; // UserStatus as string
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<String> roles;
    private DeliveryManDto deliveryMan; // Delivery man info from User Service
}
