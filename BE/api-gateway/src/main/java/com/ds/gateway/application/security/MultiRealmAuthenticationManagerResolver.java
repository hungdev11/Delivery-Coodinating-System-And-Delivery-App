package com.ds.gateway.application.security;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationManagerResolver;
import org.springframework.security.oauth2.server.resource.authentication.JwtIssuerAuthenticationManagerResolver;
import org.springframework.stereotype.Component;

/**
 * Multi-realm Authentication Manager Resolver
 * Supports JWT validation for multiple Keycloak realms (backend and client)
 */
@Slf4j
@Component
public class MultiRealmAuthenticationManagerResolver implements AuthenticationManagerResolver<HttpServletRequest> {

    @Value("${keycloak.backend.issuer-uri}")
    private String backendIssuerUri;

    @Value("${keycloak.client.issuer-uri}")
    private String clientIssuerUri;

    @Value("${keycloak.backend.jwk-set-uri}")
    private String backendJwkSetUri;

    @Value("${keycloak.client.jwk-set-uri}")
    private String clientJwkSetUri;

    private JwtIssuerAuthenticationManagerResolver jwtIssuerAuthenticationManagerResolver;

    /**
     * Resolve the appropriate authentication manager based on JWT issuer
     */
    @Override
    public AuthenticationManager resolve(HttpServletRequest request) {
        if (jwtIssuerAuthenticationManagerResolver == null) {
            jwtIssuerAuthenticationManagerResolver = createIssuerResolver();
        }
        return jwtIssuerAuthenticationManagerResolver.resolve(request);
    }

    /**
     * Create issuer-based authentication manager resolver
     */
    private JwtIssuerAuthenticationManagerResolver createIssuerResolver() {
        log.info("Configuring multi-realm JWT authentication");
        log.info("Backend realm issuer: {}", backendIssuerUri);
        log.info("Client realm issuer: {}", clientIssuerUri);

        // Use JwtIssuerAuthenticationManagerResolver with trusted issuers
        return JwtIssuerAuthenticationManagerResolver.fromTrustedIssuers(backendIssuerUri, clientIssuerUri);
    }

}
