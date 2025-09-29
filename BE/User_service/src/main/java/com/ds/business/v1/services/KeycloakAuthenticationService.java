package com.ds.business.v1.services;

import com.ds.common.entities.dto.auth.AuthTokenDto;
import com.ds.common.interfaces.IAuthenticationService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

/**
 * Keycloak implementation of IAuthenticationService
 */
@Service
public class KeycloakAuthenticationService implements IAuthenticationService {
    
    private static final Logger log = LoggerFactory.getLogger(KeycloakAuthenticationService.class);
    
    @Value("${keycloak.auth-server-url}")
    private String serverUrl;
    
    @Value("${keycloak.realm}")
    private String realm;
    
    @Value("${keycloak.resource}")
    private String clientId;
    
    @Value("${keycloak.credentials.secret}")
    private String clientSecret;
    
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Override
    public AuthTokenDto login(String username, String password) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username không được để trống");
        }
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("Password không được để trống");
        }
        
        log.debug("[KeycloakAuth] Attempting login for user: {}", username);
        
        try {
            // Use Keycloak client for authentication
            Keycloak userKeycloak = KeycloakBuilder.builder()
                .serverUrl(serverUrl)
                .realm(realm)
                .username(username)
                .password(password)
                .clientId(clientId)
                .clientSecret(clientSecret)
                .grantType(OAuth2Constants.PASSWORD)
                .build();
            
            // Get token
            org.keycloak.representations.AccessTokenResponse tokenResponse = 
                userKeycloak.tokenManager().getAccessToken();
            
            AuthTokenDto authToken = AuthTokenDto.builder()
                .accessToken(tokenResponse.getToken())
                .refreshToken(tokenResponse.getRefreshToken())
                .tokenType(tokenResponse.getTokenType())
                .expiresIn((int) tokenResponse.getExpiresIn())
                .refreshExpiresIn((int) tokenResponse.getRefreshExpiresIn())
                .build();
            
            log.info("[KeycloakAuth] Login successful for user: {}", username);
            return authToken;
            
        } catch (Exception e) {
            log.error("[KeycloakAuth] Login failed for user: {}, error: {}", username, e.getMessage(), e);
            throw new RuntimeException("Đăng nhập thất bại: " + e.getMessage());
        }
    }
    
    @Override
    public AuthTokenDto refreshToken(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new IllegalArgumentException("Refresh token không được để trống");
        }
        
        log.debug("[KeycloakAuth] Refreshing token");
        
        try {
            String formData = String.format(
                "grant_type=refresh_token&refresh_token=%s&client_id=%s&client_secret=%s",
                URLEncoder.encode(refreshToken, StandardCharsets.UTF_8),
                URLEncoder.encode(clientId, StandardCharsets.UTF_8),
                URLEncoder.encode(clientSecret, StandardCharsets.UTF_8)
            );
            
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(serverUrl + "/realms/" + realm + "/protocol/openid-connect/token"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(formData))
                .build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                JsonNode jsonResponse = objectMapper.readTree(response.body());
                
                AuthTokenDto authToken = AuthTokenDto.builder()
                    .accessToken(jsonResponse.get("access_token").asText())
                    .refreshToken(jsonResponse.get("refresh_token").asText())
                    .tokenType(jsonResponse.get("token_type").asText())
                    .expiresIn(jsonResponse.get("expires_in").asInt())
                    .refreshExpiresIn(jsonResponse.has("refresh_expires_in") ? 
                        jsonResponse.get("refresh_expires_in").asInt() : null)
                    .build();
                
                log.info("[KeycloakAuth] Token refreshed successfully");
                return authToken;
            } else {
                log.error("[KeycloakAuth] Token refresh failed with status: {}", response.statusCode());
                throw new RuntimeException("Token refresh failed with status: " + response.statusCode());
            }
            
        } catch (Exception e) {
            log.error("[KeycloakAuth] Token refresh failed: {}", e.getMessage(), e);
            throw new RuntimeException("Refresh token thất bại: " + e.getMessage());
        }
    }
    
    @Override
    public void logout(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new IllegalArgumentException("Refresh token không được để trống");
        }
        
        log.debug("[KeycloakAuth] Logging out user");
        
        try {
            String formData = String.format(
                "client_id=%s&client_secret=%s&refresh_token=%s",
                URLEncoder.encode(clientId, StandardCharsets.UTF_8),
                URLEncoder.encode(clientSecret, StandardCharsets.UTF_8),
                URLEncoder.encode(refreshToken, StandardCharsets.UTF_8)
            );
            
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(serverUrl + "/realms/" + realm + "/protocol/openid-connect/logout"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(formData))
                .build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200 || response.statusCode() == 204) {
                log.info("[KeycloakAuth] User logged out successfully");
            } else {
                log.error("[KeycloakAuth] Logout failed with status: {}", response.statusCode());
                throw new RuntimeException("Logout failed with status: " + response.statusCode());
            }
            
        } catch (Exception e) {
            log.error("[KeycloakAuth] Logout failed: {}", e.getMessage(), e);
            throw new RuntimeException("Đăng xuất thất bại: " + e.getMessage());
        }
    }
    
    @Override
    public boolean verifyToken(String accessToken) {
        if (accessToken == null || accessToken.isBlank()) {
            return false;
        }
        
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(serverUrl + "/realms/" + realm + "/protocol/openid-connect/userinfo"))
                .header("Authorization", "Bearer " + accessToken)
                .GET()
                .build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            return response.statusCode() == 200;
            
        } catch (Exception e) {
            log.error("[KeycloakAuth] Token verification failed: {}", e.getMessage());
            return false;
        }
    }
}
