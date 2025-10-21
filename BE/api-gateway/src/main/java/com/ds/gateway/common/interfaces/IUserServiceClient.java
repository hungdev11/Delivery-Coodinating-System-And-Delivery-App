package com.ds.gateway.common.interfaces;

import com.ds.gateway.common.entities.dto.common.PagedData;
import com.ds.gateway.common.entities.dto.common.PagingRequest;
import com.ds.gateway.common.entities.dto.user.CreateUserRequestDto;
import com.ds.gateway.common.entities.dto.user.UpdateUserRequestDto;
import com.ds.gateway.common.entities.dto.user.UserDto;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Interface for User Service REST client
 * Defines contract for calling User microservice
 */
public interface IUserServiceClient {
    
    /**
     * Create a new user
     */
    CompletableFuture<UserDto> createUser(CreateUserRequestDto request);
    
    /**
     * Get user by ID
     */
    CompletableFuture<UserDto> getUserById(String userId);
    
    /**
     * Get user by username
     */
    CompletableFuture<UserDto> getUserByUsername(String username);
    
    /**
     * Update user
     */
    CompletableFuture<UserDto> updateUser(String userId, UpdateUserRequestDto request);
    
    /**
     * Delete user
     */
    CompletableFuture<Void> deleteUser(String userId);

    /**
     * Get users with filtering/sorting/paging (POST)
     */
    CompletableFuture<PagedData<UserDto>> getUsers(PagingRequest query);
    
    /**
     * Sync user by Keycloak ID (create or update)
     */
    CompletableFuture<UserDto> syncUserByKeycloakId(String keycloakId, String username, String email, 
                                                    String firstName, String lastName);
}
