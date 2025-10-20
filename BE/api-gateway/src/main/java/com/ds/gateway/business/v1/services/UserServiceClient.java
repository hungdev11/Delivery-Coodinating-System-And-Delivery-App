package com.ds.gateway.business.v1.services;

import com.ds.gateway.common.entities.dto.common.BaseResponse;
import com.ds.gateway.common.entities.dto.common.PagedData;
import com.ds.gateway.common.entities.dto.common.PagingRequest;
import com.ds.gateway.common.entities.dto.user.CreateUserRequestDto;
import com.ds.gateway.common.entities.dto.user.UpdateUserRequestDto;
import com.ds.gateway.common.entities.dto.user.UserDto;
import com.ds.gateway.common.exceptions.ServiceUnavailableException;
import com.ds.gateway.common.interfaces.IUserServiceClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * REST client implementation for User Service
 */
@Slf4j
@Service
public class UserServiceClient implements IUserServiceClient {
    
    @Autowired
    @Qualifier("userServiceWebClient")
    private WebClient userServiceWebClient;
    
    @Override
    public CompletableFuture<UserDto> createUser(CreateUserRequestDto request) {
        log.debug("Creating user via REST: {}", request.getUsername());
        
        return userServiceWebClient.post()
            .uri("/api/v1/users/create")
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
    public CompletableFuture<PagedData<UserDto>> getUsers(PagingRequest query) {
        log.debug("Getting users via POST with filters/sorts/paging");

        return userServiceWebClient.post()
            .uri("/api/v1/users")
            .bodyValue(query)
            .retrieve()
            .bodyToMono(new ParameterizedTypeReference<BaseResponse<PagedData<UserDto>>>() {})
            .map(BaseResponse::getResult)
            .onErrorMap(ex -> new ServiceUnavailableException("User service unavailable: " + ex.getMessage(), ex))
            .toFuture();
    }
    
    @Override
    public CompletableFuture<UserDto> syncUserByKeycloakId(String keycloakId, String username, String email, 
                                                           String firstName, String lastName) {
        log.debug("Syncing user by Keycloak ID via REST: {}", keycloakId);
        
        Map<String, String> request = new HashMap<>();
        request.put("keycloakId", keycloakId);
        if (username != null) request.put("username", username);
        if (email != null) request.put("email", email);
        if (firstName != null) request.put("firstName", firstName);
        if (lastName != null) request.put("lastName", lastName);
        
        return userServiceWebClient.post()
            .uri("/api/v1/users/sync")
            .bodyValue(request)
            .retrieve()
            .bodyToMono(new ParameterizedTypeReference<BaseResponse<UserDto>>() {})
            .map(BaseResponse::getResult)
            .onErrorMap(ex -> new ServiceUnavailableException("User service unavailable: " + ex.getMessage(), ex))
            .toFuture();
    }
}
