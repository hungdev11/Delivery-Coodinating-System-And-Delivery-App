package com.ds.user.application.startup.data;

import com.ds.user.application.startup.data.services.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.springframework.stereotype.Service;

/**
 * Service for initializing Keycloak data (realms, clients, roles, users)
 * Orchestrates the initialization process using specialized services
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KeycloakInitializationService {

    private final KeycloakInitConfig initConfig;
    private final MasterRealmConnectionService masterRealmConnectionService;
    private final RealmInitializationService realmInitializationService;
    private final RoleInitializationService roleInitializationService;
    private final ClientInitializationService clientInitializationService;
    private final UserInitializationService userInitializationService;

    /**
     * Initialize all Keycloak data based on configuration
     */
    public void initializeKeycloakData() {
        if (!initConfig.isEnabled()) {
            log.info("Keycloak initialization is disabled");
            return;
        }

        log.info("Starting Keycloak initialization...");

        // Connect to master realm
        Keycloak masterKeycloak = masterRealmConnectionService.connectToMasterRealm();
        if (masterKeycloak == null) {
            log.error("Failed to connect to master realm. Aborting initialization.");
            return;
        }

        try {
            // Initialize each realm
            for (KeycloakInitConfig.RealmConfig realmConfig : initConfig.getRealms()) {
                initializeRealm(masterKeycloak, realmConfig);
            }

            log.info("Keycloak initialization completed successfully");
        } catch (Exception e) {
            log.error("Error during Keycloak initialization: {}", e.getMessage(), e);
        } finally {
            masterKeycloak.close();
        }
    }

    /**
     * Initialize a single realm with its clients, roles, and users
     */
    private void initializeRealm(Keycloak masterKeycloak, KeycloakInitConfig.RealmConfig realmConfig) {
        log.info("Initializing realm: {}", realmConfig.getName());

        try {
            // Create or get realm
            RealmResource realmResource = realmInitializationService.createOrGetRealm(masterKeycloak, realmConfig);
            if (realmResource == null) {
                log.error("✗ Failed to create/get realm: {} - skipping realm initialization", realmConfig.getName());
                return;
            }

            log.info("✓ Realm '{}' is ready for configuration", realmConfig.getName());

            // Create realm roles
            log.info("Creating realm roles for '{}'", realmConfig.getName());
            roleInitializationService.createRealmRoles(realmResource, realmConfig.getRoles());

            // Create clients with their roles
            log.info("Creating clients for realm '{}'", realmConfig.getName());
            for (KeycloakInitConfig.ClientConfig clientConfig : realmConfig.getClients()) {
                clientInitializationService.createClient(realmResource, clientConfig);
            }

            // Create users
            log.info("Creating users for realm '{}'", realmConfig.getName());
            userInitializationService.createUsers(realmResource, realmConfig.getUsers());

            log.info("✓ Realm '{}' initialized successfully", realmConfig.getName());
            
        } catch (Exception e) {
            log.error("✗ Failed to initialize realm '{}': {}", realmConfig.getName(), e.getMessage(), e);
        }
    }
}
