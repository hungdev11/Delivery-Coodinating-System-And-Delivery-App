package com.ds.user.common.interfaces;

import com.ds.user.common.entities.dto.auth.AuthTokenDto;

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
     * Authenticate user with username, password, realm and clientId
     * @param username user's username
     * @param password user's password
     * @param realm Keycloak realm name
     * @param clientId Keycloak client ID
     * @return authentication tokens
     */
    AuthTokenDto login(String username, String password, String realm, String clientId);
    
    /**
     * Refresh access token using refresh token
     * @param refreshToken refresh token
     * @return new authentication tokens
     */
    AuthTokenDto refreshToken(String refreshToken);
    
    /**
     * Refresh access token using refresh token, realm and clientId
     * @param refreshToken refresh token
     * @param realm Keycloak realm name
     * @param clientId Keycloak client ID
     * @return new authentication tokens
     */
    AuthTokenDto refreshToken(String refreshToken, String realm, String clientId);
    
    /**
     * Logout user and revoke tokens
     * @param refreshToken refresh token to revoke
     */
    void logout(String refreshToken);
    
    /**
     * Logout user and revoke tokens
     * @param refreshToken refresh token to revoke
     * @param realm Keycloak realm name
     * @param clientId Keycloak client ID
     */
    void logout(String refreshToken, String realm, String clientId);
    
    /**
     * Verify if a token is valid
     * @param accessToken access token to verify
     * @return true if token is valid
     */
    boolean verifyToken(String accessToken);
    
    /**
     * Verify if a token is valid
     * @param accessToken access token to verify
     * @param realm Keycloak realm name
     * @return true if token is valid
     */
    boolean verifyToken(String accessToken, String realm);
}
