package com.ds.user.business.v1.services;

import com.ds.user.common.entities.dto.auth.AuthTokenDto;
import com.ds.user.common.interfaces.IAuthenticationService;
import com.ds.user.common.interfaces.ISettingsReaderService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
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
 * Retrieves client secrets from Settings Service for enhanced security
 */
@Service
@RequiredArgsConstructor
public class KeycloakAuthenticationService implements IAuthenticationService {

    private static final Logger log = LoggerFactory.getLogger(KeycloakAuthenticationService.class);
    private static final String KEYCLOAK_GROUP = "keycloak";

    @Value("${keycloak.auth-server-url}")
    private String serverUrl;

    @Value("${keycloak.realm}")
    private String realm;

    @Value("${keycloak.resource}")
    private String clientId;

    @Value("${keycloak.credentials.secret:}")
    private String fallbackClientSecret;

    private final ISettingsReaderService settingsReaderService;
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Get client secret by clientId from Settings Service
     */
    private String getClientSecret(String targetClientId) {
        String clientKey = targetClientId.replace("-", "_").toUpperCase();
        String settingKey = "KEYCLOAK_CLIENT_" + clientKey + "_SECRET";

        String secret = settingsReaderService.getSettingValue(settingKey, KEYCLOAK_GROUP, null);

        if (secret != null) {
            log.debug("Using client secret from Settings Service for client: {}", targetClientId);
            return secret;
        }

        // Fallback to config if Settings Service doesn't have it
        if (fallbackClientSecret != null && !fallbackClientSecret.isEmpty()) {
            log.debug("Using fallback client secret from config for client: {}", targetClientId);
            return fallbackClientSecret;
        }

        log.debug("[user-service] [KeycloakAuthenticationService.getClientSecret] No client secret found for client '{}' in Settings Service or config", targetClientId);
        return "";
    }

    /**
     * Get server URL from Settings Service or fallback to config
     */
    private String getServerUrl() {
        String url = settingsReaderService.getSettingValue("KEYCLOAK_AUTH_SERVER_URL", KEYCLOAK_GROUP, null);
        return url != null ? url : serverUrl;
    }

    /**
     * Get default realm from Settings Service or fallback to config
     */
    private String getDefaultRealm() {
        String realmFromSettings = settingsReaderService.getSettingValue("KEYCLOAK_REALM", KEYCLOAK_GROUP, null);
        return realmFromSettings != null ? realmFromSettings : realm;
    }

    /**
     * Get default client ID from Settings Service or fallback to config
     */
    private String getDefaultClientId() {
        String clientFromSettings = settingsReaderService.getSettingValue("KEYCLOAK_CLIENT_ID", KEYCLOAK_GROUP, null);
        return clientFromSettings != null ? clientFromSettings : clientId;
    }

    /**
     * Get realm info from Settings Service
     */
    private String getRealmFromSettings(String targetRealm) {
        String realmKey = targetRealm.replace("-", "_").toUpperCase();
        String settingKey = "KEYCLOAK_REALM_" + realmKey;

        String realmFromSettings = settingsReaderService.getSettingValue(settingKey, KEYCLOAK_GROUP, null);
        return realmFromSettings != null ? realmFromSettings : targetRealm;
    }

    @Override
    public AuthTokenDto login(String username, String password) {
        return login(username, password, getDefaultRealm(), getDefaultClientId());
    }

    @Override
    public AuthTokenDto login(String username, String password, String targetRealm, String targetClientId) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username không được để trống");
        }
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("Password không được để trống");
        }
        if (targetRealm == null || targetRealm.isBlank()) {
            throw new IllegalArgumentException("Realm không được để trống");
        }
        if (targetClientId == null || targetClientId.isBlank()) {
            throw new IllegalArgumentException("Client ID không được để trống");
        }

        log.debug("[KeycloakAuth] Attempting login for user: {} in realm: {} with client: {}", username, targetRealm,
                targetClientId);

        try {
            // Get configurations from Settings Service
            String serverUrlToUse = getServerUrl();
            String realmToUse = getRealmFromSettings(targetRealm);
            String secret = getClientSecret(targetClientId);

            // Use Keycloak client for authentication
            Keycloak userKeycloak = KeycloakBuilder.builder()
                    .serverUrl(serverUrlToUse)
                    .realm(realmToUse)
                    .username(username)
                    .password(password)
                    .clientId(targetClientId)
                    .clientSecret(secret)
                    .grantType(OAuth2Constants.PASSWORD)
                    .build();

            // Get token
            org.keycloak.representations.AccessTokenResponse tokenResponse = userKeycloak.tokenManager()
                    .getAccessToken();

            AuthTokenDto authToken = AuthTokenDto.builder()
                    .accessToken(tokenResponse.getToken())
                    .refreshToken(tokenResponse.getRefreshToken())
                    .tokenType(tokenResponse.getTokenType())
                    .expiresIn((int) tokenResponse.getExpiresIn())
                    .refreshExpiresIn((int) tokenResponse.getRefreshExpiresIn())
                    .build();

            log.debug("[KeycloakAuth] Login successful for user: {} in realm: {} with client: {}", username,
                    targetRealm, targetClientId);
            return authToken;

        } catch (Exception e) {
            log.error("[user-service] [KeycloakAuthenticationService.login] Login failed for user: {} in realm: {} with client: {}", username, targetRealm, targetClientId, e);
            throw new RuntimeException("Đăng nhập thất bại: " + e.getMessage());
        }
    }

    @Override
    public AuthTokenDto refreshToken(String refreshToken) {
        return refreshToken(refreshToken, getDefaultRealm(), getDefaultClientId());
    }

    @Override
    public AuthTokenDto refreshToken(String refreshToken, String targetRealm, String targetClientId) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new IllegalArgumentException("Refresh token không được để trống");
        }
        if (targetRealm == null || targetRealm.isBlank()) {
            throw new IllegalArgumentException("Realm không được để trống");
        }
        if (targetClientId == null || targetClientId.isBlank()) {
            throw new IllegalArgumentException("Client ID không được để trống");
        }

        log.debug("[KeycloakAuth] Refreshing token for realm: {} with client: {}", targetRealm, targetClientId);

        try {
            // Get configurations from Settings Service
            String serverUrlToUse = getServerUrl();
            String realmToUse = getRealmFromSettings(targetRealm);
            String secret = getClientSecret(targetClientId);

            String formData = String.format(
                    "grant_type=refresh_token&refresh_token=%s&client_id=%s&client_secret=%s",
                    URLEncoder.encode(refreshToken, StandardCharsets.UTF_8),
                    URLEncoder.encode(targetClientId, StandardCharsets.UTF_8),
                    URLEncoder.encode(secret, StandardCharsets.UTF_8));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(serverUrlToUse + "/realms/" + realmToUse + "/protocol/openid-connect/token"))
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
                        .refreshExpiresIn(
                                jsonResponse.has("refresh_expires_in") ? jsonResponse.get("refresh_expires_in").asInt()
                                        : null)
                        .build();

                log.debug("[KeycloakAuth] Token refreshed successfully for realm: {} with client: {}", targetRealm,
                        targetClientId);
                return authToken;
            } else {
                log.error("[user-service] [KeycloakAuthenticationService.refreshToken] Token refresh failed with status: {} for realm: {} with client: {}", response.statusCode(), targetRealm, targetClientId);
                throw new RuntimeException("Token refresh failed with status: " + response.statusCode());
            }

        } catch (Exception e) {
            log.error("[user-service] [KeycloakAuthenticationService.refreshToken] Token refresh failed for realm: {} with client: {}", targetRealm, targetClientId, e);
            throw new RuntimeException("Refresh token thất bại: " + e.getMessage());
        }
    }

    @Override
    public void logout(String refreshToken) {
        logout(refreshToken, getDefaultRealm(), getDefaultClientId());
    }

    @Override
    public void logout(String refreshToken, String targetRealm, String targetClientId) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new IllegalArgumentException("Refresh token không được để trống");
        }
        if (targetRealm == null || targetRealm.isBlank()) {
            throw new IllegalArgumentException("Realm không được để trống");
        }
        if (targetClientId == null || targetClientId.isBlank()) {
            throw new IllegalArgumentException("Client ID không được để trống");
        }

        log.debug("[KeycloakAuth] Logging out user from realm: {} with client: {}", targetRealm, targetClientId);

        try {
            // Get configurations from Settings Service
            String serverUrlToUse = getServerUrl();
            String realmToUse = getRealmFromSettings(targetRealm);
            String secret = getClientSecret(targetClientId);

            String formData = String.format(
                    "client_id=%s&client_secret=%s&refresh_token=%s",
                    URLEncoder.encode(targetClientId, StandardCharsets.UTF_8),
                    URLEncoder.encode(secret, StandardCharsets.UTF_8),
                    URLEncoder.encode(refreshToken, StandardCharsets.UTF_8));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(serverUrlToUse + "/realms/" + realmToUse + "/protocol/openid-connect/logout"))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(formData))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200 || response.statusCode() == 204) {
                log.debug("[KeycloakAuth] User logged out successfully from realm: {} with client: {}", targetRealm,
                        targetClientId);
            } else {
                log.error("[user-service] [KeycloakAuthenticationService.logout] Logout failed with status: {} for realm: {} with client: {}", response.statusCode(), targetRealm, targetClientId);
                throw new RuntimeException("Logout failed with status: " + response.statusCode());
            }

        } catch (Exception e) {
            log.error("[user-service] [KeycloakAuthenticationService.logout] Logout failed for realm: {} with client: {}", targetRealm, targetClientId, e);
            throw new RuntimeException("Đăng xuất thất bại: " + e.getMessage());
        }
    }

    @Override
    public boolean verifyToken(String accessToken) {
        return verifyToken(accessToken, getDefaultRealm());
    }

    @Override
    public boolean verifyToken(String accessToken, String targetRealm) {
        if (accessToken == null || accessToken.isBlank()) {
            return false;
        }
        if (targetRealm == null || targetRealm.isBlank()) {
            return false;
        }

        try {
            // Get configurations from Settings Service
            String serverUrlToUse = getServerUrl();
            String realmToUse = getRealmFromSettings(targetRealm);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(serverUrlToUse + "/realms/" + realmToUse + "/protocol/openid-connect/userinfo"))
                    .header("Authorization", "Bearer " + accessToken)
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            boolean isValid = response.statusCode() == 200;
            log.debug("[KeycloakAuth] Token verification for realm: {} - {}", targetRealm,
                    isValid ? "VALID" : "INVALID");
            return isValid;

        } catch (Exception e) {
            log.error("[user-service] [KeycloakAuthenticationService.verifyToken] Token verification failed for realm: {}", targetRealm, e);
            return false;
        }
    }
}
