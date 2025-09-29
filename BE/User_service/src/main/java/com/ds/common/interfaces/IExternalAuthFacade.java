package com.ds.common.interfaces;

import com.ds.common.entities.dto.auth.AuthTokenDto;
import com.ds.common.entities.dto.auth.ExternalUserDto;

import java.util.List;

/**
 * Facade interface combining Authentication and Identity Provider operations
 * Provides a unified interface for business layer to interact with external auth system
 */
public interface IExternalAuthFacade {
    
    // ========== Authentication Operations ==========
    
    /**
     * Authenticate user and return tokens
     */
    AuthTokenDto login(String username, String password);
    
    /**
     * Refresh access token
     */
    AuthTokenDto refreshToken(String refreshToken);
    
    /**
     * Logout and revoke tokens
     */
    void logout(String refreshToken);
    
    /**
     * Verify if token is valid
     */
    boolean verifyToken(String accessToken);
    
    // ========== User Management Operations ==========
    
    /**
     * Create user with roles
     */
    String createUser(String username, String email, String password,
                     String firstName, String lastName, List<String> roles);
    
    /**
     * Get user by external ID
     */
    ExternalUserDto getUserById(String externalId);
    
    /**
     * Update user information
     */
    void updateUser(String externalId, String email, String firstName, String lastName);
    
    /**
     * Delete user
     */
    void deleteUser(String externalId);
    
    /**
     * Update password
     */
    void updatePassword(String externalId, String newPassword);
    
    /**
     * Get user roles
     */
    List<String> getUserRoles(String externalId);
    
    /**
     * Assign roles to user
     */
    void assignRoles(String externalId, List<String> roles);
    
    /**
     * Send email verification
     */
    void sendEmailVerification(String externalId);
    
    /**
     * Send password reset email
     */
    void sendPasswordResetEmail(String username);
}
