package com.ds.project.application.controllers.v1;

import com.ds.project.common.entities.base.BaseResponse;
import com.ds.project.common.entities.base.PagedData;
import com.ds.project.common.entities.base.Page;
import com.ds.project.common.entities.dto.UserDto;
import com.ds.project.common.entities.dto.request.UserRequest;
import com.ds.project.common.utils.ResponseUtils;
import com.ds.project.application.annotations.AuthRequired;
import com.ds.project.application.annotations.PublicRoute;
import com.ds.project.common.interfaces.IUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

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
        BaseResponse<UserDto> response = userService.createUser(userRequest);
        
        if (response.getResult().isPresent()) {
            return ResponseUtils.success(response.getResult().get());
        } else {
            return ResponseUtils.error(response.getMessage().orElse("Failed to create user"), 
                org.springframework.http.HttpStatus.BAD_REQUEST);
        }
    }
    
    @GetMapping("/{id}")
    @AuthRequired
    public ResponseEntity<Map<String, Object>> getUserById(@PathVariable String id) {
        return userService.getUserById(id)
            .map(response -> {
                if (response.getResult().isPresent()) {
                    return ResponseUtils.success(response.getResult().get());
                } else {
                    return ResponseUtils.error(response.getMessage().orElse("User not found"), 
                        org.springframework.http.HttpStatus.NOT_FOUND);
                }
            })
            .orElse(ResponseUtils.error("User not found", org.springframework.http.HttpStatus.NOT_FOUND));
    }
    
    @GetMapping
    @AuthRequired
    public ResponseEntity<Map<String, Object>> getAllUsers() {
        BaseResponse<PagedData<Page, UserDto>> response = userService.getAllUsers();
        
        if (response.getResult().isPresent()) {
            return ResponseUtils.success(response.getResult().get());
        } else {
            return ResponseUtils.error(response.getMessage().orElse("Failed to get users"), 
                org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    @PutMapping("/{id}")
    @AuthRequired
    public ResponseEntity<Map<String, Object>> updateUser(@PathVariable String id, @Valid @RequestBody UserRequest userRequest) {
        BaseResponse<UserDto> response = userService.updateUser(id, userRequest);
        
        if (response.getResult().isPresent()) {
            return ResponseUtils.success(response.getResult().get());
        } else {
            return ResponseUtils.error(response.getMessage().orElse("Failed to update user"), 
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
        BaseResponse<UserDto> response = userService.assignRole(userId, roleId);
        
        if (response.getResult().isPresent()) {
            return ResponseUtils.success(response.getResult().get());
        } else {
            return ResponseUtils.error(response.getMessage().orElse("Failed to assign role"), 
                org.springframework.http.HttpStatus.BAD_REQUEST);
        }
    }
    
    @DeleteMapping("/{userId}/roles/{roleId}")
    @AuthRequired
    public ResponseEntity<Map<String, Object>> removeRole(@PathVariable String userId, @PathVariable String roleId) {
        BaseResponse<UserDto> response = userService.removeRole(userId, roleId);
        
        if (response.getResult().isPresent()) {
            return ResponseUtils.success(response.getResult().get());
        } else {
            return ResponseUtils.error(response.getMessage().orElse("Failed to remove role"), 
                org.springframework.http.HttpStatus.BAD_REQUEST);
        }
    }
}
