package com.ds.common.interfaces;

import com.ds.common.entities.dto.auth.ExternalUserDto;

import java.util.List;

/**
 * Interface for identity provider operations (user management)
 * Can be implemented by Keycloak, Auth0, AWS Cognito, etc.
 */
public interface IIdentityProvider {
    
    /**
     * Create a new user in identity provider
     * @param username username
     * @param email email
     * @param password password
     * @param firstName first name
     * @param lastName last name
     * @param roles roles to assign
     * @return external user ID
     */
    String createUser(String username, String email, String password, 
                     String firstName, String lastName, List<String> roles);
    
    /**
     * Get user by external ID
     * @param externalId external user ID
     * @return external user info
     */
    ExternalUserDto getUserById(String externalId);
    
    /**
     * Update user information
     * @param externalId external user ID
     * @param email new email
     * @param firstName new first name
     * @param lastName new last name
     */
    void updateUser(String externalId, String email, String firstName, String lastName);
    
    /**
     * Delete user from identity provider
     * @param externalId external user ID
     */
    void deleteUser(String externalId);
    
    /**
     * Update user password
     * @param externalId external user ID
     * @param newPassword new password
     */
    void updatePassword(String externalId, String newPassword);
    
    /**
     * Get user roles
     * @param externalId external user ID
     * @return list of role names
     */
    List<String> getUserRoles(String externalId);
    
    /**
     * Assign roles to user
     * @param externalId external user ID
     * @param roles roles to assign
     */
    void assignRoles(String externalId, List<String> roles);
    
    /**
     * Send email verification
     * @param externalId external user ID
     */
    void sendEmailVerification(String externalId);
    
    /**
     * Send password reset email
     * @param username username
     */
    void sendPasswordResetEmail(String username);
}
