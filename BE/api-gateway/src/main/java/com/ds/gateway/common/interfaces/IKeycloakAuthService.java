package com.ds.gateway.common.interfaces;

import com.ds.gateway.common.entities.dto.auth.*;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.concurrent.CompletableFuture;

/**
 * Interface for Keycloak authentication service
 * Handles token validation, user info extraction, and token refresh
 */
public interface IKeycloakAuthService {
    
    /**
     * Login user with username/password and return tokens + user info
     */
    CompletableFuture<LoginResponseDto> login(String username, String password, String type);
    
    /**
     * Login with specific realm and client ID and return tokens + user info
     */
    CompletableFuture<LoginResponseDto> loginWithRealmAndClient(String username, String password, String realm, String clientId);
    
    /**
     * Default login using default realm and client configuration and return tokens + user info
     */
    CompletableFuture<LoginResponseDto> defaultLogin(String username, String password);
    
    /**
     * Validate JWT token and extract user information
     */
    CompletableFuture<KeycloakUserInfoDto> validateTokenAndGetUserInfo(String accessToken);
    
    /**
     * Refresh access token using refresh token
     */
    CompletableFuture<KeycloakTokenResponseDto> refreshToken(String refreshToken);
    
    /**
     * Extract user info from JWT token (already validated by Spring Security)
     */
    KeycloakUserInfoDto extractUserInfoFromJwt(Jwt jwt);
    
    /**
     * Validate if token is still valid (not expired)
     */
    CompletableFuture<Boolean> isTokenValid(String accessToken);
    
    /**
     * Logout user by invalidating refresh token
     */
    CompletableFuture<Boolean> logout(String refreshToken);
}
