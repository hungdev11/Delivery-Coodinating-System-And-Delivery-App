package com.ds.gateway.business.v1.services;

import com.ds.gateway.common.entities.dto.common.BaseResponse;
import com.ds.gateway.common.entities.dto.user.CreateUserRequestDto;
import com.ds.gateway.common.entities.dto.user.UpdateUserRequestDto;
import com.ds.gateway.common.entities.dto.user.UserDto;
import com.ds.gateway.common.exceptions.ServiceUnavailableException;
import com.ds.gateway.common.interfaces.IUserServiceClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * REST client implementation for User Service
 */
@Slf4j
@Service
public class UserServiceClient implements IUserServiceClient {
    
    @Autowired
    private WebClient userServiceWebClient;
    
    @Override
    public CompletableFuture<UserDto> createUser(CreateUserRequestDto request) {
        log.debug("Creating user via REST: {}", request.getUsername());
        
        return userServiceWebClient.post()
            .uri("/api/v1/users")
            .bodyValue(request)
            .retrieve()
            .bodyToMono(new ParameterizedTypeReference<BaseResponse<UserDto>>() {})
            .map(BaseResponse::getResult)
            .onErrorMap(ex -> new ServiceUnavailableException("User service unavailable: " + ex.getMessage(), ex))
            .toFuture();
    }
    
    @Override
    public CompletableFuture<UserDto> getUserById(String userId) {
        log.debug("Getting user by ID via REST: {}", userId);
        
        return userServiceWebClient.get()
            .uri("/api/v1/users/{id}", userId)
            .retrieve()
            .bodyToMono(new ParameterizedTypeReference<BaseResponse<UserDto>>() {})
            .map(BaseResponse::getResult)
            .onErrorMap(ex -> new ServiceUnavailableException("User service unavailable: " + ex.getMessage(), ex))
            .toFuture();
    }
    
    @Override
    public CompletableFuture<UserDto> getUserByUsername(String username) {
        log.debug("Getting user by username via REST: {}", username);
        
        return userServiceWebClient.get()
            .uri("/api/v1/users/username/{username}", username)
            .retrieve()
            .bodyToMono(new ParameterizedTypeReference<BaseResponse<UserDto>>() {})
            .map(BaseResponse::getResult)
            .onErrorMap(ex -> new ServiceUnavailableException("User service unavailable: " + ex.getMessage(), ex))
            .toFuture();
    }
    
    @Override
    public CompletableFuture<UserDto> updateUser(String userId, UpdateUserRequestDto request) {
        log.debug("Updating user via REST: {}", userId);
        
        return userServiceWebClient.put()
            .uri("/api/v1/users/{id}", userId)
            .bodyValue(request)
            .retrieve()
            .bodyToMono(new ParameterizedTypeReference<BaseResponse<UserDto>>() {})
            .map(BaseResponse::getResult)
            .onErrorMap(ex -> new ServiceUnavailableException("User service unavailable: " + ex.getMessage(), ex))
            .toFuture();
    }
    
    @Override
    public CompletableFuture<Void> deleteUser(String userId) {
        log.debug("Deleting user via REST: {}", userId);
        
        return userServiceWebClient.delete()
            .uri("/api/v1/users/{id}", userId)
            .retrieve()
            .bodyToMono(Void.class)
            .onErrorMap(ex -> new ServiceUnavailableException("User service unavailable: " + ex.getMessage(), ex))
            .toFuture();
    }
    
    @Override
    public CompletableFuture<List<UserDto>> listUsers() {
        log.debug("Listing all users via REST");
        
        return userServiceWebClient.get()
            .uri("/api/v1/users")
            .retrieve()
            .bodyToMono(new ParameterizedTypeReference<BaseResponse<List<UserDto>>>() {})
            .map(BaseResponse::getResult)
            .onErrorMap(ex -> new ServiceUnavailableException("User service unavailable: " + ex.getMessage(), ex))
            .toFuture();
    }
    
    @Override
    public CompletableFuture<UserDto> getUserByKeycloakId(String keycloakId) {
        log.debug("Getting user by Keycloak ID via REST: {}", keycloakId);
        
        return userServiceWebClient.get()
            .uri("/api/v1/users/keycloak/{keycloakId}", keycloakId)
            .retrieve()
            .bodyToMono(new ParameterizedTypeReference<BaseResponse<UserDto>>() {})
            .map(BaseResponse::getResult)
            .onErrorMap(ex -> new ServiceUnavailableException("User service unavailable: " + ex.getMessage(), ex))
            .toFuture();
    }
}
