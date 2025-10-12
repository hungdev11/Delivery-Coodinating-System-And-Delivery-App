package com.ds.user.application.startup.data.services;

import com.ds.user.application.startup.data.KeycloakInitConfig;
import com.ds.user.common.interfaces.ISettingsWriterService;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Service for initializing Keycloak clients
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ClientInitializationService {

    private final RoleInitializationService roleInitializationService;
    private final ISettingsWriterService settingsWriterService;

    /**
     * Create or get existing client with its roles
     */
    public void createClient(RealmResource realmResource, KeycloakInitConfig.ClientConfig clientConfig) {
        log.info("Processing client: {}", clientConfig.getClientId());

        try {
            // Check if client exists
            log.debug("Checking if client '{}' exists...", clientConfig.getClientId());
            Optional<ClientRepresentation> existingClient = realmResource.clients()
                    .findByClientId(clientConfig.getClientId())
                    .stream()
                    .findFirst();

            if (existingClient.isPresent()) {
                log.info("✓ Client '{}' already exists - retrieving existing data", clientConfig.getClientId());
                
                // Client exists, save settings and create roles
                writeClientSettings(realmResource, existingClient.get());
                
                if (!clientConfig.isPublicClient()) {
                    log.info("Client '{}' is confidential; secret managed in Keycloak and Settings Service", clientConfig.getClientId());
                }
                
                // Create/update client roles
                roleInitializationService.createClientRoles(realmResource, existingClient.get().getId(), clientConfig.getRoles());
                return;
            }

            // Client doesn't exist, create it
            log.info("Client '{}' does not exist - creating new client...", clientConfig.getClientId());
            createNewClient(realmResource, clientConfig);

        } catch (Exception e) {
            log.error("✗ Error processing client '{}': {}", clientConfig.getClientId(), e.getMessage(), e);
        }
    }
    
    /**
     * Create a new client
     */
    private void createNewClient(RealmResource realmResource, KeycloakInitConfig.ClientConfig clientConfig) {
        try {
            ClientRepresentation clientRepresentation = new ClientRepresentation();
            clientRepresentation.setClientId(clientConfig.getClientId());
            clientRepresentation.setName(clientConfig.getName());
            clientRepresentation.setEnabled(true);
            clientRepresentation.setPublicClient(clientConfig.isPublicClient());
            clientRepresentation.setServiceAccountsEnabled(clientConfig.isServiceAccountsEnabled());
            clientRepresentation.setStandardFlowEnabled(clientConfig.isStandardFlowEnabled());
            clientRepresentation.setDirectAccessGrantsEnabled(clientConfig.isDirectAccessGrantsEnabled());
            clientRepresentation.setRedirectUris(clientConfig.getRedirectUris());
            clientRepresentation.setWebOrigins(clientConfig.getWebOrigins());
            
            // Set client secret if provided (will be auto-generated if empty)
            if (clientConfig.getSecret() != null && !clientConfig.getSecret().isBlank()) {
                clientRepresentation.setSecret(clientConfig.getSecret());
            }

            Response response = realmResource.clients().create(clientRepresentation);
            
            if (response.getStatus() == 201) {
                log.info("✓ Client '{}' created successfully", clientConfig.getClientId());
                
                // Get created client ID
                String clientId = extractIdFromLocationHeader(response);
                if (clientId != null) {
                    // Create/update settings for the client
                    ClientRepresentation created = realmResource.clients().get(clientId).toRepresentation();
                    writeClientSettings(realmResource, created);
                    roleInitializationService.createClientRoles(realmResource, clientId, clientConfig.getRoles());
                } else {
                    log.error("✗ Failed to extract client ID from response for '{}'", clientConfig.getClientId());
                }
            } else {
                log.error("✗ Failed to create client '{}'. Status: {}", clientConfig.getClientId(), response.getStatus());
            }
            response.close();

        } catch (Exception e) {
            log.error("✗ Failed to create client '{}': {}", clientConfig.getClientId(), e.getMessage(), e);
        }
    }

    /**
     * Extract ID from Location header in response
     */
    private String extractIdFromLocationHeader(Response response) {
        String location = response.getHeaderString("Location");
        if (location != null) {
            String[] parts = location.split("/");
            return parts[parts.length - 1];
        }
        return null;
    }

    private void writeClientSettings(RealmResource realmResource, ClientRepresentation client) {
        String realmName = realmResource.toRepresentation().getRealm();
        String realmKey = realmName.replace("-", "_").toUpperCase();
        String clientKey = client.getClientId().replace("-", "_").toUpperCase();
        
        // Save client ID
        settingsWriterService.createSetting(
                "KEYCLOAK_CLIENT_" + clientKey + "_ID",
                "keycloak",
                "Client ID for " + client.getName(),
                "STRING",
                client.getClientId(),
                "SYSTEM",
                true,
                "TEXT"
        );
        
        // Save realm info
        settingsWriterService.createSetting(
                "KEYCLOAK_REALM_" + realmKey,
                "keycloak",
                "Realm: " + realmName,
                "STRING",
                realmName,
                "SYSTEM",
                true,
                "TEXT"
        );
        
        // Save default realm and client configuration
        settingsWriterService.createSetting(
                "KEYCLOAK_DEFAULT_REALM",
                "keycloak",
                "Default Keycloak realm for authentication",
                "STRING",
                realmName,
                "SYSTEM",
                true,
                "TEXT"
        );
        
        settingsWriterService.createSetting(
                "KEYCLOAK_DEFAULT_CLIENT_ID",
                "keycloak",
                "Default Keycloak client ID for authentication",
                "STRING",
                client.getClientId(),
                "SYSTEM",
                true,
                "TEXT"
        );
        
        // Get and save client secret for confidential clients
        if (Boolean.FALSE.equals(client.isPublicClient())) {
            try {
                log.debug("  Processing confidential client '{}' - attempting to get/generate secret", client.getClientId());
                
                // Get the actual client secret from Keycloak
                org.keycloak.representations.idm.CredentialRepresentation secret = 
                    realmResource.clients().get(client.getId()).getSecret();
                
                String secretValue = null;
                
                if (secret != null && secret.getValue() != null && !secret.getValue().trim().isEmpty()) {
                    secretValue = secret.getValue();
                    log.debug("  ✓ Retrieved existing secret for client '{}'", client.getClientId());
                } else {
                    log.warn("  ⚠ Client '{}' is confidential but no valid secret was found. Generating new secret...", client.getClientId());
                    
                    // Generate a new secret for the client
                    org.keycloak.representations.idm.CredentialRepresentation newSecret = 
                        realmResource.clients().get(client.getId()).generateNewSecret();
                    
                    if (newSecret != null && newSecret.getValue() != null && !newSecret.getValue().trim().isEmpty()) {
                        secretValue = newSecret.getValue();
                        log.debug("  ✓ Generated new secret for client '{}'", client.getClientId());
                    } else {
                        log.error("  ✗ Failed to generate valid secret for client '{}'", client.getClientId());
                    }
                }
                
                // Save the secret to Settings Service if we have a valid value
                if (secretValue != null && !secretValue.trim().isEmpty()) {
                    settingsWriterService.createSetting(
                            "KEYCLOAK_CLIENT_" + clientKey + "_SECRET",
                            "keycloak",
                            "Client secret for " + client.getName(),
                            "STRING",
                            secretValue,
                            "SYSTEM",
                            true,
                            "PASSWORD"
                    );
                    log.info("  ✓ Saved client secret for '{}' to Settings Service (length: {})", 
                        client.getClientId(), secretValue.length());
                } else {
                    log.error("  ✗ Cannot save secret for client '{}' - value is empty or null", client.getClientId());
                }
                
            } catch (Exception e) {
                log.error("  ✗ Failed to retrieve/generate/save secret for client '{}': {}", 
                    client.getClientId(), e.getMessage(), e);
            }
        }
    }
}
