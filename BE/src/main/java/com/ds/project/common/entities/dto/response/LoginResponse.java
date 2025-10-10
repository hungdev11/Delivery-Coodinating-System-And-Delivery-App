package com.ds.project.common.entities.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Login response DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {
    
    private String token;
    private long expiresIn;
    private UserResponse user;
    private List<String> roles;
    
    @Builder.Default
    private String tokenType = "Bearer";
}
