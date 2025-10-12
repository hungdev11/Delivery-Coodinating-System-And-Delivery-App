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
     * Returns both tokens and user information from User Service
     */
    @PublicRoute
    @PostMapping("/login")
    public CompletableFuture<ResponseEntity<BaseResponse<LoginResponseDto>>> login(
            @Valid @RequestBody KeycloakLoginRequestDto request) {
        log.info("🔐 LOGIN REQUEST - Username: {}, Type: {}", request.getUsername(), request.getType());
        
        return keycloakAuthService.login(request.getUsername(), request.getPassword(), request.getType())
            .thenApply(loginResponse -> {
                log.info("✅ LOGIN SUCCESS - Username: {}, UserId: {}, TokenType: {}, ExpiresIn: {}s", 
                    request.getUsername(), 
                    loginResponse.getUser() != null ? loginResponse.getUser().getId() : "N/A",
                    loginResponse.getTokenType(),
                    loginResponse.getExpiresIn());
                return ResponseEntity.ok(BaseResponse.success(loginResponse, "Login successful"));
            })
            .exceptionally(ex -> {
                log.error("❌ LOGIN FAILED - Username: {}, Error: {}", request.getUsername(), ex.getMessage());
                return createLoginErrorResponse(ex);
            });
    }
    
    /**
     * Default login using default realm and client configuration
     * Returns both tokens and user information from User Service
     */
    @PublicRoute
    @PostMapping("/login/default")
    public CompletableFuture<ResponseEntity<BaseResponse<LoginResponseDto>>> defaultLogin(
            @Valid @RequestBody KeycloakLoginRequestDto request) {
        log.info("🔐 DEFAULT LOGIN REQUEST - Username: {}", request.getUsername());
        
        return keycloakAuthService.defaultLogin(request.getUsername(), request.getPassword())
            .thenApply(loginResponse -> {
                log.info("✅ DEFAULT LOGIN SUCCESS - Username: {}, UserId: {}, TokenType: {}, ExpiresIn: {}s", 
                    request.getUsername(), 
                    loginResponse.getUser() != null ? loginResponse.getUser().getId() : "N/A",
                    loginResponse.getTokenType(),
                    loginResponse.getExpiresIn());
                return ResponseEntity.ok(BaseResponse.success(loginResponse, "Login successful"));
            })
            .exceptionally(ex -> {
                log.error("❌ DEFAULT LOGIN FAILED - Username: {}, Error: {}", request.getUsername(), ex.getMessage());
                return createLoginErrorResponse(ex);
            });
    }
    
    /**
     * Login with specific realm and client ID
     * Returns both tokens and user information from User Service
     */
    @PublicRoute
    @PostMapping("/login/custom")
    public CompletableFuture<ResponseEntity<BaseResponse<LoginResponseDto>>> customLogin(
            @Valid @RequestBody CustomLoginRequestDto request) {
        log.info("🔐 CUSTOM LOGIN REQUEST - Username: {}, Realm: {}, Client: {}", 
                request.getUsername(), request.getRealm(), request.getClientId());
        
        return keycloakAuthService.loginWithRealmAndClient(
                request.getUsername(), 
                request.getPassword(), 
                request.getRealm(), 
                request.getClientId())
            .thenApply(loginResponse -> {
                log.info("✅ CUSTOM LOGIN SUCCESS - Username: {}, Realm: {}, UserId: {}, TokenType: {}, ExpiresIn: {}s", 
                    request.getUsername(), 
                    request.getRealm(),
                    loginResponse.getUser() != null ? loginResponse.getUser().getId() : "N/A",
                    loginResponse.getTokenType(),
                    loginResponse.getExpiresIn());
                return ResponseEntity.ok(BaseResponse.success(loginResponse, "Login successful"));
            })
            .exceptionally(ex -> {
                log.error("❌ CUSTOM LOGIN FAILED - Username: {}, Realm: {}, Error: {}", 
                    request.getUsername(), request.getRealm(), ex.getMessage());
                return createLoginErrorResponse(ex);
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
        
        if (!isValidAuthorizationHeader(authorization)) {
            log.warn("🔍 TOKEN VALIDATION REQUEST - Invalid authorization header format");
            return CompletableFuture.completedFuture(
                ResponseEntity.badRequest().body(BaseResponse.error("Invalid authorization header format"))
            );
        }
        
        String token = extractTokenFromHeader(authorization);
        log.info("🔍 TOKEN VALIDATION REQUEST - Token length: {} chars", token.length());
        
        return keycloakAuthService.validateTokenAndGetUserInfo(token)
            .thenApply(userInfo -> {
                log.info("✅ TOKEN VALIDATION SUCCESS - Username: {}, Sub: {}", 
                    userInfo.getPreferredUsername(), userInfo.getSub());
                return ResponseEntity.ok(BaseResponse.success(userInfo, "Token valid"));
            })
            .exceptionally(ex -> {
                log.error("❌ TOKEN VALIDATION FAILED - Error: {}", ex.getMessage());
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
        if (!isValidJwtAuthentication(authentication)) {
            log.warn("👤 GET CURRENT USER REQUEST - No valid JWT token found");
            return ResponseEntity.badRequest().body(BaseResponse.error("Authentication required"));
        }
        
        Jwt jwt = (Jwt) authentication.getPrincipal();
        KeycloakUserInfoDto userInfo = keycloakAuthService.extractUserInfoFromJwt(jwt);
        
        log.info("✅ GET CURRENT USER SUCCESS - Username: {}, Sub: {}", 
            userInfo.getPreferredUsername(), userInfo.getSub());
        return ResponseEntity.ok(BaseResponse.success(userInfo, "User info retrieved"));
    }
    
    /**
     * Sync user data from Keycloak to User Service after login
     * This endpoint is called after successful authentication to ensure user exists in DB
     */
    @PostMapping("/sync")
    public CompletableFuture<ResponseEntity<BaseResponse<UserDto>>> syncUser(Authentication authentication) {
        if (!isValidJwtAuthentication(authentication)) {
            log.warn("🔄 SYNC USER REQUEST - No valid JWT token found");
            return CompletableFuture.completedFuture(
                ResponseEntity.badRequest().body(BaseResponse.error("Authentication required"))
            );
        }
        
        Jwt jwt = (Jwt) authentication.getPrincipal();
        KeycloakUserInfoDto keycloakUserInfo = keycloakAuthService.extractUserInfoFromJwt(jwt);
        
        log.info("🔄 SYNC USER REQUEST - KeycloakId: {}, Username: {}", 
                keycloakUserInfo.getSub(), keycloakUserInfo.getPreferredUsername());
        
        return userServiceClient.syncUserByKeycloakId(
                keycloakUserInfo.getSub(), 
                keycloakUserInfo.getPreferredUsername(), 
                keycloakUserInfo.getEmail(), 
                keycloakUserInfo.getGivenName(), 
                keycloakUserInfo.getFamilyName())
            .thenApply(user -> {
                log.info("✅ SYNC USER SUCCESS - Username: {}, UserId: {}, Email: {}", 
                    user.getUsername(), user.getId(), user.getEmail());
                return ResponseEntity.ok(BaseResponse.success(user, "User synced successfully"));
            })
            .exceptionally(ex -> {
                log.error("❌ SYNC USER FAILED - KeycloakId: {}, Error: {}", 
                    keycloakUserInfo.getSub(), ex.getMessage(), ex);
                return ResponseEntity.badRequest().body(BaseResponse.error("Failed to sync user: " + ex.getMessage()));
            });
    }
    
    // ==================== HELPER METHODS ====================
    
    /**
     * Create error response for login operations with clear, user-friendly messages
     */
    private ResponseEntity<BaseResponse<LoginResponseDto>> createLoginErrorResponse(Throwable ex) {
        String errorMessage = parseLoginErrorMessage(ex);
        log.error("Login failed: {}", errorMessage);
        return ResponseEntity.badRequest().body(BaseResponse.error(errorMessage));
    }
    
    /**
     * Parse Keycloak login error messages to be more user-friendly
     * Only uses response codes and actual Keycloak error structure
     */
    private String parseLoginErrorMessage(Throwable ex) {
        // Handle WebClientResponseException with actual Keycloak response
        if (ex instanceof org.springframework.web.reactive.function.client.WebClientResponseException) {
            return parseWebClientError((org.springframework.web.reactive.function.client.WebClientResponseException) ex);
        }
        
        // Handle ServiceUnavailableException (our custom exception)
        if (ex instanceof com.ds.gateway.common.exceptions.ServiceUnavailableException) {
            return "Authentication service is temporarily unavailable. Please try again later";
        }
        
        // For all other errors, return generic message
        return "Login failed. Please check your credentials and try again";
    }
    
    /**
     * Parse WebClientResponseException to extract Keycloak error details
     */
    private String parseWebClientError(org.springframework.web.reactive.function.client.WebClientResponseException ex) {
        String responseBody = ex.getResponseBodyAsString();
        
        try {
            // Try to parse Keycloak error response JSON
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.databind.JsonNode jsonNode = mapper.readTree(responseBody);
            
            // Keycloak error response structure:
            // {
            //   "error": "invalid_grant",
            //   "error_description": "Invalid user credentials"
            // }
            
            String error = jsonNode.has("error") ? jsonNode.get("error").asText() : null;
            String errorDescription = jsonNode.has("error_description") ? jsonNode.get("error_description").asText() : null;
            
            // Map Keycloak error codes to user-friendly messages
            if (error != null) {
                return mapKeycloakErrorCode(error, errorDescription);
            }
            
        } catch (Exception parseEx) {
            log.debug("Failed to parse Keycloak error response: {}", parseEx.getMessage());
        }
        
        // Fallback to HTTP status code mapping
        return mapHttpStatusCodeToMessage(ex.getStatusCode().value());
    }
    
    /**
     * Map Keycloak error codes to user-friendly messages
     */
    private String mapKeycloakErrorCode(String error, String errorDescription) {
        switch (error) {
            case "invalid_grant":
                if (errorDescription != null && errorDescription.contains("Invalid user credentials")) {
                    return "Invalid username or password";
                }
                return "Invalid login credentials";
                
            case "invalid_client":
                return "Authentication configuration error. Please contact administrator";
                
            case "invalid_scope":
                return "Authentication scope error. Please contact administrator";
                
            case "unauthorized_client":
                return "Client not authorized for this operation. Please contact administrator";
                
            case "invalid_request":
                return "Invalid request format. Please check your input";
                
            case "unsupported_grant_type":
                return "Authentication method not supported. Please contact administrator";
                
            case "server_error":
                return "Authentication server error. Please try again later";
                
            default:
                // Use error description if available, otherwise use generic message
                return errorDescription != null ? errorDescription : "Login failed. Please check your credentials";
        }
    }
    
    /**
     * Map HTTP status codes to user-friendly messages
     */
    private String mapHttpStatusCodeToMessage(int statusCode) {
        switch (statusCode) {
            case 400:
                return "Invalid request format or credentials";
            case 401:
                return "Invalid username or password";
            case 403:
                return "Access denied. Please contact administrator";
            case 404:
                return "Authentication endpoint not found. Please contact administrator";
            case 500:
                return "Authentication service error. Please try again later";
            case 503:
                return "Authentication service is temporarily unavailable. Please try again later";
            default:
                return "Login failed. Please check your credentials and try again";
        }
    }
    
    /**
     * Validate authorization header format
     */
    private boolean isValidAuthorizationHeader(String authorization) {
        return authorization != null && authorization.startsWith("Bearer ");
    }
    
    /**
     * Extract token from authorization header
     */
    private String extractTokenFromHeader(String authorization) {
        return isValidAuthorizationHeader(authorization) ? authorization.substring(7) : null;
    }
    
    /**
     * Validate JWT authentication
     */
    private boolean isValidJwtAuthentication(Authentication authentication) {
        return authentication != null && authentication.getPrincipal() instanceof Jwt;
    }
}
