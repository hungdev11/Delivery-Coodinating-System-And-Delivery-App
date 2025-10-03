package com.ds.user.business.v1.services;

import com.ds.user.common.entities.dto.auth.ExternalUserDto;
import com.ds.user.common.interfaces.IIdentityProvider;
import jakarta.ws.rs.core.Response;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Keycloak implementation of IIdentityProvider
 */
@Service
public class KeycloakIdentityProvider implements IIdentityProvider {
    
    private static final Logger log = LoggerFactory.getLogger(KeycloakIdentityProvider.class);
    
    @Value("${keycloak.realm}")
    private String realm;
    
    @Value("${keycloak.resource}")
    private String clientId;
    
    @Autowired
    private Keycloak keycloak;
    
    private UsersResource getUsersResource() {
        RealmResource realmResource = keycloak.realm(realm);
        return realmResource.users();
    }
    
    @Override
    public String createUser(String username, String email, String password,
                            String firstName, String lastName, List<String> roles) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username must not be null or empty");
        }
        
        log.debug("[KeycloakIdentity] Creating user: username={}", username);
        
        try {
            UserRepresentation user = new UserRepresentation();
            user.setEnabled(true);
            user.setUsername(username);
            user.setEmail(email);
            user.setFirstName(firstName);
            user.setLastName(lastName);
            user.setEmailVerified(false);
            
            // Set password
            CredentialRepresentation credential = new CredentialRepresentation();
            credential.setType(CredentialRepresentation.PASSWORD);
            credential.setValue(password);
            credential.setTemporary(false);
            user.setCredentials(Collections.singletonList(credential));
            
            UsersResource usersResource = getUsersResource();
            Response response = usersResource.create(user);
            
            if (response.getStatus() == 201) {
                // Get created user
                List<UserRepresentation> users = usersResource.searchByUsername(username, true);
                if (!users.isEmpty()) {
                    UserRepresentation createdUser = users.get(0);
                    String userId = createdUser.getId();
                    
                    // Assign roles
                    if (roles != null && !roles.isEmpty()) {
                        assignRoles(userId, roles);
                    }
                    
                    // Send email verification
                    try {
                        sendEmailVerification(userId);
                        log.info("[KeycloakIdentity] Verification email sent to user: {}", userId);
                    } catch (Exception e) {
                        log.warn("[KeycloakIdentity] Failed to send verification email: {}", e.getMessage());
                    }
                    
                    log.info("[KeycloakIdentity] User created successfully: {}", userId);
                    return userId;
                }
            }
            
            String errorMessage = response.readEntity(String.class);
            log.error("[KeycloakIdentity] Error creating user: {} - {}", response.getStatus(), errorMessage);
            throw new RuntimeException("Error creating user: " + response.getStatus());
            
        } catch (Exception e) {
            log.error("[KeycloakIdentity] Exception while creating user: {}", e.getMessage(), e);
            throw new RuntimeException("Exception while creating user: " + e.getMessage(), e);
        }
    }
    
    @Override
    public ExternalUserDto getUserById(String externalId) {
        if (externalId == null) {
            throw new IllegalArgumentException("External ID must not be null");
        }
        
        log.debug("[KeycloakIdentity] Getting user by ID: {}", externalId);
        
        try {
            UserRepresentation user = getUsersResource().get(externalId).toRepresentation();
            List<String> roles = getUserRoles(externalId);
            
            // Convert Map<String, List<String>> to Map<String, Object>
            Map<String, Object> attributes = user.getAttributes() != null 
                ? new java.util.HashMap<>(user.getAttributes()) 
                : null;
            
            return ExternalUserDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .emailVerified(user.isEmailVerified())
                .enabled(user.isEnabled())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .roles(roles)
                .attributes(attributes)
                .createdTimestamp(user.getCreatedTimestamp())
                .build();
                
        } catch (Exception e) {
            log.error("[KeycloakIdentity] Error getting user by ID: {}", e.getMessage(), e);
            throw new RuntimeException("Error getting user: " + e.getMessage(), e);
        }
    }
    
    @Override
    public void updateUser(String externalId, String email, String firstName, String lastName) {
        if (externalId == null) {
            throw new IllegalArgumentException("External ID must not be null");
        }
        
        log.debug("[KeycloakIdentity] Updating user: {}", externalId);
        
        try {
            UserResource userResource = getUsersResource().get(externalId);
            UserRepresentation user = userResource.toRepresentation();
            
            if (email != null) user.setEmail(email);
            if (firstName != null) user.setFirstName(firstName);
            if (lastName != null) user.setLastName(lastName);
            
            userResource.update(user);
            log.info("[KeycloakIdentity] User updated successfully: {}", externalId);
            
        } catch (Exception e) {
            log.error("[KeycloakIdentity] Error updating user: {}", e.getMessage(), e);
            throw new RuntimeException("Error updating user: " + e.getMessage(), e);
        }
    }
    
    @Override
    public void deleteUser(String externalId) {
        if (externalId == null) {
            throw new IllegalArgumentException("External ID must not be null");
        }
        
        log.debug("[KeycloakIdentity] Deleting user: {}", externalId);
        
        try {
            getUsersResource().delete(externalId);
            log.info("[KeycloakIdentity] User deleted successfully: {}", externalId);
            
        } catch (Exception e) {
            log.error("[KeycloakIdentity] Error deleting user: {}", e.getMessage(), e);
            throw new RuntimeException("Error deleting user: " + e.getMessage(), e);
        }
    }
    
    @Override
    public void updatePassword(String externalId, String newPassword) {
        if (externalId == null || newPassword == null) {
            throw new IllegalArgumentException("External ID and password must not be null");
        }
        
        log.debug("[KeycloakIdentity] Updating password for user: {}", externalId);
        
        try {
            UserResource userResource = getUsersResource().get(externalId);
            
            CredentialRepresentation credential = new CredentialRepresentation();
            credential.setType(CredentialRepresentation.PASSWORD);
            credential.setValue(newPassword);
            credential.setTemporary(false);
            
            userResource.resetPassword(credential);
            log.info("[KeycloakIdentity] Password updated successfully: {}", externalId);
            
        } catch (Exception e) {
            log.error("[KeycloakIdentity] Error updating password: {}", e.getMessage(), e);
            throw new RuntimeException("Error updating password: " + e.getMessage(), e);
        }
    }
    
    @Override
    public List<String> getUserRoles(String externalId) {
        if (externalId == null) {
            throw new IllegalArgumentException("External ID must not be null");
        }
        
        try {
            UserResource userResource = getUsersResource().get(externalId);
            
            // Get client
            var client = keycloak.realm(realm).clients().findByClientId(clientId).stream().findFirst();
            if (client.isEmpty()) {
                log.error("[KeycloakIdentity] Client '{}' not found in realm '{}'", clientId, realm);
                return Collections.emptyList();
            }
            
            String clientUuid = client.get().getId();
            List<RoleRepresentation> roles = userResource.roles().clientLevel(clientUuid).listAll();
            
            return roles.stream()
                .map(RoleRepresentation::getName)
                .collect(Collectors.toList());
                
        } catch (Exception e) {
            log.error("[KeycloakIdentity] Error getting user roles: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }
    
    @Override
    public void assignRoles(String externalId, List<String> roles) {
        if (externalId == null) {
            throw new IllegalArgumentException("External ID must not be null");
        }
        if (roles == null || roles.isEmpty()) {
            return;
        }
        
        log.debug("[KeycloakIdentity] Assigning roles to user {}: {}", externalId, roles);
        
        try {
            UserResource userResource = getUsersResource().get(externalId);
            
            // Get client
            var client = keycloak.realm(realm).clients().findByClientId(clientId).stream().findFirst();
            if (client.isEmpty()) {
                log.error("[KeycloakIdentity] Client '{}' not found in realm '{}'", clientId, realm);
                return;
            }
            
            String clientUuid = client.get().getId();
            var clientRolesResource = keycloak.realm(realm).clients().get(clientUuid).roles();
            
            List<RoleRepresentation> toAssign = new ArrayList<>();
            for (String roleName : roles) {
                try {
                    RoleRepresentation role = clientRolesResource.get(roleName).toRepresentation();
                    toAssign.add(role);
                } catch (Exception e) {
                    log.warn("[KeycloakIdentity] Role '{}' not found, skipping", roleName);
                }
            }
            
            if (!toAssign.isEmpty()) {
                userResource.roles().clientLevel(clientUuid).add(toAssign);
                log.info("[KeycloakIdentity] Roles assigned successfully to user: {}", externalId);
            }
            
        } catch (Exception e) {
            log.error("[KeycloakIdentity] Error assigning roles: {}", e.getMessage(), e);
            throw new RuntimeException("Error assigning roles: " + e.getMessage(), e);
        }
    }
    
    @Override
    public void sendEmailVerification(String externalId) {
        if (externalId == null) {
            throw new IllegalArgumentException("External ID must not be null");
        }
        
        log.debug("[KeycloakIdentity] Sending email verification: {}", externalId);
        
        try {
            getUsersResource().get(externalId).sendVerifyEmail();
            log.info("[KeycloakIdentity] Email verification sent: {}", externalId);
            
        } catch (Exception e) {
            log.error("[KeycloakIdentity] Error sending email verification: {}", e.getMessage(), e);
            throw new RuntimeException("Error sending email verification: " + e.getMessage(), e);
        }
    }
    
    @Override
    public void sendPasswordResetEmail(String username) {
        if (username == null) {
            throw new IllegalArgumentException("Username must not be null");
        }
        
        log.debug("[KeycloakIdentity] Sending password reset email: {}", username);
        
        try {
            UsersResource usersResource = getUsersResource();
            List<UserRepresentation> users = usersResource.searchByUsername(username, true);
            
            if (users.isEmpty()) {
                log.error("[KeycloakIdentity] Username not found: {}", username);
                throw new RuntimeException("Username not found");
            }
            
            UserRepresentation user = users.get(0);
            UserResource userResource = usersResource.get(user.getId());
            
            List<String> actions = new ArrayList<>();
            actions.add("UPDATE_PASSWORD");
            userResource.executeActionsEmail(actions);
            
            log.info("[KeycloakIdentity] Password reset email sent: {}", username);
            
        } catch (Exception e) {
            log.error("[KeycloakIdentity] Error sending password reset email: {}", e.getMessage(), e);
            throw new RuntimeException("Error sending password reset email: " + e.getMessage(), e);
        }
    }
}
