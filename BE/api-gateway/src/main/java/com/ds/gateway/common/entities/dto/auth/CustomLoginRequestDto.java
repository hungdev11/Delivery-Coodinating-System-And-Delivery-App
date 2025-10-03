package com.ds.gateway.common.entities.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Custom login request DTO for realm and client ID specific login
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomLoginRequestDto {
    
    @NotBlank(message = "Username is required")
    private String username;
    
    @NotBlank(message = "Password is required")
    private String password;
    
    @NotBlank(message = "Realm is required")
    private String realm;
    
    @NotBlank(message = "Client ID is required")
    private String clientId;
}
