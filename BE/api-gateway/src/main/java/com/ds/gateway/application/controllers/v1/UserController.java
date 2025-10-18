package com.ds.gateway.application.controllers.v1;

import com.ds.gateway.common.entities.dto.common.BaseResponse;
import com.ds.gateway.common.entities.dto.common.PagedData;
import com.ds.gateway.common.entities.dto.user.CreateUserRequestDto;
import com.ds.gateway.common.entities.dto.user.UpdateUserRequestDto;
import com.ds.gateway.common.entities.dto.user.UserDto;
import com.ds.gateway.common.interfaces.IUserServiceClient;
import com.ds.gateway.annotations.AuthRequired;
import com.ds.gateway.application.security.UserContext;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.CompletableFuture;

/**
 * User controller for managing user operations
 * Requires authentication for all routes
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/users")
@AuthRequired
public class UserController {
    
    @Autowired
    private IUserServiceClient userServiceClient;
    
    @GetMapping("/me")
    public CompletableFuture<ResponseEntity<BaseResponse<UserDto>>> getCurrentUser() {
        UserContext currentUser = UserContext.getCurrentUser()
            .orElseThrow(() -> new RuntimeException("User not authenticated"));
        
        log.info("Get current user: {}", currentUser.getUserId());
        
        return userServiceClient.getUserByUsername(currentUser.getUsername())
            .thenApply(user -> ResponseEntity.ok(BaseResponse.success(user)))
            .exceptionally(ex -> {
                log.error("Failed to get current user: {}", ex.getMessage());
                return ResponseEntity.badRequest().body(BaseResponse.error("Failed to get user: " + ex.getMessage()));
            });
    }
    
    @GetMapping
    @AuthRequired({"ADMIN", "MANAGER"})
    public CompletableFuture<ResponseEntity<BaseResponse<PagedData<UserDto>>>> listUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("List users with pagination: page={}, size={}", page, size);
        
        return userServiceClient.listUsers(page, size)
            .thenApply(pagedUsers -> ResponseEntity.ok(BaseResponse.success(pagedUsers)))
            .exceptionally(ex -> {
                log.error("Failed to list users: {}", ex.getMessage());
                return ResponseEntity.badRequest().body(BaseResponse.error("Failed to list users: " + ex.getMessage()));
            });
    }
    
    @GetMapping("/{id}")
    public CompletableFuture<ResponseEntity<BaseResponse<UserDto>>> getUserById(@PathVariable String id) {
        log.info("Get user by ID: {}", id);
        
        return userServiceClient.getUserById(id)
            .thenApply(user -> ResponseEntity.ok(BaseResponse.success(user)))
            .exceptionally(ex -> {
                log.error("Failed to get user: {}", ex.getMessage());
                return ResponseEntity.badRequest().body(BaseResponse.error("Failed to get user: " + ex.getMessage()));
            });
    }
    
    @GetMapping("/username/{username}")
    public CompletableFuture<ResponseEntity<BaseResponse<UserDto>>> getUserByUsername(@PathVariable String username) {
        log.info("Get user by username: {}", username);
        
        return userServiceClient.getUserByUsername(username)
            .thenApply(user -> ResponseEntity.ok(BaseResponse.success(user)))
            .exceptionally(ex -> {
                log.error("Failed to get user by username: {}", ex.getMessage());
                return ResponseEntity.badRequest().body(BaseResponse.error("Failed to get user: " + ex.getMessage()));
            });
    }
    
    @PostMapping
    @AuthRequired({"ADMIN"})
    public CompletableFuture<ResponseEntity<BaseResponse<UserDto>>> createUser(
            @Valid @RequestBody CreateUserRequestDto request) {
        log.info("Create user: {}", request.getUsername());
        
        return userServiceClient.createUser(request)
            .thenApply(user -> ResponseEntity.ok(BaseResponse.success(user, "User created successfully")))
            .exceptionally(ex -> {
                log.error("Failed to create user: {}", ex.getMessage());
                return ResponseEntity.badRequest().body(BaseResponse.error("Failed to create user: " + ex.getMessage()));
            });
    }
    
    @PutMapping("/{id}")
    public CompletableFuture<ResponseEntity<BaseResponse<UserDto>>> updateUser(
            @PathVariable String id,
            @Valid @RequestBody UpdateUserRequestDto request) {
        log.info("Update user: {}", id);
        
        return userServiceClient.updateUser(id, request)
            .thenApply(user -> ResponseEntity.ok(BaseResponse.success(user, "User updated successfully")))
            .exceptionally(ex -> {
                log.error("Failed to update user: {}", ex.getMessage());
                return ResponseEntity.badRequest().body(BaseResponse.error("Failed to update user: " + ex.getMessage()));
            });
    }
    
    @DeleteMapping("/{id}")
    @AuthRequired({"ADMIN"})
    public CompletableFuture<ResponseEntity<BaseResponse<Void>>> deleteUser(@PathVariable String id) {
        log.info("Delete user: {}", id);
        
        return userServiceClient.deleteUser(id)
            .thenApply(v -> ResponseEntity.ok(BaseResponse.<Void>success(null, "User deleted successfully")))
            .exceptionally(ex -> {
                log.error("Failed to delete user: {}", ex.getMessage());
                return ResponseEntity.badRequest().body(BaseResponse.error("Failed to delete user: " + ex.getMessage()));
            });
    }
}
