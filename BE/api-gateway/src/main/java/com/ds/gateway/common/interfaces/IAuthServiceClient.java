package com.ds.gateway.common.interfaces;

import com.ds.gateway.common.entities.dto.auth.*;
import com.ds.gateway.common.entities.dto.user.UserDto;

import java.util.concurrent.CompletableFuture;

/**
 * Interface for Auth Service REST client
 * Defines contract for calling authentication endpoints
 */
public interface IAuthServiceClient {
    
    /**
     * Login user
     */
    CompletableFuture<LoginResponseDto> login(LoginRequestDto request);
    
    /**
     * Refresh access token
     */
    CompletableFuture<RefreshTokenResponseDto> refreshToken(RefreshTokenRequestDto request);
    
    /**
     * Logout user
     */
    CompletableFuture<Void> logout(String refreshToken);
    
    /**
     * Register new user
     */
    CompletableFuture<UserDto> register(RegisterRequestDto request);
    
    /**
     * Verify token
     */
    CompletableFuture<Boolean> verifyToken(String accessToken);
}
