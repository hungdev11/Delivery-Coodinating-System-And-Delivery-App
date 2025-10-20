package com.ds.user.application.controllers.v1;

import com.ds.user.common.entities.base.User;
import com.ds.user.common.entities.common.BaseResponse;
import com.ds.user.common.entities.common.PagingRequest;
import com.ds.user.common.entities.common.paging.PagedData;
import com.ds.user.common.entities.dto.auth.SyncUserRequest;
import com.ds.user.common.entities.dto.user.CreateUserRequest;
import com.ds.user.common.entities.dto.user.UpdateUserRequest;
import com.ds.user.common.entities.dto.user.UserDto;
import com.ds.user.common.interfaces.IUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

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

    @PostMapping("/create")
    @Operation(summary = "Create a new user")
    public ResponseEntity<BaseResponse<UserDto>> createUser(@Valid @RequestBody CreateUserRequest request) {
        log.info("POST /api/v1/users/create - Create user: username={}", request.getUsername());
        
        User user = User.builder()
                .keycloakId(request.getKeycloakId())
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
        UserDto dto = UserDto.from(created);
        
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(BaseResponse.success(dto, "User created successfully"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get user by ID")
    public ResponseEntity<BaseResponse<UserDto>> getUser(@PathVariable String id) {
        log.info("GET /api/v1/users/{} - Get user by ID", id);
        
        try {
            UUID userId = parseUserId(id);
            log.debug("Parsed user ID: {} -> {}", id, userId);
            
            Optional<User> userOpt = userService.getUser(userId);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                UserDto dto = UserDto.from(user);
                log.info("Found user: {} with ID: {} (original request: {})", user.getUsername(), user.getId(), id);
                return ResponseEntity.ok(BaseResponse.success(dto));
            } else {
                log.warn("User not found with ID: {} (original request: {})", userId, id);
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
                .map(user -> ResponseEntity.ok(BaseResponse.success(UserDto.from(user))))
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
            
            // Convert to PagedData<UserDto>
            List<UserDto> userDtos = userPage.getData().stream()
                    .map(UserDto::from)
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
            UUID userId = parseUserId(id);
            User user = User.builder()
                    .email(request.getEmail())
                    .firstName(request.getFirstName())
                    .lastName(request.getLastName())
                    .phone(request.getPhone())
                    .address(request.getAddress())
                    .identityNumber(request.getIdentityNumber())
                    .status(request.getStatus())
                    .build();
            
            User updated = userService.updateUser(userId, user);
            UserDto dto = UserDto.from(updated);
            
            return ResponseEntity.ok(BaseResponse.success(dto, "User updated successfully"));
        } catch (IllegalArgumentException e) {
            log.error("Invalid user ID format: {} - Error: {}", id, e.getMessage());
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(BaseResponse.error("Invalid user ID format. Expected UUID or numeric ID."));
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a user")
    public ResponseEntity<BaseResponse<Void>> deleteUser(@PathVariable String id) {
        log.info("DELETE /api/v1/users/{} - Delete user", id);
        
        try {
            UUID userId = parseUserId(id);
            userService.deleteUser(userId);
            return ResponseEntity.ok(BaseResponse.success(null, "User deleted successfully"));
        } catch (IllegalArgumentException e) {
            log.error("Invalid user ID format: {} - Error: {}", id, e.getMessage());
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(BaseResponse.error("Invalid user ID format. Expected UUID or numeric ID."));
        }
    }

    /**
     * Parse user ID from string, handling both UUID and numeric formats
     */
    private UUID parseUserId(String id) throws IllegalArgumentException {
        try {
            return UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            // If not a valid UUID, try to handle as numeric ID
            try {
                int numericId = Integer.parseInt(id);
                // Convert numeric ID to a predictable UUID format
                // This is a temporary solution for legacy numeric IDs
                return UUID.fromString(String.format("00000000-0000-0000-0000-%012d", numericId));
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException("Invalid user ID format. Expected UUID or numeric ID.");
            }
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
        
        UserDto dto = UserDto.from(result);
        return ResponseEntity.ok(BaseResponse.success(dto, "User synced successfully"));
    }
}
