package com.ds.user.application.configs;

import com.ds.user.application.startup.data.KeycloakInitConfig;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.ext.ContextResolver;
import lombok.RequiredArgsConstructor;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Keycloak Admin Client Configuration
 * 
 * For admin operations (like getting user roles), use master realm admin account.
 * For client-specific operations, fetch client secrets from Settings Service.
 */
@Configuration
@RequiredArgsConstructor
public class KeycloakAdminConfig {

    @Value("${keycloak.auth-server-url}")
    private String serverUrl;

    @Value("${keycloak.realm}")
    private String realm;

    @Value("${keycloak.resource}")
    private String clientId;

    @Value("${keycloak.credentials.username}")
    private String username;

    @Value("${keycloak.credentials.password}")
    private String password;

    @Value("${keycloak.admin-client.connection-timeout:10000}")
    private int connectionTimeout;

    @Value("${keycloak.admin-client.socket-timeout:30000}")
    private int socketTimeout;

    private final KeycloakInitConfig initConfig;

    /**
     * Master realm admin client for admin operations (user management, role queries, etc.)
     * Uses master realm admin credentials configured in keycloak.init.master
     */
    @Bean(name = "keycloakAdmin")
    public Keycloak keycloakAdmin() {
        // Configure ObjectMapper to ignore unknown properties for compatibility
        ObjectMapper objectMapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        
        // Configure ResteasyClient with timeout settings for better performance
        ResteasyClientBuilder clientBuilder = (ResteasyClientBuilder) ResteasyClientBuilder.newBuilder()
                .register(new ContextResolver<ObjectMapper>() {
                    @Override
                    public ObjectMapper getContext(Class<?> type) {
                        return objectMapper;
                    }
                })
                .connectTimeout(connectionTimeout, java.util.concurrent.TimeUnit.MILLISECONDS)
                .readTimeout(socketTimeout, java.util.concurrent.TimeUnit.MILLISECONDS);

        // Use master realm admin credentials for admin operations
        return KeycloakBuilder.builder()
                .serverUrl(serverUrl)
                .realm(initConfig.getMaster().getRealm())
                .grantType(OAuth2Constants.PASSWORD)
                .clientId("admin-cli") // Standard Keycloak admin client
                .username(initConfig.getMaster().getUsername())
                .password(initConfig.getMaster().getPassword())
                .resteasyClient(clientBuilder.build())
                .build();
    }

    /**
     * Client-specific Keycloak connection (for client operations if needed)
     * This can be used with client secrets fetched from Settings Service
     */
    @Bean(name = "keycloak")
    public Keycloak keycloak() {
        // Configure ObjectMapper to ignore unknown properties for compatibility
        ObjectMapper objectMapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        
        // Configure ResteasyClient with timeout settings for better performance
        ResteasyClientBuilder clientBuilder = (ResteasyClientBuilder) ResteasyClientBuilder.newBuilder()
                .register(new ContextResolver<ObjectMapper>() {
                    @Override
                    public ObjectMapper getContext(Class<?> type) {
                        return objectMapper;
                    }
                })
                .connectTimeout(connectionTimeout, java.util.concurrent.TimeUnit.MILLISECONDS)
                .readTimeout(socketTimeout, java.util.concurrent.TimeUnit.MILLISECONDS);

        return KeycloakBuilder.builder()
                .serverUrl(serverUrl)
                .realm(realm)
                .grantType(OAuth2Constants.PASSWORD)
                .clientId(clientId)
                .username(username)
                .password(password)
                .resteasyClient(clientBuilder.build())
                .build();
    }
}
