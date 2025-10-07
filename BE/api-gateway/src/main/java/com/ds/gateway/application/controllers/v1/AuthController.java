package com.ds.gateway.application.controllers.v1;

import com.ds.gateway.common.entities.dto.auth.*;
import com.ds.gateway.common.entities.dto.common.BaseResponse;
import com.ds.gateway.common.entities.dto.user.UserDto;
import com.ds.gateway.common.interfaces.IKeycloakAuthService;
import com.ds.gateway.common.interfaces.IUserServiceClient;
import com.ds.gateway.annotations.PublicRoute;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.CompletableFuture;

/**
 * Auth controller for handling Keycloak authentication requests
 * Most routes require authentication, except for token validation endpoints
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    
    @Autowired
    private IKeycloakAuthService keycloakAuthService;
    
    @Autowired
    private IUserServiceClient userServiceClient;
    
    /**
     * Login user with username/password via Keycloak
     * Supports type parameter to select appropriate client
     * Types: BACKEND (admin/staff), FRONTEND (shipper/client)
     */
    @PublicRoute
    @PostMapping("/login")
    public CompletableFuture<ResponseEntity<BaseResponse<KeycloakTokenResponseDto>>> login(
            @Valid @RequestBody KeycloakLoginRequestDto request) {
        log.info("Login request for username: {} with type: {}", request.getUsername(), request.getType());
        
        return keycloakAuthService.login(request.getUsername(), request.getPassword(), request.getType())
            .thenApply(response -> ResponseEntity.ok(BaseResponse.success(response, "Login successful")))
            .exceptionally(ex -> {
                log.error("Login failed: {}", ex.getMessage());
                return ResponseEntity.badRequest().body(BaseResponse.error("Login failed: " + ex.getMessage()));
            });
    }
    
    /**
     * Default login using default realm and client configuration
     */
    @PublicRoute
    @PostMapping("/login/default")
    public CompletableFuture<ResponseEntity<BaseResponse<KeycloakTokenResponseDto>>> defaultLogin(
            @Valid @RequestBody KeycloakLoginRequestDto request) {
        log.info("Default login request for username: {}", request.getUsername());
        
        return keycloakAuthService.defaultLogin(request.getUsername(), request.getPassword())
            .thenApply(response -> ResponseEntity.ok(BaseResponse.success(response, "Default login successful")))
            .exceptionally(ex -> {
                log.error("Default login failed: {}", ex.getMessage());
                return ResponseEntity.badRequest().body(BaseResponse.error("Default login failed: " + ex.getMessage()));
            });
    }
    
    /**
     * Login with specific realm and client ID
     */
    @PublicRoute
    @PostMapping("/login/custom")
    public CompletableFuture<ResponseEntity<BaseResponse<KeycloakTokenResponseDto>>> customLogin(
            @Valid @RequestBody CustomLoginRequestDto request) {
        log.info("Custom login request for username: {} with realm: {} and client: {}", 
                request.getUsername(), request.getRealm(), request.getClientId());
        
        return keycloakAuthService.loginWithRealmAndClient(
                request.getUsername(), 
                request.getPassword(), 
                request.getRealm(), 
                request.getClientId())
            .thenApply(response -> ResponseEntity.ok(BaseResponse.success(response, "Custom login successful")))
            .exceptionally(ex -> {
                log.error("Custom login failed: {}", ex.getMessage());
                return ResponseEntity.badRequest().body(BaseResponse.error("Custom login failed: " + ex.getMessage()));
            });
    }
    
    /**
     * Validate JWT token and return user information
     * This endpoint is called by clients to validate tokens and get user info
     */
    @PublicRoute
    @PostMapping("/validate-token")
    public CompletableFuture<ResponseEntity<BaseResponse<KeycloakUserInfoDto>>> validateToken(
            @RequestHeader("Authorization") String authorization) {
        
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return CompletableFuture.completedFuture(
                ResponseEntity.badRequest().body(BaseResponse.error("Invalid authorization header"))
            );
        }
        
        String token = authorization.substring(7);
        log.info("Token validation request");
        
        return keycloakAuthService.validateTokenAndGetUserInfo(token)
            .thenApply(userInfo -> ResponseEntity.ok(BaseResponse.success(userInfo, "Token valid")))
            .exceptionally(ex -> {
                log.error("Token validation failed: {}", ex.getMessage());
                return ResponseEntity.badRequest().body(BaseResponse.error("Token validation failed: " + ex.getMessage()));
            });
    }
    
    /**
     * Refresh access token using refresh token
     */
    @PublicRoute
    @PostMapping("/refresh-token")
    public CompletableFuture<ResponseEntity<BaseResponse<KeycloakTokenResponseDto>>> refreshToken(
            @Valid @RequestBody RefreshTokenRequestDto request) {
        log.info("Refresh token request");
        
        return keycloakAuthService.refreshToken(request.getRefreshToken())
            .thenApply(response -> ResponseEntity.ok(BaseResponse.success(response, "Token refreshed")))
            .exceptionally(ex -> {
                log.error("Token refresh failed: {}", ex.getMessage());
                return ResponseEntity.badRequest().body(BaseResponse.error("Token refresh failed: " + ex.getMessage()));
            });
    }
    
    /**
     * Logout user by invalidating refresh token
     */
    @PublicRoute
    @PostMapping("/logout")
    public CompletableFuture<ResponseEntity<BaseResponse<Boolean>>> logout(
            @Valid @RequestBody RefreshTokenRequestDto request) {
        log.info("Logout request");
        
        return keycloakAuthService.logout(request.getRefreshToken())
            .thenApply(success -> ResponseEntity.ok(BaseResponse.success(success, "Logout successful")))
            .exceptionally(ex -> {
                log.error("Logout failed: {}", ex.getMessage());
                return ResponseEntity.badRequest().body(BaseResponse.error("Logout failed: " + ex.getMessage()));
            });
    }
    
    /**
     * Get current user info from JWT token
     * Requires valid JWT token in Authorization header
     */
    @GetMapping("/me")
    public ResponseEntity<BaseResponse<KeycloakUserInfoDto>> getCurrentUser(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt)) {
            log.error("No valid JWT token found in request");
            return ResponseEntity.badRequest().body(BaseResponse.error("Authentication required"));
        }
        
        Jwt jwt = (Jwt) authentication.getPrincipal();
        KeycloakUserInfoDto userInfo = keycloakAuthService.extractUserInfoFromJwt(jwt);
        
        log.info("Current user info requested: {}", userInfo.getPreferredUsername());
        return ResponseEntity.ok(BaseResponse.success(userInfo, "User info retrieved"));
    }
    
    /**
     * Sync user data from Keycloak to User Service after login
     * This endpoint is called after successful authentication to ensure user exists in DB
     */
    @PostMapping("/sync")
    public CompletableFuture<ResponseEntity<BaseResponse<UserDto>>> syncUser(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt)) {
            log.error("No valid JWT token found in request");
            return CompletableFuture.completedFuture(
                ResponseEntity.badRequest().body(BaseResponse.error("Authentication required"))
            );
        }
        
        Jwt jwt = (Jwt) authentication.getPrincipal();
        KeycloakUserInfoDto keycloakUserInfo = keycloakAuthService.extractUserInfoFromJwt(jwt);
        
        log.info("Syncing user from Keycloak: keycloakId={}, username={}", 
                keycloakUserInfo.getSub(), keycloakUserInfo.getPreferredUsername());
        
        return userServiceClient.syncUserByKeycloakId(
                keycloakUserInfo.getSub(), 
                keycloakUserInfo.getPreferredUsername(), 
                keycloakUserInfo.getEmail(), 
                keycloakUserInfo.getGivenName(), 
                keycloakUserInfo.getFamilyName())
            .thenApply(user -> {
                log.info("User synced successfully: {}", user);
                return ResponseEntity.ok(BaseResponse.success(user, "User synced successfully"));
            })
            .exceptionally(ex -> {
                log.error("Failed to sync user: {}", ex.getMessage(), ex);
                return ResponseEntity.badRequest().body(BaseResponse.error("Failed to sync user: " + ex.getMessage()));
            });
    }
}
