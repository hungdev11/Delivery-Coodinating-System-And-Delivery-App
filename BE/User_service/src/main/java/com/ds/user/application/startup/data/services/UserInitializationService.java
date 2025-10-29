package com.ds.user.application.startup.data.services;

import com.ds.user.common.entities.base.User;
import com.ds.user.app_context.repositories.UserRepository;
import com.ds.user.application.startup.data.KeycloakInitConfig;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service for initializing Keycloak users with credentials and roles
 * Also syncs users to User Service database
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserInitializationService {
    
    private final UserRepository userRepository;

    /**
     * Create users with credentials and roles
     */
    public void createUsers(RealmResource realmResource, List<KeycloakInitConfig.UserConfig> userConfigs) {
        if (userConfigs == null || userConfigs.isEmpty()) {
            return;
        }

        log.info("Creating users...");
        for (KeycloakInitConfig.UserConfig userConfig : userConfigs) {
            try {
                // Check if user exists in Keycloak
                List<UserRepresentation> existingUsers = realmResource.users()
                        .search(userConfig.getUsername(), true);
                
                if (!existingUsers.isEmpty()) {
                    log.info("✓ Keycloak: User '{}' already exists", userConfig.getUsername());
                    // Sync to database if not already synced
                    String keycloakUserId = existingUsers.get(0).getId();
                    syncUserToDatabase(keycloakUserId, userConfig);
                    continue;
                }

                // Create user
                UserRepresentation userRepresentation = new UserRepresentation();
                userRepresentation.setUsername(userConfig.getUsername());
                userRepresentation.setEmail(userConfig.getEmail());
                userRepresentation.setFirstName(userConfig.getFirstName());
                userRepresentation.setLastName(userConfig.getLastName());
                userRepresentation.setEnabled(userConfig.isEnabled());
                userRepresentation.setEmailVerified(userConfig.isEmailVerified());

                Response response = realmResource.users().create(userRepresentation);
                
                if (response.getStatus() == 201) {
                    log.info("✓ Keycloak: User '{}' created successfully", userConfig.getUsername());
                    
                    String keycloakUserId = extractIdFromLocationHeader(response);
                    if (keycloakUserId != null) {
                        // Set password
                        setUserPassword(realmResource, keycloakUserId, userConfig.getPassword());
                        
                        // Assign realm roles
                        assignRealmRoles(realmResource, keycloakUserId, userConfig.getRealmRoles());
                        
                        // Assign client roles
                        assignClientRoles(realmResource, keycloakUserId, userConfig.getClientRoles());
                        
                        // Sync to User Service database
                        syncUserToDatabase(keycloakUserId, userConfig);
                    }
                } else {
                    log.error("Failed to create user '{}'. Status: {}", userConfig.getUsername(), response.getStatus());
                }
                response.close();

            } catch (Exception e) {
                log.error("Error creating user '{}': {}", userConfig.getUsername(), e.getMessage(), e);
            }
        }
    }

    /**
     * Set user password
     */
    private void setUserPassword(RealmResource realmResource, String userId, String password) {
        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(password);
        credential.setTemporary(false);
        realmResource.users().get(userId).resetPassword(credential);
        log.info("Password set for user");
    }

    /**
     * Assign realm roles to user
     */
    private void assignRealmRoles(RealmResource realmResource, String userId, List<String> roleNames) {
        if (roleNames == null || roleNames.isEmpty()) {
            return;
        }

        List<RoleRepresentation> roles = new ArrayList<>();
        for (String roleName : roleNames) {
            try {
                RoleRepresentation role = realmResource.roles().get(roleName).toRepresentation();
                roles.add(role);
            } catch (NotFoundException e) {
                log.warn("Realm role '{}' not found", roleName);
            }
        }

        if (!roles.isEmpty()) {
            realmResource.users().get(userId).roles().realmLevel().add(roles);
            log.info("Assigned {} realm roles to user", roles.size());
        }
    }

    /**
     * Assign client roles to user
     */
    private void assignClientRoles(RealmResource realmResource, String userId, Map<String, List<String>> clientRoles) {
        if (clientRoles == null || clientRoles.isEmpty()) {
            return;
        }

        for (Map.Entry<String, List<String>> entry : clientRoles.entrySet()) {
            String clientId = entry.getKey();
            List<String> roleNames = entry.getValue();

            try {
                Optional<ClientRepresentation> client = realmResource.clients()
                        .findByClientId(clientId)
                        .stream()
                        .findFirst();

                if (client.isEmpty()) {
                    log.warn("Client '{}' not found", clientId);
                    continue;
                }

                List<RoleRepresentation> roles = new ArrayList<>();
                for (String roleName : roleNames) {
                    try {
                        RoleRepresentation role = realmResource.clients()
                                .get(client.get().getId())
                                .roles()
                                .get(roleName)
                                .toRepresentation();
                        roles.add(role);
                    } catch (NotFoundException e) {
                        log.warn("Client role '{}' not found for client '{}'", roleName, clientId);
                    }
                }

                if (!roles.isEmpty()) {
                    realmResource.users().get(userId).roles()
                            .clientLevel(client.get().getId())
                            .add(roles);
                    log.info("Assigned {} client roles from '{}' to user", roles.size(), clientId);
                }

            } catch (Exception e) {
                log.error("Error assigning client roles from '{}': {}", clientId, e.getMessage(), e);
            }
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
     * Sync user from Keycloak to User Service database
     */
    private void syncUserToDatabase(String keycloakUserId, KeycloakInitConfig.UserConfig userConfig) {
        try {
            // Check if user already exists in database
            Optional<User> existingUser = userRepository.findByKeycloakId(keycloakUserId);
            if (existingUser.isPresent()) {
                log.info("✓ Database: User '{}' already synced (keycloakId: {})", 
                    userConfig.getUsername(), keycloakUserId);
                return;
            }
            
            // Create user in database
            User user = User.builder()
                    .keycloakId(keycloakUserId)
                    .username(userConfig.getUsername())
                    .email(userConfig.getEmail())
                    .firstName(userConfig.getFirstName())
                    .lastName(userConfig.getLastName())
                    .status(userConfig.isEnabled() ? User.UserStatus.ACTIVE : User.UserStatus.PENDING)
                    .build();
            
            userRepository.save(user);
            log.info("✓ Database: User '{}' synced successfully (keycloakId: {})", 
                userConfig.getUsername(), keycloakUserId);
                
        } catch (Exception e) {
            log.error("✗ Database: Failed to sync user '{}': {}", 
                userConfig.getUsername(), e.getMessage(), e);
        }
    }
}
