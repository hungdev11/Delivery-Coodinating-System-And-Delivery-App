package com.ds.user.application.startup.data;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Initialize Keycloak settings in Settings Service after Keycloak initialization
 */
@Slf4j
@Component
@Order(2) // Run after KeycloakDataInitializer (Order 1)
@RequiredArgsConstructor
public class SettingsInitializer implements CommandLineRunner {

    @Value("${SETTINGS_SERVICE_URL:http://localhost:21502}")
    private String settingsServiceUrl;

    @Value("${KEYCLOAK_HOST:localhost}")
    private String keycloakHost;

    @Value("${KEYCLOAK_PORT:8080}")
    private String keycloakPort;

    @Value("${KEYCLOAK_REALM:keycloak}")
    private String keycloakRealm;

    @Value("${KEYCLOAK_CLIENT_SECRET}")
    private String keycloakClientSecret;

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public void run(String... args) {
        log.info("=".repeat(80));
        log.info("Checking Keycloak Settings in Settings Service");
        log.info("=".repeat(80));

        try {
            // Check if settings already initialized
            if (isSettingsAlreadyInitialized()) {
                log.info("Keycloak settings already initialized - skipping");
            } else {
                log.info("Initializing Keycloak settings...");
                initializeKeycloakSettings();
                log.info("Keycloak settings initialized successfully");
            }
        } catch (Exception e) {
            log.warn("Failed to check/initialize settings (Settings Service may not be running): {}", e.getMessage());
        }

        log.info("=".repeat(80));
    }

    /**
     * Check if Keycloak settings are already initialized
     */
    private boolean isSettingsAlreadyInitialized() {
        try {
            String url = settingsServiceUrl + "/api/v1/settings/group/keycloak";
            List<?> response = restTemplate.getForObject(url, List.class);
            
            // If we got a response and it's not empty, settings are initialized
            if (response != null && !response.isEmpty()) {
                log.info("Found {} existing Keycloak settings", response.size());
                return true;
            } else {
                log.info("No existing Keycloak settings found (list is empty)");
            }
        } catch (Exception e) {
            log.debug("No existing settings found or service not ready: {}", e.getMessage());
        }
        return false;
    }

    private void initializeKeycloakSettings() {
        createSetting(
            "KEYCLOAK_HOST",
            "keycloak",
            "Keycloak server host",
            "STRING",
            keycloakHost,
            "SYSTEM",
            true,
            "TEXT"
        );

        createSetting(
            "KEYCLOAK_PORT",
            "keycloak",
            "Keycloak server port",
            "INTEGER",
            keycloakPort,
            "SYSTEM",
            true,
            "NUMBER"
        );

        createSetting(
            "KEYCLOAK_REALM_BACKEND",
            "keycloak",
            "Backend realm name",
            "STRING",
            keycloakRealm,
            "SYSTEM",
            true,
            "TEXT"
        );

        createSetting(
            "KEYCLOAK_REALM_CLIENT",
            "keycloak",
            "Client realm name",
            "STRING",
            "delivery-system-client",
            "SYSTEM",
            true,
            "TEXT"
        );

        createSetting(
            "KEYCLOAK_CLIENT_BACKEND_ID",
            "keycloak",
            "Backend service client ID",
            "STRING",
            "delivery-backend",
            "SYSTEM",
            true,
            "TEXT"
        );

        createSetting(
            "KEYCLOAK_CLIENT_BACKEND_SECRET",
            "keycloak",
            "Backend service client secret",
            "STRING",
            keycloakClientSecret,
            "SYSTEM",
            true,
            "PASSWORD"
        );

        createSetting(
            "KEYCLOAK_CLIENT_WEB_ID",
            "keycloak",
            "Web application client ID",
            "STRING",
            "delivery-management-web",
            "SYSTEM",
            true,
            "TEXT"
        );

        createSetting(
            "KEYCLOAK_CLIENT_MOBILE_ID",
            "keycloak",
            "Mobile application client ID",
            "STRING",
            "delivery-mobile-app",
            "SYSTEM",
            true,
            "TEXT"
        );

        createSetting(
            "KEYCLOAK_AUTH_SERVER_URL",
            "keycloak",
            "Keycloak authentication server URL",
            "STRING",
            "http://" + keycloakHost + ":" + keycloakPort,
            "SYSTEM",
            false,
            "URL"
        );
    }

    private void createSetting(String key, String group, String description, 
                               String type, String value, String level, boolean isReadOnly, String displayMode) {
        try {
            String url = settingsServiceUrl + "/api/v1/settings";

            Map<String, Object> request = new HashMap<>();
            request.put("key", key);
            request.put("group", group);
            request.put("description", description);
            request.put("type", type);
            request.put("value", value);
            request.put("level", level);
            request.put("isReadOnly", isReadOnly);
            request.put("displayMode", displayMode);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

            restTemplate.postForObject(url, entity, Object.class);
            log.info("  ✓ Created: {}", key);
        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().contains("already exists")) {
                log.debug("  Setting '{}' already exists", key);
            } else {
                log.warn("  Failed to create '{}': {}", key, e.getMessage());
            }
        }
    }
}
