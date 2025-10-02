package com.ds.gateway.common.entities.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Keycloak login request DTO with type-based realm/client selection
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KeycloakLoginRequestDto {
    @NotBlank(message = "Username is required")
    private String username;
    
    @NotBlank(message = "Password is required")
    private String password;
    
    /**
     * Login type to determine client
     * Options: BACKEND (admin/staff), FRONTEND (shipper/client)
     * Defaults to FRONTEND if not specified
     */
    private String type;
    
    @Builder.Default
    private String grantType = "password";
    
    private String clientSecret; // Optional, will use configured secret if not provided
}
