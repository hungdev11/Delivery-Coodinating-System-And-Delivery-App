package com.ds.communication_service.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for User information from User Service
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserInfoDto {
    private String id;
    private String firstName;
    private String lastName;
    private String username;
    private String email;
    
    /**
     * Get full name (firstName + lastName)
     */
    public String getFullName() {
        if (firstName != null && lastName != null) {
            return firstName + " " + lastName;
        } else if (firstName != null) {
            return firstName;
        } else if (lastName != null) {
            return lastName;
        } else if (username != null) {
            return username;
        }
        return "User " + (id != null ? id.substring(0, Math.min(4, id.length())) : "Unknown");
    }
}
