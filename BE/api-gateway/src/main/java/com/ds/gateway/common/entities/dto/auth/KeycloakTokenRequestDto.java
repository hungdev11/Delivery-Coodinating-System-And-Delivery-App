package com.ds.gateway.common.entities.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Keycloak token request DTO for login
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KeycloakTokenRequestDto {
    @NotBlank(message = "Username is required")
    private String username;
    
    @NotBlank(message = "Password is required")
    private String password;
    
    @NotBlank(message = "Grant type is required")
    private String grantType = "password";
    
    @NotBlank(message = "Client ID is required")
    private String clientId;
    
    private String clientSecret;
}
