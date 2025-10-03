package com.ds.user.application.configs;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.ext.ContextResolver;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
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

    @Bean
    public Keycloak keycloak() {
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
