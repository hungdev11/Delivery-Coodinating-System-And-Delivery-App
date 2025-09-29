package com.ds.business.v1.services;

import com.ds.common.entities.dto.auth.AuthTokenDto;
import com.ds.common.entities.dto.auth.ExternalUserDto;
import com.ds.common.interfaces.IAuthenticationService;
import com.ds.common.interfaces.IExternalAuthFacade;
import com.ds.common.interfaces.IIdentityProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Facade implementation that delegates to IAuthenticationService and IIdentityProvider
 * This provides a single entry point for business layer to interact with external auth system
 */
@Service
public class ExternalAuthFacade implements IExternalAuthFacade {
    
    @Autowired
    private IAuthenticationService authenticationService;
    
    @Autowired
    private IIdentityProvider identityProvider;
    
    // ========== Authentication Operations ==========
    
    @Override
    public AuthTokenDto login(String username, String password) {
        return authenticationService.login(username, password);
    }
    
    @Override
    public AuthTokenDto refreshToken(String refreshToken) {
        return authenticationService.refreshToken(refreshToken);
    }
    
    @Override
    public void logout(String refreshToken) {
        authenticationService.logout(refreshToken);
    }
    
    @Override
    public boolean verifyToken(String accessToken) {
        return authenticationService.verifyToken(accessToken);
    }
    
    // ========== User Management Operations ==========
    
    @Override
    public String createUser(String username, String email, String password,
                            String firstName, String lastName, List<String> roles) {
        return identityProvider.createUser(username, email, password, firstName, lastName, roles);
    }
    
    @Override
    public ExternalUserDto getUserById(String externalId) {
        return identityProvider.getUserById(externalId);
    }
    
    @Override
    public void updateUser(String externalId, String email, String firstName, String lastName) {
        identityProvider.updateUser(externalId, email, firstName, lastName);
    }
    
    @Override
    public void deleteUser(String externalId) {
        identityProvider.deleteUser(externalId);
    }
    
    @Override
    public void updatePassword(String externalId, String newPassword) {
        identityProvider.updatePassword(externalId, newPassword);
    }
    
    @Override
    public List<String> getUserRoles(String externalId) {
        return identityProvider.getUserRoles(externalId);
    }
    
    @Override
    public void assignRoles(String externalId, List<String> roles) {
        identityProvider.assignRoles(externalId, roles);
    }
    
    @Override
    public void sendEmailVerification(String externalId) {
        identityProvider.sendEmailVerification(externalId);
    }
    
    @Override
    public void sendPasswordResetEmail(String username) {
        identityProvider.sendPasswordResetEmail(username);
    }
}
