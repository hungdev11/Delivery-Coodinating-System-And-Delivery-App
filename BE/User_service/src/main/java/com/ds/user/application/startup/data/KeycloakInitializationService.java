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
    private final UserAddressSeedService userAddressSeedService;
    private final ParcelSeedService parcelSeedService;

    /**
     * Initialize all Keycloak data based on configuration
     */
    public void initializeKeycloakData() {
        if (!initConfig.isEnabled()) {
            log.warn("⚠️ Keycloak initialization is DISABLED (KEYCLOAK_INIT_ENABLED=false)");
            log.warn("⚠️ No users/roles will be seeded. Set KEYCLOAK_INIT_ENABLED=true to enable seeding.");
            return;
        }

        log.info("🚀 Starting Keycloak initialization...");
        log.info("📋 Configuration: {} realm(s) to initialize", initConfig.getRealms().size());

        // Connect to master realm
        Keycloak masterKeycloak = masterRealmConnectionService.connectToMasterRealm();
        if (masterKeycloak == null) {
            log.error("❌ Failed to connect to master realm. Aborting initialization.");
            log.error("❌ Please check:");
            log.error("   - KEYCLOAK_URL is correct: {}", initConfig.getMaster().getRealm());
            log.error("   - KEYCLOAK_ADMIN_USERNAME and KEYCLOAK_ADMIN_PASSWORD are set");
            log.error("   - Keycloak server is running and accessible");
            throw new RuntimeException("Failed to connect to Keycloak master realm");
        }

        try {
            int successCount = 0;
            int failCount = 0;
            
            // Initialize each realm
            for (KeycloakInitConfig.RealmConfig realmConfig : initConfig.getRealms()) {
                try {
                    initializeRealm(masterKeycloak, realmConfig);
                    successCount++;
                } catch (Exception e) {
                    failCount++;
                    log.error("❌ Failed to initialize realm '{}': {}", realmConfig.getName(), e.getMessage());
                }
            }

            log.info("✅ Keycloak initialization completed: {} success, {} failed", successCount, failCount);
            if (failCount > 0) {
                log.warn("⚠️ Some realms failed to initialize. Check logs above for details.");
            }
        } catch (Exception e) {
            log.error("❌ Error during Keycloak initialization: {}", e.getMessage(), e);
            throw e; // Re-throw to be caught by KeycloakDataInitializer
        } finally {
            try {
                masterKeycloak.close();
            } catch (Exception e) {
                log.warn("Error closing Keycloak connection: {}", e.getMessage());
            }
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
            
            // Seed primary addresses for shop and client users
            // Note: This is done after users are created to ensure users exist in the database
            try {
                userAddressSeedService.seedPrimaryAddressesForUsers(realmConfig);
            } catch (Exception e) {
                log.error("✗ Failed to seed primary addresses for realm '{}': {}", 
                        realmConfig.getName(), e.getMessage(), e);
                // Don't fail entire initialization if address seeding fails
            }
            
            // Seed parcels (orders) for shops and clients
            // Note: This is done after addresses are seeded to ensure addresses exist
            try {
                parcelSeedService.seedParcels();
            } catch (Exception e) {
                log.error("✗ Failed to seed parcels for realm '{}': {}", 
                        realmConfig.getName(), e.getMessage(), e);
                // Don't fail entire initialization if parcel seeding fails
            }
            
        } catch (Exception e) {
            log.error("✗ Failed to initialize realm '{}': {}", realmConfig.getName(), e.getMessage(), e);
        }
    }
}
