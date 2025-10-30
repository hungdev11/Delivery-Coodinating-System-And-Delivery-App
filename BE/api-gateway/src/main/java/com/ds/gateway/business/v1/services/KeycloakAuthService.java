package com.ds.gateway.business.v1.services;

import com.ds.gateway.common.entities.dto.auth.*;
import com.ds.gateway.common.enums.LoginType;
import com.ds.gateway.common.exceptions.ServiceUnavailableException;
import com.ds.gateway.common.interfaces.IKeycloakAuthService;
import com.ds.gateway.common.interfaces.IUserServiceClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.security.oauth2.jwt.Jwt;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Keycloak authentication service implementation
 * Handles JWT token validation, user info extraction, and token refresh
 */
@Slf4j
@Service
public class KeycloakAuthService implements IKeycloakAuthService {
    
    @Autowired
    @Qualifier("keycloakWebClient")
    private WebClient keycloakWebClient;
    
    @Autowired
    private SettingsClient settingsClient;
    
    @Autowired
    private IUserServiceClient userServiceClient;
    
    @Value("${keycloak.backend.realm}")
    private String backendRealm;
    
    @Value("${keycloak.default.realm}")
    private String defaultRealm;
    
    @Value("${keycloak.default.client-id}")
    private String defaultClientId;
    
    @Override
    public CompletableFuture<LoginResponseDto> login(String username, String password, String type) {
        log.debug("🔐 SERVICE LOGIN - Username: {}, Type: {}", username, type);
        
        // Get login type configuration
        LoginType loginType = LoginType.fromString(type);
        String targetRealm = loginType.getRealm();
        String targetClientId = loginType.getClientId();
        
        log.debug("🔐 SERVICE LOGIN - Using realm: {} and client: {} for login type: {}", targetRealm, targetClientId, loginType);
        
        return loginWithRealmAndClient(username, password, targetRealm, targetClientId);
    }
    
    /**
     * Login with specific realm and client ID and return tokens + user info
     */
    @Override
    public CompletableFuture<LoginResponseDto> loginWithRealmAndClient(String username, String password, String realm, String clientId) {
        log.debug("🔐 SERVICE LOGIN - Username: {}, Realm: {}, Client: {}", username, realm, clientId);
        
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "password");
        formData.add("username", username);
        formData.add("password", password);
        formData.add("client_id", clientId);
        String clientSecret = resolveClientSecret(clientId);
        if (clientSecret != null && !clientSecret.isBlank()) {
            formData.add("client_secret", clientSecret);
            log.debug("🔐 SERVICE LOGIN - Using client secret for client: {}", clientId);
        } else {
            log.debug("🔐 SERVICE LOGIN - No client secret for client: {}", clientId);
        }
        
        return keycloakWebClient.post()
            .uri("/realms/{realm}/protocol/openid-connect/token", realm)
            .header("Content-Type", "application/x-www-form-urlencoded")
            .bodyValue(formData)
            .retrieve()
            .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
            .map(tokenMap -> {
                log.debug("🔐 SERVICE LOGIN - Keycloak token response received for user: {}", username);
                return mapToTokenResponseDto(tokenMap);
            })
            .flatMap(tokenResponse -> {
                log.debug("🔐 SERVICE LOGIN - Fetching user info from User Service for user: {}", username);
                // Get user info from User Service after successful Keycloak login
                return Mono.fromFuture(userServiceClient.getUserByUsername(username))
                    .map(user -> {
                        log.debug("🔐 SERVICE LOGIN - User info retrieved, building LoginResponseDto for user: {}", username);
                        return LoginResponseDto.builder()
                            .message("Login successful")
                            .accessToken(tokenResponse.getAccessToken())
                            .refreshToken(tokenResponse.getRefreshToken())
                            .tokenType(tokenResponse.getTokenType())
                            .expiresIn(tokenResponse.getExpiresIn())
                            .user(user)
                            .build();
                    });
            })
            .onErrorMap(ex -> {
                log.error("❌ SERVICE LOGIN - Keycloak authentication failed for user: {}, error: {}", username, ex.getMessage());
                return new ServiceUnavailableException("Keycloak login service unavailable: " + ex.getMessage(), ex);
            })
            .toFuture();
    }
    
    /**
     * Default login using default realm and client configuration from Settings Service
     */
    @Override
    public CompletableFuture<LoginResponseDto> defaultLogin(String username, String password) {
        log.debug("🔐 SERVICE DEFAULT LOGIN - Username: {}", username);
        
        // Get dynamic default values from Settings Service
        String dynamicRealm = settingsClient.getDefaultRealm();
        String dynamicClientId = settingsClient.getDefaultClientId();
        
        // Fallback to configuration values if Settings Service is unavailable
        String targetRealm = (dynamicRealm != null) ? dynamicRealm : defaultRealm;
        String targetClientId = (dynamicClientId != null) ? dynamicClientId : defaultClientId;
        
        log.debug("🔐 SERVICE DEFAULT LOGIN - Using realm: {} and client: {} for user: {}", targetRealm, targetClientId, username);
        return loginWithRealmAndClient(username, password, targetRealm, targetClientId);
    }
    
    @Override
    public CompletableFuture<KeycloakUserInfoDto> validateTokenAndGetUserInfo(String accessToken) {
        log.debug("Validating token and getting user info from Keycloak");
        
        return keycloakWebClient.get()
            .uri("/realms/{realm}/protocol/openid-connect/userinfo", backendRealm)
            .header("Authorization", "Bearer " + accessToken)
            .retrieve()
            .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
            .map(this::mapToUserInfoDto)
            .onErrorMap(ex -> new ServiceUnavailableException("Keycloak user info service unavailable: " + ex.getMessage(), ex))
            .toFuture();
    }
    
    @Override
    public CompletableFuture<KeycloakTokenResponseDto> refreshToken(String refreshToken) {
        log.debug("Refreshing token via Keycloak");
        
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "refresh_token");
        formData.add("refresh_token", refreshToken);
        formData.add("client_id", defaultClientId);
        String refreshClientSecret = resolveClientSecret(defaultClientId);
        if (refreshClientSecret != null && !refreshClientSecret.isBlank()) {
            formData.add("client_secret", refreshClientSecret);
        }
        
        return keycloakWebClient.post()
            .uri("/realms/{realm}/protocol/openid-connect/token", defaultRealm)
            .header("Content-Type", "application/x-www-form-urlencoded")
            .bodyValue(formData)
            .retrieve()
            .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
            .map(this::mapToTokenResponseDto)
            .onErrorMap(ex -> new ServiceUnavailableException("Keycloak token refresh service unavailable: " + ex.getMessage(), ex))
            .toFuture();
    }
    
    @Override
    public KeycloakUserInfoDto extractUserInfoFromJwt(Jwt jwt) {
        log.debug("Extracting user info from JWT token");
        
        return KeycloakUserInfoDto.builder()
            .sub(jwt.getSubject())
            .preferredUsername(jwt.getClaimAsString("preferred_username"))
            .email(jwt.getClaimAsString("email"))
            .givenName(jwt.getClaimAsString("given_name"))
            .familyName(jwt.getClaimAsString("family_name"))
            .name(jwt.getClaimAsString("name"))
            .emailVerified(jwt.getClaimAsBoolean("email_verified"))
            .roles(extractRoles(jwt))
            .realmAccess(jwt.getClaim("realm_access"))
            .resourceAccess(jwt.getClaim("resource_access"))
            .exp(jwt.getExpiresAt() != null ? jwt.getExpiresAt().getEpochSecond() : null)
            .iat(jwt.getIssuedAt() != null ? jwt.getIssuedAt().getEpochSecond() : null)
            .iss(jwt.getClaimAsString("iss"))
            .aud(String.join(",", jwt.getClaimAsStringList("aud")))
            .build();
    }
    
    @Override
    public CompletableFuture<Boolean> isTokenValid(String accessToken) {
        log.debug("Checking token validity");
        
        return validateTokenAndGetUserInfo(accessToken)
            .thenApply(userInfo -> {
                // Check if token is expired
                if (userInfo.getExp() != null) {
                    long currentTime = System.currentTimeMillis() / 1000;
                    return userInfo.getExp() > currentTime;
                }
                return true; // If no expiration claim, assume valid
            })
            .exceptionally(ex -> {
                log.warn("Token validation failed: {}", ex.getMessage());
                return false;
            });
    }
    
    @Override
    public CompletableFuture<Boolean> logout(String refreshToken) {
        log.debug("Logging out user via Keycloak");
        
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("refresh_token", refreshToken);
        formData.add("client_id", defaultClientId);
        String logoutClientSecret = resolveClientSecret(defaultClientId);
        if (logoutClientSecret != null && !logoutClientSecret.isBlank()) {
            formData.add("client_secret", logoutClientSecret);
        }
        
        return keycloakWebClient.post()
            .uri("/realms/{realm}/protocol/openid-connect/logout", defaultRealm)
            .header("Content-Type", "application/x-www-form-urlencoded")
            .bodyValue(formData)
            .retrieve()
            .bodyToMono(Void.class)
            .thenReturn(true)
            .onErrorResume(ex -> {
                log.warn("Logout failed: {}", ex.getMessage());
                return Mono.just(false);
            })
            .toFuture();
    }
    
    /**
     * Map Keycloak user info response to DTO
     */
    private KeycloakUserInfoDto mapToUserInfoDto(Map<String, Object> userInfoMap) {
        return KeycloakUserInfoDto.builder()
            .sub((String) userInfoMap.get("sub"))
            .preferredUsername((String) userInfoMap.get("preferred_username"))
            .email((String) userInfoMap.get("email"))
            .givenName((String) userInfoMap.get("given_name"))
            .familyName((String) userInfoMap.get("family_name"))
            .name((String) userInfoMap.get("name"))
            .emailVerified((Boolean) userInfoMap.get("email_verified"))
            .build();
    }
    
    /**
     * Map Keycloak token response to DTO
     */
    private KeycloakTokenResponseDto mapToTokenResponseDto(Map<String, Object> tokenMap) {
        return KeycloakTokenResponseDto.builder()
            .accessToken((String) tokenMap.get("access_token"))
            .refreshToken((String) tokenMap.get("refresh_token"))
            .tokenType((String) tokenMap.get("token_type"))
            .expiresIn(((Number) tokenMap.get("expires_in")).intValue())
            .refreshExpiresIn(((Number) tokenMap.get("refresh_expires_in")).intValue())
            .scope((String) tokenMap.get("scope"))
            .build();
    }
    
    /**
     * Extract roles from JWT token
     */
    @SuppressWarnings("unchecked")
    private List<String> extractRoles(Jwt jwt) {
        try {
            Map<String, Object> realmAccess = jwt.getClaim("realm_access");
            if (realmAccess != null) {
                return (List<String>) realmAccess.get("roles");
            }
        } catch (Exception e) {
            log.warn("Failed to extract roles from JWT: {}", e.getMessage());
        }
        return List.of();
    }
    
    /**
     * Get client secret based on client ID
     */
    private String resolveClientSecret(String clientId) {
        // Get client secret from Settings Service
        return settingsClient.getClientSecretForClientId(clientId);
    }
}
