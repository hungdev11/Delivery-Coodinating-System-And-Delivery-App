package com.ds.project.common.interfaces;

import com.ds.project.common.entities.base.BaseResponse;
import com.ds.project.common.entities.base.Page;
import com.ds.project.common.entities.base.PagedData;
import com.ds.project.common.entities.dto.UserDto;
import com.ds.project.common.entities.dto.request.UserRequest;

import java.util.Optional;

/**
 * User service interface
 */
public interface IUserService {
    
    /**
     * Create a new user
     */
    BaseResponse<UserDto> createUser(UserRequest userRequest);
    
    /**
     * Get user by ID
     */
    Optional<BaseResponse<UserDto>> getUserById(String id);
    
    /**
     * Get user by email
     */
    Optional<BaseResponse<UserDto>> getUserByEmail(String email);
    
    /**
     * Get user by username
     */
    Optional<BaseResponse<UserDto>> getUserByUsername(String username);
    
    /**
     * Get all users
     */
    BaseResponse<PagedData<Page, UserDto>> getAllUsers();
    
    /**
     * Update user
     */
    BaseResponse<UserDto> updateUser(String id, UserRequest userRequest);
    
    /**
     * Delete user (soft delete)
     */
    void deleteUser(String id);
    
    /**
     * Assign role to user
     */
    BaseResponse<UserDto> assignRole(String userId, String roleId);
    
    /**
     * Remove role from user
     */
    BaseResponse<UserDto> removeRole(String userId, String roleId);
    
    /**
     * Check if user has role
     */
    boolean hasRole(String userId, String roleName);
    
    /**
     * Authenticate user
     */
    Optional<BaseResponse<UserDto>> authenticate(String username, String password);
    
    /**
     * Authenticate user by email or username
     */
    Optional<BaseResponse<UserDto>> authenticateByEmailOrUsername(String emailOrUsername, String password);
}
