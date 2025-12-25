package com.ds.user.common.interfaces;

import com.ds.user.common.entities.dto.UserAddressDto;
import com.ds.user.common.entities.dto.request.CreateUserAddressRequest;
import com.ds.user.common.entities.dto.request.UpdateUserAddressRequest;

import java.util.List;

public interface IUserAddressService {
    /**
     * Create a new user address
     */
    UserAddressDto createUserAddress(String userId, CreateUserAddressRequest request);
    
    /**
     * Update an existing user address
     * Only the owner or admin can update
     */
    UserAddressDto updateUserAddress(String userId, String addressId, UpdateUserAddressRequest request);
    
    /**
     * Delete a user address
     * Only the owner or admin can delete
     */
    void deleteUserAddress(String userId, String addressId);
    
    /**
     * Get user address by ID
     */
    UserAddressDto getUserAddress(String userId, String addressId);
    
    /**
     * Get all addresses for a user
     */
    List<UserAddressDto> getUserAddresses(String userId);
    
    /**
     * Get primary address for a user
     */
    UserAddressDto getPrimaryUserAddress(String userId);
    
    /**
     * Set an address as primary (and unset others)
     */
    UserAddressDto setPrimaryAddress(String userId, String addressId);
    
    /**
     * Admin: Create address for any user
     */
    UserAddressDto createUserAddressForUser(String targetUserId, CreateUserAddressRequest request);
    
    /**
     * Admin: Update address for any user
     */
    UserAddressDto updateUserAddressForUser(String targetUserId, String addressId, UpdateUserAddressRequest request);
    
    /**
     * Admin: Delete address for any user
     */
    void deleteUserAddressForUser(String targetUserId, String addressId);
    
    /**
     * Admin: Get address for any user
     */
    UserAddressDto getUserAddressForUser(String targetUserId, String addressId);
    
    /**
     * Admin: Get all addresses for any user
     */
    List<UserAddressDto> getUserAddressesForUser(String targetUserId);
    
    /**
     * Admin: Get user address by ID (without userId requirement)
     */
    UserAddressDto getUserAddressById(String addressId);
}
