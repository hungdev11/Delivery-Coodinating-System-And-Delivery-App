package com.ds.common.interfaces;

import com.ds.common.entities.dto.auth.AuthTokenDto;

/**
 * Interface for authentication operations
 * Can be implemented by Keycloak, Auth0, AWS Cognito, etc.
 */
public interface IAuthenticationService {
    
    /**
     * Authenticate user with username and password
     * @param username user's username
     * @param password user's password
     * @return authentication tokens
     */
    AuthTokenDto login(String username, String password);
    
    /**
     * Refresh access token using refresh token
     * @param refreshToken refresh token
     * @return new authentication tokens
     */
    AuthTokenDto refreshToken(String refreshToken);
    
    /**
     * Logout user and revoke tokens
     * @param refreshToken refresh token to revoke
     */
    void logout(String refreshToken);
    
    /**
     * Verify if a token is valid
     * @param accessToken access token to verify
     * @return true if token is valid
     */
    boolean verifyToken(String accessToken);
}
