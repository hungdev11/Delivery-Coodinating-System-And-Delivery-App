package com.ds.gateway.application.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.web.filter.CommonsRequestLoggingFilter;
import org.springframework.web.cors.CorsConfigurationSource;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Security configuration for API Gateway
 * - Keycloak OAuth2 Resource Server with JWT validation
 * - Method-level security with annotations
 * - Custom authentication and authorization error handlers
 */
@Slf4j
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(
    prePostEnabled = true,
    securedEnabled = true,
    jsr250Enabled = true
)
public class SecurityConfig {

    private final CorsConfigurationSource corsConfigurationSource;
    private final MultiRealmAuthenticationManagerResolver multiRealmAuthenticationManagerResolver;

    public SecurityConfig(CorsConfigurationSource corsConfigurationSource,
                         MultiRealmAuthenticationManagerResolver multiRealmAuthenticationManagerResolver) {
        this.corsConfigurationSource = corsConfigurationSource;
        this.multiRealmAuthenticationManagerResolver = multiRealmAuthenticationManagerResolver;
    }

    /**
     * Security Filter Chain with annotation-based security
     * - Allow all requests to pass through filter (to reach controller)
     * - Security determined by Method Security annotations
     * - Default requires authentication at class level
     * - Public routes marked with @PublicRoute annotation
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // Allow all requests through to let controller annotations decide
            .authorizeHttpRequests(authz -> authz
                .anyRequest().permitAll()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .authenticationManagerResolver(multiRealmAuthenticationManagerResolver)
                .authenticationEntryPoint(customAuthenticationEntryPoint())
            )
            .exceptionHandling(exceptionHandling -> exceptionHandling
                .accessDeniedHandler(customAccessDeniedHandler())
            );

        return http.build();
    }

    /**
     * Custom authentication entry point for 401 Unauthorized
     */
    @Bean
    public AuthenticationEntryPoint customAuthenticationEntryPoint() {
        return new AuthenticationEntryPoint() {
            @Override
            public void commence(HttpServletRequest request, HttpServletResponse response,
                               AuthenticationException authException) throws IOException, ServletException {
                
                log.warn("Unauthorized access to: {} - {}", request.getRequestURI(), authException.getMessage());
                
                response.setStatus(HttpStatus.UNAUTHORIZED.value());
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.setCharacterEncoding(StandardCharsets.UTF_8.name());
                
                String errorResponse = String.format(
                    "{\"error\":\"Unauthorized\",\"message\":\"%s\",\"path\":\"%s\"}",
                    authException.getMessage(),
                    request.getRequestURI()
                );
                
                response.getWriter().write(errorResponse);
            }
        };
    }

    /**
     * Custom access denied handler for 403 Forbidden
     */
    @Bean
    public AccessDeniedHandler customAccessDeniedHandler() {
        return new AccessDeniedHandler() {
            @Override
            public void handle(HttpServletRequest request, HttpServletResponse response,
                             org.springframework.security.access.AccessDeniedException accessDeniedException) 
                             throws IOException, ServletException {
                
                log.warn("Access denied to: {} - {}", request.getRequestURI(), accessDeniedException.getMessage());
                
                response.setStatus(HttpStatus.FORBIDDEN.value());
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.setCharacterEncoding(StandardCharsets.UTF_8.name());
                
                String errorResponse = String.format(
                    "{\"error\":\"Forbidden\",\"message\":\"%s\",\"path\":\"%s\"}",
                    accessDeniedException.getMessage(),
                    request.getRequestURI()
                );
                
                response.getWriter().write(errorResponse);
            }
        };
    }

    /**
     * Request logging filter for debugging
     */
    @Bean
    public CommonsRequestLoggingFilter requestLoggingFilter() {
        CommonsRequestLoggingFilter filter = new CommonsRequestLoggingFilter();
        filter.setIncludeQueryString(true);
        filter.setIncludePayload(false);
        filter.setIncludeHeaders(false);
        filter.setIncludeClientInfo(false);
        filter.setBeforeMessagePrefix("REQUEST: ");
        filter.setAfterMessagePrefix("RESPONSE: ");
        return filter;
    }

    /**
     * JWT authentication converter to extract roles with ROLE_ prefix
     */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter grantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
        grantedAuthoritiesConverter.setAuthorityPrefix("ROLE_");
        
        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(grantedAuthoritiesConverter);
        return jwtAuthenticationConverter;
    }
}
