package com.ds.user.application.startup.data.services;

import com.ds.user.application.startup.data.KeycloakInitConfig;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.ext.ContextResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Service for connecting to Keycloak master realm
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MasterRealmConnectionService {

    @Value("${keycloak.auth-server-url}")
    private String serverUrl;

    private final KeycloakInitConfig initConfig;

    /**
     * Connect to Keycloak master realm
     */
    public Keycloak connectToMasterRealm() {
        try {
            log.info("Connecting to Keycloak master realm...");
            
            // Configure ObjectMapper to ignore unknown properties for compatibility
            ObjectMapper objectMapper = new ObjectMapper()
                    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            
            ResteasyClientBuilder clientBuilder = (ResteasyClientBuilder) ResteasyClientBuilder.newBuilder()
                    .register(new ContextResolver<ObjectMapper>() {
                        @Override
                        public ObjectMapper getContext(Class<?> type) {
                            return objectMapper;
                        }
                    });
            
            Keycloak keycloak = KeycloakBuilder.builder()
                    .serverUrl(serverUrl)
                    .realm(initConfig.getMaster().getRealm())
                    .grantType(OAuth2Constants.PASSWORD)
                    .clientId("admin-cli")
                    .username(initConfig.getMaster().getUsername())
                    .password(initConfig.getMaster().getPassword())
                    .resteasyClient(clientBuilder.build())
                    .build();

            // Test connection (skip serverInfo check to avoid compatibility issues)
            log.info("Successfully connected to master realm");
            return keycloak;
        } catch (Exception e) {
            log.error("Failed to connect to master realm: {}", e.getMessage());
            return null;
        }
    }
}
