package com.ds.user.common.entities.dto.user;

import com.ds.user.common.entities.base.User;
import com.ds.user.common.entities.dto.deliveryman.DeliveryManDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

/**
 * DTO for User entity
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDto {
    private String id; // This is now the Keycloak ID
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String address;
    private String identityNumber;
    private String username;
    private User.UserStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<String> roles;
    private DeliveryManDto deliveryMan; // Delivery man info (if user is a shipper)
    
    /**
     * Convert from User entity to DTO
     */
    public static UserDto from(User user) {
        return from(user, null, null);
    }

    public static UserDto from(User user, List<String> roles) {
        return from(user, roles, null);
    }

    public static UserDto from(User user, List<String> roles, com.ds.user.common.entities.dto.deliveryman.DeliveryManDto deliveryMan) {
        if (user == null) return null;
        
        return UserDto.builder()
                .id(user.getId()) // ID is now String (Keycloak ID)
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .address(user.getAddress())
                .identityNumber(user.getIdentityNumber())
                .username(user.getUsername())
                .status(user.getStatus())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .roles(roles != null ? List.copyOf(roles) : Collections.emptyList())
                .deliveryMan(deliveryMan)
                .build();
    }
}
