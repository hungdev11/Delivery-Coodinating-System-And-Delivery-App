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
import com.ds.user.common.interfaces.IDeliveryManService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
    private final IDeliveryManService deliveryManService;

    @PostMapping("/create")
    @Operation(summary = "Create a new user")
    public ResponseEntity<BaseResponse<UserDto>> createUser(@Valid @RequestBody CreateUserRequest request) {
        log.debug("Create user: username={}", request.getUsername());

        // Note: User ID must be set from Keycloak ID before creating
        // The request should include the Keycloak ID which will be used as the primary
        // key
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
                .body(BaseResponse.success(dto));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get user by ID")
    public ResponseEntity<BaseResponse<UserDto>> getUser(@PathVariable String id) {
        log.debug("Get user by ID: {}", id);

        try {
            // ID is now a String (Keycloak ID)
            Optional<User> userOpt = userService.getUser(id);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                UserDto dto = buildUserDto(user);
                log.debug("Found user: {} with ID: {}", user.getUsername(), user.getId());
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
    public ResponseEntity<BaseResponse<UserDto>> getCurrentUser(
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        log.debug("Get current user, header userId={}", userId);

        if (userId == null || userId.isBlank()) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(BaseResponse.error("Missing X-User-Id header"));
        }

        try {
            return userService.getUser(userId)
                    .map(user -> ResponseEntity.ok(BaseResponse.success(buildUserDto(user))))
                    .orElseGet(() -> ResponseEntity
                            .status(HttpStatus.NOT_FOUND)
                            .body(BaseResponse.error("User not found")));
        } catch (Exception e) {
            log.error("Error getting current user {}: {}", userId, e.getMessage(), e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(BaseResponse.error("Failed to load current user: " + e.getMessage()));
        }
    }

    @GetMapping("/username/{username}")
    @Operation(summary = "Get user by username")
    public ResponseEntity<BaseResponse<UserDto>> getUserByUsername(@PathVariable String username) {
        log.debug("Get user by username: {}", username);

        return userService.getUserByUsername(username)
                .map(user -> ResponseEntity.ok(BaseResponse.success(buildUserDto(user))))
                .orElse(ResponseEntity
                        .status(HttpStatus.NOT_FOUND)
                        .body(BaseResponse.error("User not found")));
    }

    @PostMapping
    @Operation(summary = "Get all users with advanced filtering and sorting")
    public ResponseEntity<BaseResponse<PagedData<UserDto>>> getUsers(@Valid @RequestBody PagingRequest query) {
        log.debug("Get users with advanced filtering and sorting");
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
        log.debug("Update user: {}", id);

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

            return ResponseEntity.ok(BaseResponse.success(dto));
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
        log.debug("Delete user: {}", id);

        try {
            // ID is now a String (Keycloak ID)
            userService.deleteUser(id);
            return ResponseEntity.ok(BaseResponse.success(null));
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
        log.debug("Sync user: keycloakId={}", request.getKeycloakId());

        User result = userService.upsertByKeycloakId(
                request.getKeycloakId(),
                request.getUsername(),
                request.getEmail(),
                request.getFirstName(),
                request.getLastName());

        UserDto dto = buildUserDto(result);
        return ResponseEntity.ok(BaseResponse.success(dto));
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
                    log.warn("Failed to load roles for user {} (id={}): {}", user.getUsername(), userId,
                            e.getMessage());
                }
            }
        }

        // Fetch delivery man info if user has SHIPPER role
        com.ds.user.common.entities.dto.deliveryman.DeliveryManDto deliveryMan = null;
        if (roles != null && roles.contains("SHIPPER") && user.getId() != null) {
            try {
                deliveryMan = deliveryManService.getDeliveryManByUserId(user.getId()).orElse(null);
            } catch (Exception e) {
                log.warn("Failed to load delivery man info for user {} (id={}): {}", user.getUsername(), user.getId(),
                        e.getMessage());
            }
        }

        return UserDto.from(user, roles, deliveryMan);
    }
}
