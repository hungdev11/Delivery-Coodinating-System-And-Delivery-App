package com.ds.gateway.business.v1.services;

import com.ds.gateway.common.entities.dto.auth.*;
import com.ds.gateway.common.entities.dto.common.BaseResponse;
import com.ds.gateway.common.entities.dto.user.UserDto;
import com.ds.gateway.common.exceptions.ServiceUnavailableException;
import com.ds.gateway.common.interfaces.IAuthServiceClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.concurrent.CompletableFuture;

/**
 * REST client implementation for Auth Service
 */
@Slf4j
@Service
public class AuthServiceClient implements IAuthServiceClient {
    
    @Autowired
    private WebClient userServiceWebClient;
    
    @Override
    public CompletableFuture<LoginResponseDto> login(LoginRequestDto request) {
        log.debug("Login request via REST for user: {}", request.getUsername());
        
        return userServiceWebClient.post()
            .uri("/api/auth/login")
            .bodyValue(request)
            .retrieve()
            .bodyToMono(new ParameterizedTypeReference<BaseResponse<LoginResponseDto>>() {})
            .map(BaseResponse::getResult)
            .onErrorMap(ex -> new ServiceUnavailableException("Auth service unavailable: " + ex.getMessage(), ex))
            .toFuture();
    }
    
    @Override
    public CompletableFuture<RefreshTokenResponseDto> refreshToken(RefreshTokenRequestDto request) {
        log.debug("Refresh token request via REST");
        
        return userServiceWebClient.post()
            .uri("/api/auth/refresh-token")
            .bodyValue(request)
            .retrieve()
            .bodyToMono(new ParameterizedTypeReference<BaseResponse<RefreshTokenResponseDto>>() {})
            .map(BaseResponse::getResult)
            .onErrorMap(ex -> new ServiceUnavailableException("Auth service unavailable: " + ex.getMessage(), ex))
            .toFuture();
    }
    
    @Override
    public CompletableFuture<Void> logout(String refreshToken) {
        log.debug("Logout request via REST");
        
        return userServiceWebClient.post()
            .uri("/api/auth/logout")
            .bodyValue(new RefreshTokenRequestDto(refreshToken))
            .retrieve()
            .bodyToMono(Void.class)
            .onErrorMap(ex -> new ServiceUnavailableException("Auth service unavailable: " + ex.getMessage(), ex))
            .toFuture();
    }
    
    @Override
    public CompletableFuture<UserDto> register(RegisterRequestDto request) {
        log.debug("Register request via REST for user: {}", request.getUsername());
        
        return userServiceWebClient.post()
            .uri("/api/auth/register")
            .bodyValue(request)
            .retrieve()
            .bodyToMono(new ParameterizedTypeReference<BaseResponse<UserDto>>() {})
            .map(BaseResponse::getResult)
            .onErrorMap(ex -> new ServiceUnavailableException("Auth service unavailable: " + ex.getMessage(), ex))
            .toFuture();
    }
    
    @Override
    public CompletableFuture<Boolean> verifyToken(String accessToken) {
        log.debug("Verify token request via REST");
        
        return userServiceWebClient.post()
            .uri("/api/auth/verify-token")
            .header("Authorization", "Bearer " + accessToken)
            .retrieve()
            .bodyToMono(new ParameterizedTypeReference<BaseResponse<Boolean>>() {})
            .map(BaseResponse::getResult)
            .onErrorResume(ex -> {
                log.warn("Token verification failed: {}", ex.getMessage());
                return Mono.just(false);
            })
            .toFuture();
    }
}
