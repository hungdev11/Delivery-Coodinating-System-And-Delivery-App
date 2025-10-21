package com.ds.user.common.interfaces;

import com.ds.user.common.entities.base.User;
import com.ds.user.common.entities.common.PagingRequest;
import com.ds.user.common.entities.common.paging.PagedData;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IUserService {
    User createUser(User user);
    User updateUser(UUID id, User user);
    void deleteUser(UUID id);
    Optional<User> getUser(UUID id);
    PagedData<User> getUsers(PagingRequest query);
    Optional<User> getUserByUsername(String username);
    Optional<User> getUserByEmail(String email);

    /**
     * Create or update a user record based on Keycloak ID.
     */
    User upsertByKeycloakId(String keycloakId, String username, String email, String firstName, String lastName);

}
