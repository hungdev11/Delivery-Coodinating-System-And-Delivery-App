package com.ds.gateway.application.controllers.v1;

import com.ds.gateway.common.entities.dto.common.BaseResponse;
import com.ds.gateway.common.entities.dto.common.PagedData;
import com.ds.gateway.common.entities.dto.common.PagingRequest;
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
        
        log.debug("[api-gateway] [UserController.getCurrentUser] Get current user: {}", currentUser.getUserId());
        
        return userServiceClient.getUserByUsername(currentUser.getUsername())
            .thenApply(user -> ResponseEntity.ok(BaseResponse.success(user)))
            .exceptionally(ex -> {
                log.error("[api-gateway] [UserController.getCurrentUser] Failed to get current user", ex);
                return ResponseEntity.badRequest().body(BaseResponse.error("Failed to get user: " + ex.getMessage()));
            });
    }

    @PostMapping
    @AuthRequired({"ADMIN", "MANAGER"})
    public CompletableFuture<ResponseEntity<BaseResponse<PagedData<UserDto>>>> getUsers(
            @RequestBody PagingRequest query) {
        log.debug("[api-gateway] [UserController.getUsers] Get users (POST) with query");
        return userServiceClient.getUsers(query)
            .thenApply(pagedUsers -> ResponseEntity.ok(BaseResponse.success(pagedUsers)))
            .exceptionally(ex -> {
                log.error("[api-gateway] [UserController.getUsers] Failed to get users", ex);
                return ResponseEntity.badRequest().body(BaseResponse.error("Failed to get users: " + ex.getMessage()));
            });
    }
    
    @GetMapping("/{id}")
    public CompletableFuture<ResponseEntity<BaseResponse<UserDto>>> getUserById(@PathVariable String id) {
        log.debug("[api-gateway] [UserController.getUserById] Get user by ID: {}", id);
        
        return userServiceClient.getUserById(id)
            .thenApply(user -> ResponseEntity.ok(BaseResponse.success(user)))
            .exceptionally(ex -> {
                log.error("[api-gateway] [UserController.getUserById] Failed to get user", ex);
                return ResponseEntity.badRequest().body(BaseResponse.error("Failed to get user: " + ex.getMessage()));
            });
    }
    
    @GetMapping("/username/{username}")
    public CompletableFuture<ResponseEntity<BaseResponse<UserDto>>> getUserByUsername(@PathVariable String username) {
        log.debug("[api-gateway] [UserController.getUserByUsername] Get user by username: {}", username);
        
        return userServiceClient.getUserByUsername(username)
            .thenApply(user -> ResponseEntity.ok(BaseResponse.success(user)))
            .exceptionally(ex -> {
                log.error("[api-gateway] [UserController.getUserByUsername] Failed to get user by username", ex);
                return ResponseEntity.badRequest().body(BaseResponse.error("Failed to get user: " + ex.getMessage()));
            });
    }
    
    @PostMapping("/create")
    @AuthRequired({"ADMIN"})
    public CompletableFuture<ResponseEntity<BaseResponse<UserDto>>> createUser(
            @Valid @RequestBody CreateUserRequestDto request) {
        log.debug("[api-gateway] [UserController.createUser] Create user: {}", request.getUsername());
        
        return userServiceClient.createUser(request)
            .thenApply(user -> ResponseEntity.ok(BaseResponse.success(user, "User created successfully")))
            .exceptionally(ex -> {
                log.error("[api-gateway] [UserController.createUser] Failed to create user", ex);
                return ResponseEntity.badRequest().body(BaseResponse.error("Failed to create user: " + ex.getMessage()));
            });
    }
    
    @PutMapping("/{id}")
    public CompletableFuture<ResponseEntity<BaseResponse<UserDto>>> updateUser(
            @PathVariable String id,
            @Valid @RequestBody UpdateUserRequestDto request) {
        log.debug("[api-gateway] [UserController.updateUser] Update user: {}", id);
        
        return userServiceClient.updateUser(id, request)
            .thenApply(user -> ResponseEntity.ok(BaseResponse.success(user, "User updated successfully")))
            .exceptionally(ex -> {
                log.error("[api-gateway] [UserController.updateUser] Failed to update user", ex);
                return ResponseEntity.badRequest().body(BaseResponse.error("Failed to update user: " + ex.getMessage()));
            });
    }
    
    @DeleteMapping("/{id}")
    @AuthRequired({"ADMIN"})
    public CompletableFuture<ResponseEntity<BaseResponse<Void>>> deleteUser(@PathVariable String id) {
        log.debug("[api-gateway] [UserController.deleteUser] Delete user: {}", id);
        
        return userServiceClient.deleteUser(id)
            .thenApply(v -> ResponseEntity.ok(BaseResponse.<Void>success(null, "User deleted successfully")))
            .exceptionally(ex -> {
                log.error("[api-gateway] [UserController.deleteUser] Failed to delete user", ex);
                return ResponseEntity.badRequest().body(BaseResponse.error("Failed to delete user: " + ex.getMessage()));
            });
    }
}
