package com.ds.gateway.application.security;

import com.ds.gateway.annotations.AuthRequired;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Aspect to handle @AuthRequired annotation with role-based authorization
 */
@Slf4j
@Aspect
@Component
public class AuthRequiredAspect {

    @Around("@annotation(authRequired)")
    public Object checkAuthRequired(ProceedingJoinPoint joinPoint, AuthRequired authRequired) throws Throwable {
        log.debug("🔐 AUTH REQUIRED CHECK - Method: {}", joinPoint.getSignature().getName());
        
        // Check authentication
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            log.warn("❌ AUTH REQUIRED CHECK - No authentication found");
            throw new org.springframework.security.authentication.AuthenticationCredentialsNotFoundException("Authentication required");
        }
        
        // If no roles specified, just check authentication
        String[] requiredRoles = authRequired.value();
        if (requiredRoles.length == 0) {
            log.debug("✅ AUTH REQUIRED CHECK - Authentication only, proceeding");
            return joinPoint.proceed();
        }
        
        // Check roles
        if (authentication.getPrincipal() instanceof Jwt) {
            Jwt jwt = (Jwt) authentication.getPrincipal();
            Set<String> userRoles = UserContext.extractRoles(jwt);
            
            log.debug("🔐 AUTH REQUIRED CHECK - User roles: {}, Required roles: {}", userRoles, Set.of(requiredRoles));
            
            // Check if user has any of the required roles
            for (String requiredRole : requiredRoles) {
                if (userRoles.contains(requiredRole)) {
                    log.debug("✅ AUTH REQUIRED CHECK - Role {} found, proceeding", requiredRole);
                    return joinPoint.proceed();
                }
            }
            
            log.warn("❌ AUTH REQUIRED CHECK - User lacks required roles. User roles: {}, Required: {}", userRoles, Set.of(requiredRoles));
            throw new org.springframework.security.access.AccessDeniedException("Access denied: Required role not found");
        }
        
        log.warn("❌ AUTH REQUIRED CHECK - Invalid authentication principal type");
        throw new org.springframework.security.access.AccessDeniedException("Access denied: Invalid authentication");
    }
}
