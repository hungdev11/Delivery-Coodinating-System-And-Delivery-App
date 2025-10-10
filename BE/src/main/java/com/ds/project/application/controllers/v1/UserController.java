package com.ds.project.application.controllers.v1;

import com.ds.project.common.entities.dto.request.UserRequest;
import com.ds.project.common.entities.dto.response.UserResponse;
import com.ds.project.common.utils.ResponseUtils;
import com.ds.project.application.annotations.AuthRequired;
import com.ds.project.application.annotations.PublicRoute;
import com.ds.project.common.interfaces.IUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.List;
import java.util.Map;

/**
 * REST Controller for User operations
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    
    private final IUserService userService;
    
    @PostMapping
    @PublicRoute
    public ResponseEntity<Map<String, Object>> createUser(@Valid @RequestBody UserRequest userRequest) {
        try {
            UserResponse createdUser = userService.createUser(userRequest);
            return ResponseUtils.success(createdUser, "User created successfully");
        } catch (Exception e) {
            return ResponseUtils.error("Failed to create user: " + e.getMessage(), 
                org.springframework.http.HttpStatus.BAD_REQUEST);
        }
    }
    
    @GetMapping("/{id}")
    @AuthRequired
    public ResponseEntity<Map<String, Object>> getUserById(@PathVariable String id) {
        return userService.getUserById(id)
            .map(user -> ResponseUtils.success(user))
            .orElse(ResponseUtils.error("User not found", org.springframework.http.HttpStatus.NOT_FOUND));
    }
    
    @GetMapping
    @AuthRequired
    public ResponseEntity<Map<String, Object>> getAllUsers() {
        List<UserResponse> users = userService.getAllUsers();
        return ResponseUtils.success(users);
    }
    
    @PutMapping("/{id}")
    @AuthRequired
    public ResponseEntity<Map<String, Object>> updateUser(@PathVariable String id, @Valid @RequestBody UserRequest userRequest) {
        try {
            UserResponse updatedUser = userService.updateUser(id, userRequest);
            return ResponseUtils.success(updatedUser, "User updated successfully");
        } catch (Exception e) {
            return ResponseUtils.error("Failed to update user: " + e.getMessage(), 
                org.springframework.http.HttpStatus.BAD_REQUEST);
        }
    }
    
    @DeleteMapping("/{id}")
    @AuthRequired
    public ResponseEntity<Map<String, Object>> deleteUser(@PathVariable String id) {
        try {
            userService.deleteUser(id);
            return ResponseUtils.success(null, "User deleted successfully");
        } catch (Exception e) {
            return ResponseUtils.error("Failed to delete user: " + e.getMessage(), 
                org.springframework.http.HttpStatus.BAD_REQUEST);
        }
    }
    
    @PostMapping("/{userId}/roles/{roleId}")
    @AuthRequired
    public ResponseEntity<Map<String, Object>> assignRole(@PathVariable String userId, @PathVariable String roleId) {
        try {
            UserResponse updatedUser = userService.assignRole(userId, roleId);
            return ResponseUtils.success(updatedUser, "Role assigned successfully");
        } catch (Exception e) {
            return ResponseUtils.error("Failed to assign role: " + e.getMessage(), 
                org.springframework.http.HttpStatus.BAD_REQUEST);
        }
    }
    
    @DeleteMapping("/{userId}/roles/{roleId}")
    @AuthRequired
    public ResponseEntity<Map<String, Object>> removeRole(@PathVariable String userId, @PathVariable String roleId) {
        try {
            UserResponse updatedUser = userService.removeRole(userId, roleId);
            return ResponseUtils.success(updatedUser, "Role removed successfully");
        } catch (Exception e) {
            return ResponseUtils.error("Failed to remove role: " + e.getMessage(), 
                org.springframework.http.HttpStatus.BAD_REQUEST);
        }
    }
}
