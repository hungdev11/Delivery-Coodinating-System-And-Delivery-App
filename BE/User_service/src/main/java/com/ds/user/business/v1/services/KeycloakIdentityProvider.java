package com.ds.user.business.v1.services;

import com.ds.user.common.entities.dto.auth.ExternalUserDto;
import com.ds.user.common.interfaces.IIdentityProvider;
import jakarta.ws.rs.NotFoundException;
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
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
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
    @Qualifier("keycloakAdmin")
    private Keycloak keycloakAdmin; // Use master realm admin for admin operations
    
    @Autowired(required = false)
    @Qualifier("keycloak")
    private Keycloak keycloak; // Optional: for client-specific operations
    
    // ExecutorService for parallel role fetching
    private final ExecutorService executorService = Executors.newFixedThreadPool(10);
    
    @PostConstruct
    public void logConfiguration() {
        log.info("[KeycloakIdentity] KeycloakIdentityProvider initialized - Realm: '{}', Client: '{}'", realm, clientId);
    }
    
    /**
     * Get users resource using master realm admin connection
     * This ensures we have admin privileges for all operations
     */
    private UsersResource getUsersResource() {
        // Use master realm admin to access the target realm
        log.debug("[KeycloakIdentity] Accessing realm: {}", realm);
        RealmResource realmResource = keycloakAdmin.realm(realm);
        return realmResource.users();
    }
    
    /**
     * Get realm resource using master realm admin connection
     */
    private RealmResource getRealmResource() {
        log.debug("[KeycloakIdentity] Accessing realm: {}", realm);
        return keycloakAdmin.realm(realm);
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
        
        log.info("[KeycloakIdentity] Getting roles for user externalId={} in realm={}", externalId, realm);
        
        try {
            // First verify the user exists
            UserResource userResource = getUsersResource().get(externalId);
            try {
                UserRepresentation user = userResource.toRepresentation();
                log.info("[KeycloakIdentity] Found user: {} (username: {}) in realm: {}", 
                    externalId, user.getUsername(), realm);
            } catch (NotFoundException e) {
                log.warn("[KeycloakIdentity] User not found in Keycloak realm '{}': externalId={}. " +
                    "This may indicate the user was deleted from Keycloak or exists in a different realm.", realm, externalId);
                return Collections.emptyList();
            }
            
            Set<String> roles = new LinkedHashSet<>();

            // Realm-level roles
            try {
                List<RoleRepresentation> realmRoles = userResource.roles().realmLevel().listAll();
                for (RoleRepresentation role : realmRoles) {
                    roles.add(role.getName());
                }
                log.debug("[KeycloakIdentity] Found {} realm-level roles for user {}", roles.size(), externalId);
            } catch (NotFoundException e) {
                log.warn("[KeycloakIdentity] Cannot access realm-level roles for user {}: {}", externalId, e.getMessage());
            } catch (Exception e) {
                log.warn("[KeycloakIdentity] Error getting realm-level roles for user {}: {}", externalId, e.getMessage());
            }

            // Client-level roles - use master realm admin to access clients
            try {
                var client = getRealmResource().clients().findByClientId(clientId).stream().findFirst();
                if (client.isEmpty()) {
                    log.warn("[KeycloakIdentity] Client '{}' not found in realm '{}'", clientId, realm);
                } else {
                    String clientUuid = client.get().getId();
                    List<RoleRepresentation> clientRoles = userResource.roles().clientLevel(clientUuid).listAll();
                    int clientRoleCount = clientRoles.size();
                    for (RoleRepresentation role : clientRoles) {
                        roles.add(role.getName());
                    }
                    log.debug("[KeycloakIdentity] Found {} client-level roles for user {} (client: {})", 
                        clientRoleCount, externalId, clientId);
                }
            } catch (NotFoundException e) {
                log.warn("[KeycloakIdentity] Cannot access client-level roles for user {} (client: {}): {}", 
                    externalId, clientId, e.getMessage());
            } catch (Exception e) {
                log.warn("[KeycloakIdentity] Error getting client-level roles for user {} (client: {}): {}", 
                    externalId, clientId, e.getMessage());
            }

            log.debug("[KeycloakIdentity] Total roles found for user {}: {}", externalId, roles.size());
            return new ArrayList<>(roles);
                
        } catch (NotFoundException e) {
            log.warn("[KeycloakIdentity] User not found in Keycloak (404): externalId={}", externalId);
            return Collections.emptyList();
        } catch (Exception e) {
            log.error("[KeycloakIdentity] Unexpected error getting user roles for externalId={}: {}", 
                externalId, e.getMessage(), e);
            return Collections.emptyList();
        }
    }
    
    @Override
    public Map<String, List<String>> batchGetUserRoles(List<String> externalIds) {
        if (externalIds == null || externalIds.isEmpty()) {
            return Collections.emptyMap();
        }
        
        log.info("[KeycloakIdentity] Batch getting roles for {} users in realm={}", externalIds.size(), realm);
        long startTime = System.currentTimeMillis();
        
        try {
            // Fetch roles in parallel using ExecutorService
            List<CompletableFuture<Map.Entry<String, List<String>>>> futures = externalIds.stream()
                    .map(userId -> CompletableFuture.<Map.Entry<String, List<String>>>supplyAsync(() -> {
                        try {
                            List<String> roles = getUserRoles(userId);
                            return Map.entry(userId, roles);
                        } catch (Exception e) {
                            log.warn("[KeycloakIdentity] Failed to get roles for user {}: {}", userId, e.getMessage());
                            return Map.entry(userId, Collections.<String>emptyList());
                        }
                    }, executorService))
                    .collect(Collectors.toList());
            
            // Wait for all futures to complete
            CompletableFuture<Void> allFutures = CompletableFuture.allOf(
                    futures.toArray(new CompletableFuture[0]));
            
            // Collect results
            Map<String, List<String>> rolesMap = new HashMap<>();
            try {
                allFutures.get(30, TimeUnit.SECONDS); // Timeout after 30 seconds
                for (CompletableFuture<Map.Entry<String, List<String>>> future : futures) {
                    Map.Entry<String, List<String>> entry = future.get();
                    rolesMap.put(entry.getKey(), entry.getValue());
                }
            } catch (Exception e) {
                log.error("[KeycloakIdentity] Error waiting for batch role fetch: {}", e.getMessage(), e);
                // Collect completed results
                for (CompletableFuture<Map.Entry<String, List<String>>> future : futures) {
                    if (future.isDone()) {
                        try {
                            Map.Entry<String, List<String>> entry = future.get();
                            rolesMap.put(entry.getKey(), entry.getValue());
                        } catch (Exception ex) {
                            log.warn("[KeycloakIdentity] Failed to get result from future: {}", ex.getMessage());
                        }
                    }
                }
            }
            
            long duration = System.currentTimeMillis() - startTime;
            log.info("[KeycloakIdentity] Batch fetched roles for {} users in {}ms (avg: {}ms/user)", 
                    rolesMap.size(), duration, rolesMap.size() > 0 ? duration / rolesMap.size() : 0);
            
            return rolesMap;
            
        } catch (Exception e) {
            log.error("[KeycloakIdentity] Unexpected error in batchGetUserRoles: {}", e.getMessage(), e);
            return Collections.emptyMap();
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
            
            // Get client using master realm admin
            var client = getRealmResource().clients().findByClientId(clientId).stream().findFirst();
            if (client.isEmpty()) {
                log.error("[KeycloakIdentity] Client '{}' not found in realm '{}'", clientId, realm);
                return;
            }
            
            String clientUuid = client.get().getId();
            var clientRolesResource = getRealmResource().clients().get(clientUuid).roles();
            
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
