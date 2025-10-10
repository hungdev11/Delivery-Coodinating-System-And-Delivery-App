package com.ds.project.business.v1.services;

import com.ds.project.app_context.models.Role;
import com.ds.project.app_context.models.User;
import com.ds.project.app_context.models.UserRoles;
import com.ds.project.app_context.repositories.RoleRepository;
import com.ds.project.app_context.repositories.UserRepository;
import com.ds.project.app_context.repositories.UserRolesRepository;
import com.ds.project.common.entities.dto.request.UserRequest;
import com.ds.project.common.entities.dto.response.UserResponse;
import com.ds.project.common.interfaces.IUserService;
import com.ds.project.common.mapper.UserMapper;
import com.ds.project.common.utils.PasswordUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * User service implementation
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UserService implements IUserService {
    
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRolesRepository userRolesRepository;
    private final UserMapper userMapper;
    
    @Override
    public UserResponse createUser(UserRequest userRequest) {
        // Check if user already exists
        if (userRepository.existsByEmail(userRequest.getEmail())) {
            throw new IllegalArgumentException("User with email " + userRequest.getEmail() + " already exists");
        }
        if (userRepository.existsByUsername(userRequest.getUsername())) {
            throw new IllegalArgumentException("User with username " + userRequest.getUsername() + " already exists");
        }
        
        User user = userMapper.mapToEntity(userRequest);
        user.setPassword(PasswordUtils.encodePassword(userRequest.getPassword()));
        
        // Assign roles if provided
        if (userRequest.getRoles() != null && !userRequest.getRoles().isEmpty()) {
            for (String roleName : userRequest.getRoles()) {
                Optional<Role> roleOpt = roleRepository.findByName(roleName);
                if (roleOpt.isPresent() && !roleOpt.get().getDeleted()) {
                    user.addRole(roleOpt.get());
                } else {
                    log.warn("Role '{}' not found or deleted, skipping role assignment for user: {}", 
                        roleName, userRequest.getEmail());
                }
            }
        }
        
        User savedUser = userRepository.save(user);
        
        // Force flush to ensure the user is persisted before mapping
        userRepository.flush();
        
        return userMapper.mapToResponse(savedUser);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Optional<UserResponse> getUserById(String id) {
        return userRepository.findById(id)
            .filter(user -> !user.getDeleted())
            .map(userMapper::mapToResponse);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Optional<UserResponse> getUserByEmail(String email) {
        return userRepository.findByEmail(email)
            .filter(user -> !user.getDeleted())
            .map(userMapper::mapToResponse);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Optional<UserResponse> getUserByUsername(String username) {
        return userRepository.findByUsername(username)
            .filter(user -> !user.getDeleted())
            .map(userMapper::mapToResponse);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
            .filter(user -> !user.getDeleted())
            .map(userMapper::mapToResponse)
            .collect(Collectors.toList());
    }
    
    @Override
    @Transactional
    public UserResponse updateUser(String id, UserRequest userRequest) {
        
        User existingUser = userRepository.findById(id)
            .filter(user -> !user.getDeleted())
            .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + id));
        
        // Check if new email conflicts with existing user
        if (!existingUser.getEmail().equals(userRequest.getEmail()) && 
            userRepository.existsByEmail(userRequest.getEmail())) {
            throw new IllegalArgumentException("User with email " + userRequest.getEmail() + " already exists");
        }
        
        // Check if new username conflicts with existing user
        if (!existingUser.getUsername().equals(userRequest.getUsername()) && 
            userRepository.existsByUsername(userRequest.getUsername())) {
            throw new IllegalArgumentException("User with username " + userRequest.getUsername() + " already exists");
        }
        
        existingUser.setEmail(userRequest.getEmail());
        existingUser.setUsername(userRequest.getUsername());
        existingUser.setFirstName(userRequest.getFirstName());
        existingUser.setLastName(userRequest.getLastName());
        existingUser.setPassword(PasswordUtils.encodePassword(userRequest.getPassword()));
        existingUser.setUpdatedAt(LocalDateTime.now());
        
        // Update roles if provided
        if (userRequest.getRoles() != null) {
            // Clear existing roles
            if (existingUser.getUserRoles() != null) {
                // Remove all existing user roles from database
                userRolesRepository.deleteByUserId(existingUser.getId());
                existingUser.getUserRoles().clear();
            }
            
            // Add new roles
            for (String roleName : userRequest.getRoles()) {
                Optional<Role> roleOpt = roleRepository.findByName(roleName);
                if (roleOpt.isPresent() && !roleOpt.get().getDeleted()) {
                    existingUser.addRole(roleOpt.get());
                } else {
                    log.warn("Role '{}' not found or deleted, skipping role assignment for user: {}", 
                        roleName, existingUser.getEmail());
                }
            }
        }
        
        User updatedUser = userRepository.save(existingUser);
        
        return userMapper.mapToResponse(updatedUser);
    }
    
    @Override
    @Transactional
    public void deleteUser(String id) {
        
        User user = userRepository.findById(id)
            .filter(u -> !u.getDeleted())
            .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + id));
        
        user.setDeleted(true);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
        
        log.info("Successfully soft deleted user: {}", user.getEmail());
    }
    
    @Override
    @Transactional
    public UserResponse assignRole(String userId, String roleId) {
        log.info("Assigning role {} to user {}", roleId, userId);
        
        User user = userRepository.findById(userId)
            .filter(u -> !u.getDeleted())
            .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));
        
        Role role = roleRepository.findById(roleId)
            .filter(r -> !r.getDeleted())
            .orElseThrow(() -> new IllegalArgumentException("Role not found with id: " + roleId));
        
        // Check if user already has this role
        if (userRolesRepository.findByUserAndRole(user, role).isPresent()) {
            throw new IllegalArgumentException("User already has role: " + role.getName());
        }
        
        UserRoles userRole = UserRoles.builder()
            .user(user)
            .role(role)
            .createdAt(LocalDateTime.now())
            .build();
        
        userRolesRepository.save(userRole);
        user.addRole(role);
        
        return userMapper.mapToResponse(user);
    }
    
    @Override
    @Transactional
    public UserResponse removeRole(String userId, String roleId) {
        
        User user = userRepository.findById(userId)
            .filter(u -> !u.getDeleted())
            .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));
        
        Role role = roleRepository.findById(roleId)
            .filter(r -> !r.getDeleted())
            .orElseThrow(() -> new IllegalArgumentException("Role not found with id: " + roleId));
        
        UserRoles userRole = userRolesRepository.findByUserAndRole(user, role)
            .orElseThrow(() -> new IllegalArgumentException("User does not have role: " + role.getName()));
        
        userRolesRepository.delete(userRole);
        user.removeRole(role);
        
        return userMapper.mapToResponse(user);
    }
    
    @Override
    @Transactional(readOnly = true)
    public boolean hasRole(String userId, String roleName) {
        
        User user = userRepository.findById(userId)
            .filter(u -> !u.getDeleted())
            .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));
        
        return user.hasRole(roleName);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Optional<UserResponse> authenticate(String username, String password) {
        
        Optional<User> userOpt = userRepository.findByUsername(username)
            .filter(user -> !user.getDeleted());
        
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (PasswordUtils.matches(password, user.getPassword())) {
                log.info("User authenticated successfully: {}", username);
                return Optional.of(userMapper.mapToResponse(user));
            } else {
                log.warn("Authentication failed for user: {} - invalid password", username);
            }
        } else {
            log.warn("Authentication failed for user: {} - user not found", username);
        }
        
        return Optional.empty();
    }
    
    /**
     * Authenticate user by email or username
     */
    @Transactional(readOnly = true)
    public Optional<UserResponse> authenticateByEmailOrUsername(String emailOrUsername, String password) {
        Optional<User> userOpt = Optional.empty();
        
        // Try to find user by email first
        if (emailOrUsername.contains("@")) {
            userOpt = userRepository.findByEmail(emailOrUsername)
                .filter(user -> !user.getDeleted());
        }
        
        // If not found by email, try username
        if (userOpt.isEmpty()) {
            userOpt = userRepository.findByUsername(emailOrUsername)
                .filter(user -> !user.getDeleted());
        }
        
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (PasswordUtils.matches(password, user.getPassword())) {
                log.info("User authenticated successfully: {}", emailOrUsername);
                return Optional.of(userMapper.mapToResponse(user));
            } else {
                log.warn("Authentication failed for user: {} - invalid password", emailOrUsername);
            }
        } else {
            log.warn("Authentication failed for user: {} - user not found", emailOrUsername);
        }
        
        return Optional.empty();
    }
    
}
