package com.ds.gateway.annotations;

import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark class/method requiring authentication
 * Default requires authenticated user
 * 
 * Usage:
 * @AuthRequired                    // requires authentication only
 * @AuthRequired({"ADMIN"})         // requires ADMIN role
 * @AuthRequired({"ADMIN", "USER"}) // requires ADMIN or USER role
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface AuthRequired {
    
    /**
     * Required roles. If empty, only authentication is required.
     * If specified, user must have at least one of these roles.
     */
    String[] value() default {};
}
