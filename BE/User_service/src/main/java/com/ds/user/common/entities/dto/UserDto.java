package com.ds.user.common.entities.dto;

import com.ds.user.app_context.models.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for User entity
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDto {
    private UUID id;
    private String keycloakId;
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
    
    /**
     * Convert from User entity to DTO
     */
    public static UserDto from(User user) {
        if (user == null) return null;
        
        return UserDto.builder()
                .id(user.getId())
                .keycloakId(user.getKeycloakId())
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
                .build();
    }
}
