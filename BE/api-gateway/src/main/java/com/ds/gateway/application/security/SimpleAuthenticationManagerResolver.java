package com.ds.gateway.application.security;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationManagerResolver;
import org.springframework.security.oauth2.server.resource.authentication.JwtIssuerAuthenticationManagerResolver;
import org.springframework.stereotype.Component;


/**
 * Simple Authentication Manager Resolver
 * Resolves authentication manager based on JWT issuer
 */
@Slf4j
@Component
public class SimpleAuthenticationManagerResolver implements AuthenticationManagerResolver<HttpServletRequest> {

    @Value("${keycloak.backend.issuer-uri}")
    private String backendIssuerUri;

    @Value("${keycloak.client.issuer-uri}")
    private String clientIssuerUri;


    private JwtIssuerAuthenticationManagerResolver jwtIssuerResolver;
    private boolean initialized = false;

    /**
     * Resolve authentication manager for the request
     */
    @Override
    public AuthenticationManager resolve(HttpServletRequest request) {
        if (!initialized) {
            initializeJwtIssuerResolver();
        }
        return jwtIssuerResolver.resolve(request);
    }

    /**
     * Initialize JWT issuer resolver based on issuer configuration
     */
    private void initializeJwtIssuerResolver() {
        log.info("Initializing JWT issuer resolver");
        log.info("Backend issuer: {}", backendIssuerUri);
        log.info("Client issuer: {}", clientIssuerUri);

        // Use JwtIssuerAuthenticationManagerResolver which handles Keycloak JWT validation automatically
        if (backendIssuerUri.equals(clientIssuerUri)) {
            // Same issuer - use single issuer
            log.info("Using single issuer configuration");
            jwtIssuerResolver = JwtIssuerAuthenticationManagerResolver.fromTrustedIssuers(backendIssuerUri);
        } else {
            // Different issuers - use multi-issuer
            log.info("Using multi-issuer configuration");
            jwtIssuerResolver = JwtIssuerAuthenticationManagerResolver.fromTrustedIssuers(backendIssuerUri, clientIssuerUri);
        }
        
        initialized = true;
        log.info("JWT issuer resolver initialized successfully");
    }
}
