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

    @PostMapping("/me/addresses")
    @Operation(summary = "Create address for current user", description = "Client endpoint - creates address for the authenticated user")
    public ResponseEntity<BaseResponse<UserAddressDto>> createMyAddress(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @Valid @RequestBody CreateUserAddressRequest request) {
        log.debug("Create address for user: {}", userId);

        if (userId == null || userId.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(BaseResponse.error("User ID is required"));
        }

        UserAddressDto created = userAddressService.createUserAddress(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.success(created));
    }

    @GetMapping("/me/addresses")
    @Operation(summary = "Get all addresses for current user", description = "Client endpoint - returns all addresses for the authenticated user")
    public ResponseEntity<BaseResponse<List<UserAddressDto>>> getMyAddresses(
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        log.debug("Get addresses for user: {}", userId);

        if (userId == null || userId.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(BaseResponse.error("User ID is required"));
        }

        List<UserAddressDto> addresses = userAddressService.getUserAddresses(userId);
        return ResponseEntity.ok(BaseResponse.success(addresses));
    }

    @GetMapping("/me/addresses/primary")
    @Operation(summary = "Get primary address for current user", description = "Client endpoint - returns the primary address for the authenticated user")
    public ResponseEntity<BaseResponse<UserAddressDto>> getMyPrimaryAddress(
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        log.debug("Get primary address for user: {}", userId);

        if (userId == null || userId.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(BaseResponse.error("User ID is required"));
        }

        UserAddressDto address = userAddressService.getPrimaryUserAddress(userId);
        return ResponseEntity.ok(BaseResponse.success(address));
    }

    @GetMapping("/me/addresses/{addressId}")
    @Operation(summary = "Get address by ID for current user", description = "Client endpoint - returns a specific address for the authenticated user")
    public ResponseEntity<BaseResponse<UserAddressDto>> getMyAddress(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @PathVariable String addressId) {
        log.debug("Get address {} for user: {}", addressId, userId);

        if (userId == null || userId.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(BaseResponse.error("User ID is required"));
        }

        UserAddressDto address = userAddressService.getUserAddress(userId, addressId);
        return ResponseEntity.ok(BaseResponse.success(address));
    }

    @PutMapping("/me/addresses/{addressId}")
    @Operation(summary = "Update address for current user", description = "Client endpoint - updates a specific address for the authenticated user")
    public ResponseEntity<BaseResponse<UserAddressDto>> updateMyAddress(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @PathVariable String addressId,
            @Valid @RequestBody UpdateUserAddressRequest request) {
        log.debug("Update address {} for user: {}", addressId, userId);

        if (userId == null || userId.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(BaseResponse.error("User ID is required"));
        }

        UserAddressDto updated = userAddressService.updateUserAddress(userId, addressId, request);
        return ResponseEntity.ok(BaseResponse.success(updated));
    }

    @DeleteMapping("/me/addresses/{addressId}")
    @Operation(summary = "Delete address for current user", description = "Client endpoint - deletes a specific address for the authenticated user")
    public ResponseEntity<BaseResponse<Void>> deleteMyAddress(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @PathVariable String addressId) {
        log.debug("Delete address {} for user: {}", addressId, userId);

        if (userId == null || userId.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(BaseResponse.error("User ID is required"));
        }

        userAddressService.deleteUserAddress(userId, addressId);
        return ResponseEntity.ok(BaseResponse.success(null));
    }

    @PutMapping("/me/addresses/{addressId}/set-primary")
    @Operation(summary = "Set address as primary for current user", description = "Client endpoint - sets a specific address as primary for the authenticated user")
    public ResponseEntity<BaseResponse<UserAddressDto>> setMyPrimaryAddress(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @PathVariable String addressId) {
        log.debug("Set primary address {} for user: {}", addressId, userId);

        if (userId == null || userId.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(BaseResponse.error("User ID is required"));
        }

        UserAddressDto updated = userAddressService.setPrimaryAddress(userId, addressId);
        return ResponseEntity.ok(BaseResponse.success(updated));
    }

    // ==================== ADMIN ENDPOINTS (Any User) ====================

    @PostMapping("/{userId}/addresses")
    @Operation(summary = "Create address for user (Admin)", description = "Admin endpoint - creates address for any user")
    public ResponseEntity<BaseResponse<UserAddressDto>> createUserAddress(
            @PathVariable String userId,
            @Valid @RequestBody CreateUserAddressRequest request) {
        log.debug("Create address for user: {} (Admin)", userId);

        UserAddressDto created = userAddressService.createUserAddressForUser(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.success(created));
    }

    @GetMapping("/{userId}/addresses")
    @Operation(summary = "Get all addresses for user (Admin)", description = "Admin endpoint - returns all addresses for any user")
    public ResponseEntity<BaseResponse<List<UserAddressDto>>> getUserAddresses(
            @PathVariable String userId) {
        log.debug("Get addresses for user: {} (Admin)", userId);

        List<UserAddressDto> addresses = userAddressService.getUserAddressesForUser(userId);
        return ResponseEntity.ok(BaseResponse.success(addresses));
    }

    @GetMapping("/{userId}/addresses/primary")
    @Operation(summary = "Get primary address for user (Admin)", description = "Admin endpoint - returns the primary address for any user")
    public ResponseEntity<BaseResponse<UserAddressDto>> getUserPrimaryAddress(
            @PathVariable String userId) {
        log.debug("Get primary address for user: {} (Admin)", userId);

        UserAddressDto address = userAddressService.getPrimaryUserAddress(userId);
        return ResponseEntity.ok(BaseResponse.success(address));
    }

    @GetMapping("/{userId}/addresses/{addressId}")
    @Operation(summary = "Get address by ID for user (Admin)", description = "Admin endpoint - returns a specific address for any user")
    public ResponseEntity<BaseResponse<UserAddressDto>> getUserAddress(
            @PathVariable String userId,
            @PathVariable String addressId) {
        log.debug("Get address {} for user: {} (Admin)", addressId, userId);

        UserAddressDto address = userAddressService.getUserAddressForUser(userId, addressId);
        return ResponseEntity.ok(BaseResponse.success(address));
    }

    @PutMapping("/{userId}/addresses/{addressId}")
    @Operation(summary = "Update address for user (Admin)", description = "Admin endpoint - updates a specific address for any user")
    public ResponseEntity<BaseResponse<UserAddressDto>> updateUserAddress(
            @PathVariable String userId,
            @PathVariable String addressId,
            @Valid @RequestBody UpdateUserAddressRequest request) {
        log.debug("Update address {} for user: {} (Admin)", addressId, userId);

        UserAddressDto updated = userAddressService.updateUserAddressForUser(userId, addressId, request);
        return ResponseEntity.ok(BaseResponse.success(updated));
    }

    @DeleteMapping("/{userId}/addresses/{addressId}")
    @Operation(summary = "Delete address for user (Admin)", description = "Admin endpoint - deletes a specific address for any user")
    public ResponseEntity<BaseResponse<Void>> deleteUserAddress(
            @PathVariable String userId,
            @PathVariable String addressId) {
        log.debug("Delete address {} for user: {} (Admin)", addressId, userId);

        userAddressService.deleteUserAddressForUser(userId, addressId);
        return ResponseEntity.ok(BaseResponse.success(null));
    }
}
