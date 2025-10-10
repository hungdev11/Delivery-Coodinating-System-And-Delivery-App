package com.ds.project.common.interfaces;

import com.ds.project.common.entities.dto.request.UserRequest;
import com.ds.project.common.entities.dto.response.UserResponse;

import java.util.List;
import java.util.Optional;

/**
 * User service interface
 */
public interface IUserService {
    
    /**
     * Create a new user
     */
    UserResponse createUser(UserRequest userRequest);
    
    /**
     * Get user by ID
     */
    Optional<UserResponse> getUserById(String id);
    
    /**
     * Get user by email
     */
    Optional<UserResponse> getUserByEmail(String email);
    
    /**
     * Get user by username
     */
    Optional<UserResponse> getUserByUsername(String username);
    
    /**
     * Get all users
     */
    List<UserResponse> getAllUsers();
    
    /**
     * Update user
     */
    UserResponse updateUser(String id, UserRequest userRequest);
    
    /**
     * Delete user (soft delete)
     */
    void deleteUser(String id);
    
    /**
     * Assign role to user
     */
    UserResponse assignRole(String userId, String roleId);
    
    /**
     * Remove role from user
     */
    UserResponse removeRole(String userId, String roleId);
    
    /**
     * Check if user has role
     */
    boolean hasRole(String userId, String roleName);
    
    /**
     * Authenticate user
     */
    Optional<UserResponse> authenticate(String username, String password);
    
    /**
     * Authenticate user by email or username
     */
    Optional<UserResponse> authenticateByEmailOrUsername(String emailOrUsername, String password);
}
