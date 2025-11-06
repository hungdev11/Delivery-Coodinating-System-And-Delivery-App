package com.ds.user.common.interfaces;

import com.ds.user.common.entities.base.User;
import com.ds.user.common.entities.common.PagingRequest;
import com.ds.user.common.entities.common.PagingRequestV0;
import com.ds.user.common.entities.common.PagingRequestV2;
import com.ds.user.common.entities.common.paging.PagedData;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IUserService {
    User createUser(User user);
    User updateUser(UUID id, User user);
    void deleteUser(UUID id);
    Optional<User> getUser(UUID id);
    
    /**
     * V1 API - Get users with filtering and sorting (existing)
     */
    PagedData<User> getUsers(PagingRequest query);
    
    /**
     * V0 API - Get users with simple paging and sorting (no dynamic filters)
     */
    PagedData<User> getUsersV0(PagingRequestV0 query);
    
    /**
     * V2 API - Get users with enhanced filtering (operations between each pair)
     */
    PagedData<User> getUsersV2(PagingRequestV2 query);
    
    Optional<User> getUserByUsername(String username);
    Optional<User> getUserByEmail(String email);

    /**
     * Create or update a user record based on Keycloak ID.
     */
    User upsertByKeycloakId(String keycloakId, String username, String email, String firstName, String lastName);

}
