package com.ds.user.application.controllers.v1;

import com.ds.user.common.entities.base.User;
import com.ds.user.common.entities.common.BaseResponse;
import com.ds.user.common.entities.common.PagingRequest;
import com.ds.user.common.entities.common.paging.PagedData;
import com.ds.user.common.entities.dto.auth.SyncUserRequest;
import com.ds.user.common.entities.dto.user.CreateUserRequest;
import com.ds.user.common.entities.dto.user.UpdateUserRequest;
import com.ds.user.common.entities.dto.user.UserDto;
import com.ds.user.common.interfaces.IExternalAuthFacade;
import com.ds.user.common.interfaces.IUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * REST API Controller for User Management
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "User Management API")
public class UserController {

    private final IUserService userService;
    private final IExternalAuthFacade externalAuthFacade;

    @PostMapping("/create")
    @Operation(summary = "Create a new user")
    public ResponseEntity<BaseResponse<UserDto>> createUser(@Valid @RequestBody CreateUserRequest request) {
        log.info("POST /api/v1/users/create - Create user: username={}", request.getUsername());
        
        // Note: User ID must be set from Keycloak ID before creating
        // The request should include the Keycloak ID which will be used as the primary key
        User user = User.builder()
                .id(request.getKeycloakId()) // Use Keycloak ID as the primary key
                .username(request.getUsername())
                .email(request.getEmail())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phone(request.getPhone())
                .address(request.getAddress())
                .identityNumber(request.getIdentityNumber())
                .status(request.getStatus())
                .build();
        
        User created = userService.createUser(user);
        UserDto dto = buildUserDto(created);
        
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(BaseResponse.success(dto, "User created successfully"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get user by ID")
    public ResponseEntity<BaseResponse<UserDto>> getUser(@PathVariable String id) {
        log.info("GET /api/v1/users/{} - Get user by ID", id);
        
        try {
            // ID is now a String (Keycloak ID)
            Optional<User> userOpt = userService.getUser(id);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                UserDto dto = buildUserDto(user);
                log.info("Found user: {} with ID: {}", user.getUsername(), user.getId());
                return ResponseEntity.ok(BaseResponse.success(dto));
            } else {
                log.warn("User not found with ID: {}", id);
                return ResponseEntity
                        .status(HttpStatus.NOT_FOUND)
                        .body(BaseResponse.error("User not found"));
            }
        } catch (Exception e) {
            log.error("Unexpected error getting user with ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(BaseResponse.error("Internal server error: " + e.getMessage()));
        }
    }

    @GetMapping("/me")
    @Operation(summary = "Get current user")
    public ResponseEntity<BaseResponse<UserDto>> getCurrentUser() {
        log.info("GET /api/v1/users/me - Get current user");
        
        // This endpoint would typically extract user info from JWT token
        // For now, we'll return a placeholder response
        return ResponseEntity
                .status(HttpStatus.NOT_IMPLEMENTED)
                .body(BaseResponse.error("Current user endpoint not implemented"));
    }

    @GetMapping("/username/{username}")
    @Operation(summary = "Get user by username")
    public ResponseEntity<BaseResponse<UserDto>> getUserByUsername(@PathVariable String username) {
        log.info("GET /api/v1/users/username/{} - Get user by username", username);
        
        return userService.getUserByUsername(username)
                .map(user -> ResponseEntity.ok(BaseResponse.success(buildUserDto(user))))
                .orElse(ResponseEntity
                        .status(HttpStatus.NOT_FOUND)
                        .body(BaseResponse.error("User not found")));
    }

    @PostMapping
    @Operation(summary = "Get all users with advanced filtering and sorting")
    public ResponseEntity<BaseResponse<PagedData<UserDto>>> getUsers(@Valid @RequestBody PagingRequest query) {
        log.info("POST /api/v1/users - Get users with advanced filtering and sorting");
        log.debug("Query payload: {}", query);
        
        try {
            // Get users using the service
            PagedData<User> userPage = userService.getUsers(query);
            
            // Batch fetch roles for all users in parallel
            List<String> userIds = userPage.getData().stream()
                    .map(User::getId)
                    .filter(id -> id != null && !id.isBlank())
                    .toList();
            
            Map<String, List<String>> rolesMap = userIds.isEmpty() 
                    ? Collections.emptyMap() 
                    : externalAuthFacade.batchGetUserRoles(userIds);
            
            // Convert to PagedData<UserDto> with roles
            List<UserDto> userDtos = userPage.getData().stream()
                    .map(user -> buildUserDto(user, rolesMap.getOrDefault(user.getId(), Collections.emptyList())))
                    .toList();
            
            // Use the existing paging from userPage
            PagedData<UserDto> pagedData = PagedData.<UserDto>builder()
                    .data(userDtos)
                    .page(userPage.getPage())
                    .build();
            
            return ResponseEntity.ok(BaseResponse.success(pagedData));
            
        } catch (Exception e) {
            log.error("Error searching users: {}", e.getMessage(), e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(BaseResponse.error("Search failed: " + e.getMessage()));
        }
    }


    @PutMapping("/{id}")
    @Operation(summary = "Update a user")
    public ResponseEntity<BaseResponse<UserDto>> updateUser(
            @PathVariable String id, 
            @Valid @RequestBody UpdateUserRequest request) {
        log.info("PUT /api/v1/users/{} - Update user", id);
        
        try {
            // ID is now a String (Keycloak ID)
            User user = User.builder()
                    .email(request.getEmail())
                    .firstName(request.getFirstName())
                    .lastName(request.getLastName())
                    .phone(request.getPhone())
                    .address(request.getAddress())
                    .identityNumber(request.getIdentityNumber())
                    .status(request.getStatus())
                    .build();
            
            User updated = userService.updateUser(id, user);
            UserDto dto = buildUserDto(updated);
            
            return ResponseEntity.ok(BaseResponse.success(dto, "User updated successfully"));
        } catch (Exception e) {
            log.error("Error updating user with ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(BaseResponse.error("Error updating user: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a user")
    public ResponseEntity<BaseResponse<Void>> deleteUser(@PathVariable String id) {
        log.info("DELETE /api/v1/users/{} - Delete user", id);
        
        try {
            // ID is now a String (Keycloak ID)
            userService.deleteUser(id);
            return ResponseEntity.ok(BaseResponse.success(null, "User deleted successfully"));
        } catch (Exception e) {
            log.error("Error deleting user with ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(BaseResponse.error("Error deleting user: " + e.getMessage()));
        }
    }

    @PostMapping("/sync")
    @Operation(summary = "Sync user from Keycloak")
    public ResponseEntity<BaseResponse<UserDto>> upsertByKeycloakId(@Valid @RequestBody SyncUserRequest request) {
        log.info("POST /api/v1/users/sync - Sync user: keycloakId={}", request.getKeycloakId());
        
        User result = userService.upsertByKeycloakId(
            request.getKeycloakId(),
            request.getUsername(),
            request.getEmail(),
            request.getFirstName(),
            request.getLastName()
        );
        
        UserDto dto = buildUserDto(result);
        return ResponseEntity.ok(BaseResponse.success(dto, "User synced successfully"));
    }

    private UserDto buildUserDto(User user) {
        return buildUserDto(user, null);
    }
    
    private UserDto buildUserDto(User user, List<String> roles) {
        if (user == null) {
            return null;
        }

        // If roles are provided (from batch fetch), use them
        // Otherwise, fetch roles individually (for single user requests)
        if (roles == null) {
            roles = Collections.emptyList();
            String userId = user.getId();
            if (userId != null && !userId.isBlank()) {
                try {
                    roles = externalAuthFacade.getUserRoles(userId);
                } catch (Exception e) {
                    log.warn("Failed to load roles for user {} (id={}): {}", user.getUsername(), userId, e.getMessage());
                }
            }
        }

        return UserDto.from(user, roles);
    }
}
