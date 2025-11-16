package com.ds.user.application.controllers.v1;

import com.ds.user.common.entities.common.BaseResponse;
import com.ds.user.common.entities.dto.UserAddressDto;
import com.ds.user.common.entities.dto.request.CreateUserAddressRequest;
import com.ds.user.common.entities.dto.request.UpdateUserAddressRequest;
import com.ds.user.common.interfaces.IUserAddressService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST API Controller for User Address Management
 * 
 * Client endpoints: /api/v1/users/me/addresses (for current user)
 * Admin endpoints: /api/v1/users/{userId}/addresses (for any user)
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "User Addresses", description = "User Address Management API")
public class UserAddressController {

    private final IUserAddressService userAddressService;

    // ==================== CLIENT ENDPOINTS (Current User) ====================

    /**
     * POST /api/v1/users/me/addresses
     * Create address for current user
     */
    @PostMapping("/me/addresses")
    @Operation(summary = "Create address for current user", description = "Client endpoint - creates address for the authenticated user")
    public ResponseEntity<BaseResponse<UserAddressDto>> createMyAddress(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @Valid @RequestBody CreateUserAddressRequest request) {
        log.info("POST /api/v1/users/me/addresses - Create address for user: {}", userId);
        
        // TODO: Extract userId from JWT token in production
        if (userId == null || userId.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(BaseResponse.error("User ID is required"));
        }

        UserAddressDto created = userAddressService.createUserAddress(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.success(created, "Address created successfully"));
    }

    /**
     * GET /api/v1/users/me/addresses
     * Get all addresses for current user
     */
    @GetMapping("/me/addresses")
    @Operation(summary = "Get all addresses for current user", description = "Client endpoint - returns all addresses for the authenticated user")
    public ResponseEntity<BaseResponse<List<UserAddressDto>>> getMyAddresses(
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        log.info("GET /api/v1/users/me/addresses - Get addresses for user: {}", userId);
        
        if (userId == null || userId.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(BaseResponse.error("User ID is required"));
        }

        List<UserAddressDto> addresses = userAddressService.getUserAddresses(userId);
        return ResponseEntity.ok(BaseResponse.success(addresses));
    }

    /**
     * GET /api/v1/users/me/addresses/primary
     * Get primary address for current user
     */
    @GetMapping("/me/addresses/primary")
    @Operation(summary = "Get primary address for current user", description = "Client endpoint - returns the primary address for the authenticated user")
    public ResponseEntity<BaseResponse<UserAddressDto>> getMyPrimaryAddress(
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        log.info("GET /api/v1/users/me/addresses/primary - Get primary address for user: {}", userId);
        
        if (userId == null || userId.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(BaseResponse.error("User ID is required"));
        }

        UserAddressDto address = userAddressService.getPrimaryUserAddress(userId);
        return ResponseEntity.ok(BaseResponse.success(address));
    }

    /**
     * GET /api/v1/users/me/addresses/{addressId}
     * Get specific address for current user
     */
    @GetMapping("/me/addresses/{addressId}")
    @Operation(summary = "Get address by ID for current user", description = "Client endpoint - returns a specific address for the authenticated user")
    public ResponseEntity<BaseResponse<UserAddressDto>> getMyAddress(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @PathVariable String addressId) {
        log.info("GET /api/v1/users/me/addresses/{} - Get address for user: {}", addressId, userId);
        
        if (userId == null || userId.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(BaseResponse.error("User ID is required"));
        }

        UserAddressDto address = userAddressService.getUserAddress(userId, addressId);
        return ResponseEntity.ok(BaseResponse.success(address));
    }

    /**
     * PUT /api/v1/users/me/addresses/{addressId}
     * Update address for current user
     */
    @PutMapping("/me/addresses/{addressId}")
    @Operation(summary = "Update address for current user", description = "Client endpoint - updates a specific address for the authenticated user")
    public ResponseEntity<BaseResponse<UserAddressDto>> updateMyAddress(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @PathVariable String addressId,
            @Valid @RequestBody UpdateUserAddressRequest request) {
        log.info("PUT /api/v1/users/me/addresses/{} - Update address for user: {}", addressId, userId);
        
        if (userId == null || userId.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(BaseResponse.error("User ID is required"));
        }

        UserAddressDto updated = userAddressService.updateUserAddress(userId, addressId, request);
        return ResponseEntity.ok(BaseResponse.success(updated, "Address updated successfully"));
    }

    /**
     * DELETE /api/v1/users/me/addresses/{addressId}
     * Delete address for current user
     */
    @DeleteMapping("/me/addresses/{addressId}")
    @Operation(summary = "Delete address for current user", description = "Client endpoint - deletes a specific address for the authenticated user")
    public ResponseEntity<BaseResponse<Void>> deleteMyAddress(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @PathVariable String addressId) {
        log.info("DELETE /api/v1/users/me/addresses/{} - Delete address for user: {}", addressId, userId);
        
        if (userId == null || userId.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(BaseResponse.error("User ID is required"));
        }

        userAddressService.deleteUserAddress(userId, addressId);
        return ResponseEntity.ok(BaseResponse.success(null, "Address deleted successfully"));
    }

    /**
     * PUT /api/v1/users/me/addresses/{addressId}/set-primary
     * Set address as primary for current user
     */
    @PutMapping("/me/addresses/{addressId}/set-primary")
    @Operation(summary = "Set address as primary for current user", description = "Client endpoint - sets a specific address as primary for the authenticated user")
    public ResponseEntity<BaseResponse<UserAddressDto>> setMyPrimaryAddress(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @PathVariable String addressId) {
        log.info("PUT /api/v1/users/me/addresses/{}/set-primary - Set primary address for user: {}", addressId, userId);
        
        if (userId == null || userId.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(BaseResponse.error("User ID is required"));
        }

        UserAddressDto updated = userAddressService.setPrimaryAddress(userId, addressId);
        return ResponseEntity.ok(BaseResponse.success(updated, "Primary address updated successfully"));
    }

    // ==================== ADMIN ENDPOINTS (Any User) ====================

    /**
     * POST /api/v1/users/{userId}/addresses
     * Create address for any user (Admin only)
     */
    @PostMapping("/{userId}/addresses")
    @Operation(summary = "Create address for user (Admin)", description = "Admin endpoint - creates address for any user")
    public ResponseEntity<BaseResponse<UserAddressDto>> createUserAddress(
            @PathVariable String userId,
            @Valid @RequestBody CreateUserAddressRequest request) {
        log.info("POST /api/v1/users/{}/addresses - Create address (Admin)", userId);

        UserAddressDto created = userAddressService.createUserAddressForUser(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.success(created, "Address created successfully"));
    }

    /**
     * GET /api/v1/users/{userId}/addresses
     * Get all addresses for any user (Admin only)
     */
    @GetMapping("/{userId}/addresses")
    @Operation(summary = "Get all addresses for user (Admin)", description = "Admin endpoint - returns all addresses for any user")
    public ResponseEntity<BaseResponse<List<UserAddressDto>>> getUserAddresses(
            @PathVariable String userId) {
        log.info("GET /api/v1/users/{}/addresses - Get addresses (Admin)", userId);

        List<UserAddressDto> addresses = userAddressService.getUserAddressesForUser(userId);
        return ResponseEntity.ok(BaseResponse.success(addresses));
    }

    /**
     * GET /api/v1/users/{userId}/addresses/primary
     * Get primary address for any user (Admin only)
     */
    @GetMapping("/{userId}/addresses/primary")
    @Operation(summary = "Get primary address for user (Admin)", description = "Admin endpoint - returns the primary address for any user")
    public ResponseEntity<BaseResponse<UserAddressDto>> getUserPrimaryAddress(
            @PathVariable String userId) {
        log.info("GET /api/v1/users/{}/addresses/primary - Get primary address (Admin)", userId);

        UserAddressDto address = userAddressService.getPrimaryUserAddress(userId);
        return ResponseEntity.ok(BaseResponse.success(address));
    }

    /**
     * GET /api/v1/users/{userId}/addresses/{addressId}
     * Get specific address for any user (Admin only)
     */
    @GetMapping("/{userId}/addresses/{addressId}")
    @Operation(summary = "Get address by ID for user (Admin)", description = "Admin endpoint - returns a specific address for any user")
    public ResponseEntity<BaseResponse<UserAddressDto>> getUserAddress(
            @PathVariable String userId,
            @PathVariable String addressId) {
        log.info("GET /api/v1/users/{}/addresses/{} - Get address (Admin)", userId, addressId);

        UserAddressDto address = userAddressService.getUserAddressForUser(userId, addressId);
        return ResponseEntity.ok(BaseResponse.success(address));
    }

    /**
     * PUT /api/v1/users/{userId}/addresses/{addressId}
     * Update address for any user (Admin only)
     */
    @PutMapping("/{userId}/addresses/{addressId}")
    @Operation(summary = "Update address for user (Admin)", description = "Admin endpoint - updates a specific address for any user")
    public ResponseEntity<BaseResponse<UserAddressDto>> updateUserAddress(
            @PathVariable String userId,
            @PathVariable String addressId,
            @Valid @RequestBody UpdateUserAddressRequest request) {
        log.info("PUT /api/v1/users/{}/addresses/{} - Update address (Admin)", userId, addressId);

        UserAddressDto updated = userAddressService.updateUserAddressForUser(userId, addressId, request);
        return ResponseEntity.ok(BaseResponse.success(updated, "Address updated successfully"));
    }

    /**
     * DELETE /api/v1/users/{userId}/addresses/{addressId}
     * Delete address for any user (Admin only)
     */
    @DeleteMapping("/{userId}/addresses/{addressId}")
    @Operation(summary = "Delete address for user (Admin)", description = "Admin endpoint - deletes a specific address for any user")
    public ResponseEntity<BaseResponse<Void>> deleteUserAddress(
            @PathVariable String userId,
            @PathVariable String addressId) {
        log.info("DELETE /api/v1/users/{}/addresses/{} - Delete address (Admin)", userId, addressId);

        userAddressService.deleteUserAddressForUser(userId, addressId);
        return ResponseEntity.ok(BaseResponse.success(null, "Address deleted successfully"));
    }
}
