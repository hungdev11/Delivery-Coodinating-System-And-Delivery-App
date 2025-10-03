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
     * Create client with its roles
     */
    public void createClient(RealmResource realmResource, KeycloakInitConfig.ClientConfig clientConfig) {
        log.info("Creating client: {}", clientConfig.getClientId());

        try {
            // Check if client exists
            Optional<ClientRepresentation> existingClient = realmResource.clients()
                    .findByClientId(clientConfig.getClientId())
                    .stream()
                    .findFirst();

            if (existingClient.isPresent()) {
                log.info("Client '{}' already exists", clientConfig.getClientId());
                writeClientSettings(realmResource, existingClient.get());
                // Log secret info for existing client
                if (!clientConfig.isPublicClient()) {
                    log.info("Client '{}' is confidential; secret should be managed in Keycloak and Settings Service (not in config)", clientConfig.getClientId());
                }
                
                // Create client roles
                roleInitializationService.createClientRoles(realmResource, existingClient.get().getId(), clientConfig.getRoles());
                return;
            }

            // Create new client
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

            Response response = realmResource.clients().create(clientRepresentation);
            
            if (response.getStatus() == 201) {
                log.info("Client '{}' created successfully", clientConfig.getClientId());
                
                // Get created client ID
                String clientId = extractIdFromLocationHeader(response);
                if (clientId != null) {
                    // Create/update settings for the client
                    ClientRepresentation created = realmResource.clients().get(clientId).toRepresentation();
                    writeClientSettings(realmResource, created);
                    roleInitializationService.createClientRoles(realmResource, clientId, clientConfig.getRoles());
                }
            } else {
                log.error("Failed to create client '{}'. Status: {}", clientConfig.getClientId(), response.getStatus());
            }
            response.close();

        } catch (Exception e) {
            log.error("Error creating client '{}': {}", clientConfig.getClientId(), e.getMessage(), e);
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
        
        // Get and save client secret for confidential clients
        if (Boolean.FALSE.equals(client.isPublicClient())) {
            try {
                // Get the actual client secret from Keycloak
                org.keycloak.representations.idm.CredentialRepresentation secret = 
                    realmResource.clients().get(client.getId()).getSecret();
                
                if (secret != null && secret.getValue() != null) {
                    settingsWriterService.createSetting(
                            "KEYCLOAK_CLIENT_" + clientKey + "_SECRET",
                            "keycloak",
                            "Client secret for " + client.getName(),
                            "STRING",
                            secret.getValue(),
                            "SYSTEM",
                            true,
                            "PASSWORD"
                    );
                    log.info("  ✓ Saved client secret for '{}' to Settings Service", client.getClientId());
                } else {
                    log.warn("  ⚠ Client '{}' is confidential but no secret was found", client.getClientId());
                }
            } catch (Exception e) {
                log.error("  ✗ Failed to retrieve/save secret for client '{}': {}", 
                    client.getClientId(), e.getMessage(), e);
            }
        }
    }
}
