package com.ds.common.interfaces;

import com.ds.app_context.models.User;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IUserService {
    User createUser(User user);
    User updateUser(UUID id, User user);
    void deleteUser(UUID id);
    Optional<User> getUser(UUID id);
    List<User> listUsers();
    Optional<User> getUserByUsername(String username);
    Optional<User> getUserByEmail(String email);
}
