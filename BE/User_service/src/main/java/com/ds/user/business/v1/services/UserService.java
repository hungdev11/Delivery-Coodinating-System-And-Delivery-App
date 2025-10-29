package com.ds.user.business.v1.services;

import com.ds.user.common.entities.base.User;
import com.ds.user.common.entities.common.PagingRequest;
import com.ds.user.common.entities.common.paging.PagedData;
import com.ds.user.common.helper.FilterableFieldRegistry;
import com.ds.user.common.helper.GenericQueryService;
import com.ds.user.app_context.repositories.UserRepository;
import com.ds.user.common.interfaces.IUserService;

import org.springframework.beans.factory.annotation.Autowired;

import com.ds.user.common.utils.EnhancedQueryParser;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserService implements IUserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FilterableFieldRegistry fieldRegistry;

    @Override
    public User createUser(User user) {
        return userRepository.save(user);
    }

    @Override
    public User updateUser(UUID id, User user) {
        User existingUser = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        existingUser.setFirstName(user.getFirstName());
        existingUser.setLastName(user.getLastName());
        existingUser.setEmail(user.getEmail());
        existingUser.setPhone(user.getPhone());
        existingUser.setAddress(user.getAddress());
        return userRepository.save(existingUser);
    }

    @Override
    public void deleteUser(UUID id) {
        userRepository.deleteById(id);
    }

    @Override
    public Optional<User> getUser(UUID id) {
        return userRepository.findById(id);
    }

    @Override
    public PagedData<User> getUsers(PagingRequest query) {
        // Initialize field registry for User entity if not already done
        if (fieldRegistry.getFilterableFields(User.class).isEmpty()) {
            fieldRegistry.autoDiscoverFields(User.class);
        }

        // Set the field registry for enhanced query parser
        EnhancedQueryParser.setFieldRegistry(fieldRegistry);

        // Use GenericQueryService for consistent query handling
        return GenericQueryService.executeQuery(userRepository, query, User.class);
    }

    @Override
    public Optional<User> getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    @Override
    public Optional<User> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Override
    public User upsertByKeycloakId(String keycloakId, String username, String email, String firstName,
            String lastName) {
        Optional<User> existingOpt = userRepository.findByKeycloakId(keycloakId);
        if (existingOpt.isPresent()) {
            User existing = existingOpt.get();
            existing.setUsername(username != null ? username : existing.getUsername());
            existing.setEmail(email != null ? email : existing.getEmail());
            existing.setFirstName(firstName != null ? firstName : existing.getFirstName());
            existing.setLastName(lastName != null ? lastName : existing.getLastName());
            return userRepository.save(existing);
        }

        User user = User.builder()
                .keycloakId(keycloakId)
                .username(username)
                .email(email)
                .firstName(firstName)
                .lastName(lastName)
                .status(User.UserStatus.ACTIVE)
                .build();
        return userRepository.save(user);
    }

}
