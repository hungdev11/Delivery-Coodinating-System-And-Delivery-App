package com.ds.user.application.controllers.v1;

import com.ds.user.app_context.models.User;
import com.ds.user.common.entities.dto.*;
import com.ds.user.common.entities.dto.auth.SyncUserRequest;
import com.ds.user.common.entities.dto.common.BaseResponse;
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
import java.util.UUID;
import java.util.stream.Collectors;

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

    @PostMapping
    @Operation(summary = "Create a new user")
    public ResponseEntity<BaseResponse<UserDto>> createUser(@Valid @RequestBody CreateUserRequest request) {
        log.info("POST /api/v1/users - Create user: username={}", request.getUsername());
        
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
    public ResponseEntity<BaseResponse<UserDto>> getUser(@PathVariable UUID id) {
        log.info("GET /api/v1/users/{} - Get user by ID", id);
        
        return userService.getUser(id)
                .map(user -> ResponseEntity.ok(BaseResponse.success(UserDto.from(user))))
                .orElse(ResponseEntity
                        .status(HttpStatus.NOT_FOUND)
                        .body(BaseResponse.error("User not found")));
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

    @GetMapping
    @Operation(summary = "Get all users")
    public ResponseEntity<BaseResponse<List<UserDto>>> listUsers() {
        log.info("GET /api/v1/users - Get all users");
        
        List<UserDto> users = userService.listUsers()
                .stream()
                .map(UserDto::from)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(BaseResponse.success(users));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a user")
    public ResponseEntity<BaseResponse<UserDto>> updateUser(
            @PathVariable UUID id, 
            @Valid @RequestBody UpdateUserRequest request) {
        log.info("PUT /api/v1/users/{} - Update user", id);
        
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
        UserDto dto = UserDto.from(updated);
        
        return ResponseEntity.ok(BaseResponse.success(dto, "User updated successfully"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a user")
    public ResponseEntity<BaseResponse<Void>> deleteUser(@PathVariable UUID id) {
        log.info("DELETE /api/v1/users/{} - Delete user", id);
        userService.deleteUser(id);
        return ResponseEntity.ok(BaseResponse.success(null, "User deleted successfully"));
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
