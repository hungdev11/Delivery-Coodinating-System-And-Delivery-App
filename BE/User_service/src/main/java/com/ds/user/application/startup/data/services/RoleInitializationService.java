package com.ds.user.application.startup.data.services;

import com.ds.user.application.startup.data.KeycloakInitConfig;
import jakarta.ws.rs.NotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.RoleRepresentation;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service for initializing Keycloak roles (realm and client roles)
 */
@Slf4j
@Service
public class RoleInitializationService {

    /**
     * Create realm roles
     */
    public void createRealmRoles(RealmResource realmResource, List<KeycloakInitConfig.RoleConfig> roleConfigs) {
        if (roleConfigs == null || roleConfigs.isEmpty()) {
            return;
        }

        log.info("Creating realm roles...");
        for (KeycloakInitConfig.RoleConfig roleConfig : roleConfigs) {
            try {
                // Check if role exists
                realmResource.roles().get(roleConfig.getName()).toRepresentation();
                log.info("Realm role '{}' already exists", roleConfig.getName());
            } catch (NotFoundException e) {
                // Create role
                RoleRepresentation roleRepresentation = new RoleRepresentation();
                roleRepresentation.setName(roleConfig.getName());
                roleRepresentation.setDescription(roleConfig.getDescription());
                realmResource.roles().create(roleRepresentation);
                log.info("Realm role '{}' created", roleConfig.getName());
            }
        }
    }

    /**
     * Create roles for a client
     */
    public void createClientRoles(RealmResource realmResource, String clientUuid, List<String> roleNames) {
        if (roleNames == null || roleNames.isEmpty()) {
            return;
        }

        log.info("Creating client roles...");
        for (String roleName : roleNames) {
            try {
                // Check if role exists
                realmResource.clients().get(clientUuid).roles().get(roleName).toRepresentation();
                log.info("Client role '{}' already exists", roleName);
            } catch (NotFoundException e) {
                // Create role
                RoleRepresentation roleRepresentation = new RoleRepresentation();
                roleRepresentation.setName(roleName);
                realmResource.clients().get(clientUuid).roles().create(roleRepresentation);
                log.info("Client role '{}' created", roleName);
            }
        }
    }
}
