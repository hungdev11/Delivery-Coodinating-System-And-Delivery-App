package com.ds.gateway.application.security;

import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * User context holder for authenticated users from Keycloak JWT
 */
@Slf4j
@Data
@Builder
public class UserContext {
    private String userId;
    private String username;
    private String email;
    private Set<String> roles;
    private Set<String> permissions;
    private String realm;
    private String clientId;
    private Jwt jwt;

    /**
     * Get current user context from security context
     */
    public static Optional<UserContext> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated() || !(authentication.getPrincipal() instanceof Jwt)) {
            return Optional.empty();
        }

        Jwt jwt = (Jwt) authentication.getPrincipal();
        
        UserContext userContext = UserContext.builder()
            .userId(extractUserId(jwt))
            .username(extractUsername(jwt))
            .email(extractEmail(jwt))
            .roles(extractRoles(jwt))
            .permissions(extractPermissions(jwt))
            .realm(extractRealm(jwt))
            .clientId(extractClientId(jwt))
            .jwt(jwt)
            .build();

        return Optional.of(userContext);
    }

    /**
     * Check if current user has specific role
     */
    public static boolean hasRole(String role) {
        return getCurrentUser()
            .map(user -> user.getRoles().contains(role))
            .orElse(false);
    }

    /**
     * Check if current user has any of the specified roles
     */
    public static boolean hasAnyRole(String... roles) {
        return getCurrentUser()
            .map(user -> user.getRoles().stream().anyMatch(role -> 
                java.util.Arrays.asList(roles).contains(role)))
            .orElse(false);
    }

    /**
     * Check if current user has all of the specified roles
     */
    public static boolean hasAllRoles(String... roles) {
        return getCurrentUser()
            .map(user -> user.getRoles().containsAll(java.util.Arrays.asList(roles)))
            .orElse(false);
    }

    private static String extractUserId(Jwt jwt) {
        return jwt.getClaimAsString("sub");
    }

    private static String extractUsername(Jwt jwt) {
        return jwt.getClaimAsString("preferred_username");
    }

    private static String extractEmail(Jwt jwt) {
        return jwt.getClaimAsString("email");
    }

    public static Set<String> extractRoles(Jwt jwt) {
        log.debug("🔍 EXTRACTING ROLES from JWT token");
        log.debug("🔍 JWT claims: {}", jwt.getClaims());
        
        try {
            // Try to extract from realm_access.roles
            Object realmAccess = jwt.getClaim("realm_access");
            log.debug("🔍 Realm access claim: {}", realmAccess);
            
            if (realmAccess instanceof java.util.Map) {
                @SuppressWarnings("unchecked")
                java.util.Map<String, Object> realmMap = (java.util.Map<String, Object>) realmAccess;
                Object roles = realmMap.get("roles");
                log.debug("🔍 Realm roles: {}", roles);
                
                if (roles instanceof Collection) {
                    Set<String> roleSet = ((Collection<?>) roles).stream()
                        .map(Object::toString)
                        .collect(Collectors.toSet());
                    log.debug("✅ EXTRACTED ROLES from realm_access: {}", roleSet);
                    return roleSet;
                }
            }
        } catch (Exception e) {
            log.debug("🔍 Failed to extract from realm_access: {}", e.getMessage());
        }

        try {
            // Try to extract from resource_access
            Object resourceAccess = jwt.getClaim("resource_access");
            log.debug("🔍 Resource access claim: {}", resourceAccess);
            
            if (resourceAccess instanceof java.util.Map) {
                @SuppressWarnings("unchecked")
                java.util.Map<String, Object> resourceMap = (java.util.Map<String, Object>) resourceAccess;
                
                // Try multiple client IDs that might have roles
                String[] clientIds = {"backend-client", "frontend-client", "api-gateway"};
                for (String clientId : clientIds) {
                    log.debug("🔍 Checking client: {}", clientId);
                    Object clientRoles = resourceMap.get(clientId);
                    log.debug("🔍 Client {} roles: {}", clientId, clientRoles);
                    
                    if (clientRoles instanceof java.util.Map) {
                        @SuppressWarnings("unchecked")
                        java.util.Map<String, Object> clientMap = (java.util.Map<String, Object>) clientRoles;
                        Object roles = clientMap.get("roles");
                        log.debug("🔍 Client {} roles object: {}", clientId, roles);
                        
                        if (roles instanceof Collection) {
                            Set<String> roleSet = ((Collection<?>) roles).stream()
                                .map(Object::toString)
                                .collect(Collectors.toSet());
                            log.debug("✅ EXTRACTED ROLES from resource_access[{}]: {}", clientId, roleSet);
                            return roleSet;
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.debug("🔍 Failed to extract from resource_access: {}", e.getMessage());
        }

        log.debug("❌ NO ROLES FOUND in JWT token");
        return Collections.emptySet();
    }

    private static Set<String> extractPermissions(Jwt jwt) {
        // Extract permissions if available in JWT
        try {
            Object permissions = jwt.getClaim("permissions");
            if (permissions instanceof Collection) {
                return ((Collection<?>) permissions).stream()
                    .map(Object::toString)
                    .collect(Collectors.toSet());
            }
        } catch (Exception e) {
            // Return empty set if permissions not available
        }
        return Collections.emptySet();
    }

    private static String extractRealm(Jwt jwt) {
        String iss = jwt.getClaimAsString("iss");
        if (iss != null && iss.contains("/realms/")) {
            // Extract realm name from issuer URI
            // Example: http://localhost:8080/realms/delivery-system -> delivery-system
            String[] parts = iss.split("/realms/");
            if (parts.length > 1) {
                return parts[1];
            }
        }
        return iss; // Fallback to full issuer if parsing fails
    }

    private static String extractClientId(Jwt jwt) {
        return jwt.getClaimAsString("azp");
    }
}
