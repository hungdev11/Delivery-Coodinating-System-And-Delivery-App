package ptithcm.graduation.apigateway.configs;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.jwt.*;

/**
 * Keycloak configuration for OAuth2 Resource Server
 */
@Slf4j
@Configuration
public class KeycloakConfig {

    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
    private String issuerUri;

    @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}")
    private String jwkSetUri;

    /**
     * JWT decoder with custom validation
     */
    @Bean
    public JwtDecoder jwtDecoder() {
        NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
        
        // Add custom validators
        OAuth2TokenValidator<Jwt> withIssuer = JwtValidators.createDefaultWithIssuer(issuerUri);
        OAuth2TokenValidator<Jwt> withTimestamp = new JwtTimestampValidator();
        
        jwtDecoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(withIssuer, withTimestamp));
        
        log.info("Configured JWT decoder with issuer: {} and JWK set URI: {}", issuerUri, jwkSetUri);
        return jwtDecoder;
    }

    /**
     * Custom JWT timestamp validator
     */
    private static class JwtTimestampValidator implements OAuth2TokenValidator<Jwt> {
        @Override
        public OAuth2TokenValidatorResult validate(Jwt jwt) {
            try {
                if (jwt.getExpiresAt() != null && jwt.getExpiresAt().isBefore(java.time.Instant.now())) {
                                    return OAuth2TokenValidatorResult.failure(
                    new OAuth2Error("jwt_expired")
                );
                }
                return OAuth2TokenValidatorResult.success();
            } catch (Exception e) {
                return OAuth2TokenValidatorResult.failure(
                    new OAuth2Error("jwt_validation_error")
                );
            }
        }
    }
}
