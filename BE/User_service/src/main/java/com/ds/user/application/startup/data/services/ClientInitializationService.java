package com.ds.user.application.startup.data.services;

import com.ds.user.application.startup.data.KeycloakInitConfig;
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
                
                // Log secret info for existing client
                if (!clientConfig.isPublicClient()) {
                    String existingSecret = existingClient.get().getSecret();
                    if (existingSecret != null && !existingSecret.isEmpty()) {
                        log.info("Client '{}' secret: {}", clientConfig.getClientId(), maskSecret(existingSecret));
                        log.warn("Client '{}' - If you need the full secret, check Keycloak Admin Console", clientConfig.getClientId());
                    } else {
                        log.warn("Client '{}' - Cannot retrieve secret (may need regeneration in Keycloak Admin Console)", clientConfig.getClientId());
                    }
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

            if (!clientConfig.isPublicClient() && clientConfig.getSecret() != null) {
                clientRepresentation.setSecret(clientConfig.getSecret());
                log.info("Client '{}' will be created with configured secret", clientConfig.getClientId());
            }

            Response response = realmResource.clients().create(clientRepresentation);
            
            if (response.getStatus() == 201) {
                log.info("Client '{}' created successfully", clientConfig.getClientId());
                
                // Log secret information for confidential clients
                if (!clientConfig.isPublicClient() && clientConfig.getSecret() != null) {
                    log.info("✓ Client '{}' secret configured: {}", clientConfig.getClientId(), maskSecret(clientConfig.getSecret()));
                    log.info("✓ Make sure KEYCLOAK_CLIENT_SECRET in env.local matches this value");
                }
                
                // Get created client ID
                String clientId = extractIdFromLocationHeader(response);
                if (clientId != null) {
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

    /**
     * Mask secret for logging (show first 4 and last 4 characters)
     */
    private String maskSecret(String secret) {
        if (secret == null || secret.length() <= 8) {
            return "****";
        }
        return secret.substring(0, 4) + "****" + secret.substring(secret.length() - 4);
    }
}
