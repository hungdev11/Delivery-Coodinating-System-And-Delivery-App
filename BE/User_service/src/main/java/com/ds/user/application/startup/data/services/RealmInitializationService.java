package com.ds.user.application.startup.data.services;

import com.ds.user.application.startup.data.KeycloakInitConfig;
import com.ds.user.common.interfaces.ISettingsWriterService;
import jakarta.ws.rs.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.RealmRepresentation;
import org.springframework.stereotype.Service;

/**
 * Service for initializing Keycloak realms
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RealmInitializationService {

    private final ISettingsWriterService settingsWriterService;

    /**
     * Create or get existing realm
     */
    public RealmResource createOrGetRealm(Keycloak masterKeycloak, KeycloakInitConfig.RealmConfig config) {
        try {
            // Check if realm exists
            RealmResource realmResource = masterKeycloak.realm(config.getName());
            realmResource.toRepresentation();
            log.info("Realm '{}' already exists", config.getName());
            writeRealmSettings(config);
            return realmResource;
        } catch (NotFoundException e) {
            // Realm doesn't exist, create it
            log.info("Creating new realm: {}", config.getName());
            RealmRepresentation realmRepresentation = new RealmRepresentation();
            realmRepresentation.setRealm(config.getName());
            realmRepresentation.setDisplayName(config.getDisplayName() != null ? config.getDisplayName() : config.getName());
            realmRepresentation.setEnabled(config.isEnabled());
            realmRepresentation.setRegistrationAllowed(false);
            realmRepresentation.setLoginWithEmailAllowed(true);
            realmRepresentation.setDuplicateEmailsAllowed(false);
            realmRepresentation.setResetPasswordAllowed(true);
            realmRepresentation.setEditUsernameAllowed(false);

            try {
                masterKeycloak.realms().create(realmRepresentation);
                log.info("Realm '{}' created successfully", config.getName());
                writeRealmSettings(config);
                return masterKeycloak.realm(config.getName());
            } catch (Exception ex) {
                log.error("Failed to create realm '{}': {}", config.getName(), ex.getMessage(), ex);
                return null;
            }
        }
    }

    private void writeRealmSettings(KeycloakInitConfig.RealmConfig config) {
        String realmKey = config.getName().replace("-", "_").toUpperCase();
        
        // Save realm name
        settingsWriterService.createSetting(
                "KEYCLOAK_REALM_" + realmKey,
                "keycloak",
                "Realm: " + (config.getDisplayName() != null ? config.getDisplayName() : config.getName()),
                "STRING",
                config.getName(),
                "SYSTEM",
                true,
                "TEXT"
        );
        
        // Generate and save a unique realm secret for internal service communication
        // This is different from client secrets and can be used for realm-level operations
        String realmSecret = java.util.UUID.randomUUID().toString().replace("-", "");
        settingsWriterService.createSetting(
                "KEYCLOAK_REALM_" + realmKey + "_SECRET",
                "keycloak",
                "Realm secret for " + (config.getDisplayName() != null ? config.getDisplayName() : config.getName()),
                "STRING",
                realmSecret,
                "SYSTEM",
                true,
                "PASSWORD"
        );
        log.info("  ✓ Generated and saved realm secret for '{}'", config.getName());
    }
}
