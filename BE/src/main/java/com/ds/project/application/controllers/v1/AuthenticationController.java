package com.ds.project.application.controllers.v1;

import com.ds.project.application.annotations.PublicRoute;
import com.ds.project.business.v1.services.UserService;
import com.ds.project.common.entities.common.UserPayload;
import com.ds.project.common.entities.dto.request.LoginRequest;
import com.ds.project.common.entities.dto.response.LoginResponse;
import com.ds.project.common.entities.dto.response.UserResponse;
import com.ds.project.common.utils.JwtUtils;
import com.ds.project.common.utils.ResponseUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Authentication controller
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthenticationController {
    
    private final UserService userService;
    private final JwtUtils jwtUtils;
    
    /**
     * Login endpoint
     */
    @PostMapping("/login")
    @PublicRoute
    public ResponseEntity<Map<String, Object>> login(@Valid @RequestBody LoginRequest loginRequest) {
        log.info("Login attempt for: {}", loginRequest.getEmailOrUsername());
        
        try {
            // Authenticate user
            UserResponse userResponse = userService.authenticateByEmailOrUsername(
                loginRequest.getEmailOrUsername(), 
                loginRequest.getPassword()
            ).orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));
            
            // Create user payload for JWT
            UserPayload userPayload = UserPayload.builder()
                .userId(userResponse.getId())
                .username(userResponse.getUsername())
                .email(userResponse.getEmail())
                .firstName(userResponse.getFirstName())
                .lastName(userResponse.getLastName())
                .roles(List.copyOf(userResponse.getRoles()))
                .build();
            
            // Generate JWT token
            String token = jwtUtils.generateToken(userPayload, loginRequest.isRememberMe());
            
            // Calculate expiration time
            long expiresIn = loginRequest.isRememberMe() ? 
                jwtUtils.getExpirationTime(true) : jwtUtils.getExpirationTime(false);
            
            // Create login response
            LoginResponse loginResponse = LoginResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .expiresIn(expiresIn)
                .user(userResponse)
                .roles(List.copyOf(userResponse.getRoles()))
                .build();
            
            log.info("Login successful for user: {}", userResponse.getUsername());
            return ResponseUtils.success(loginResponse, "Login successful");
            
        } catch (IllegalArgumentException e) {
            log.warn("Login failed for {}: {}", loginRequest.getEmailOrUsername(), e.getMessage());
            return ResponseUtils.error("Invalid credentials", HttpStatus.UNAUTHORIZED);
        } catch (Exception e) {
            log.error("Login error for {}: {}", loginRequest.getEmailOrUsername(), e.getMessage(), e);
            return ResponseUtils.error("Login failed", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    /**
     * Get current user info from token
     */
    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getCurrentUser(@RequestHeader("Authorization") String token) {
        try {
            // Remove "Bearer " prefix
            if (token.startsWith("Bearer ")) {
                token = token.substring(7);
            }
            
            // Validate token
            if (!jwtUtils.validateToken(token)) {
                return ResponseUtils.error("Invalid token", HttpStatus.UNAUTHORIZED);
            }
            
            // Extract user payload
            UserPayload userPayload = jwtUtils.getUserPayloadFromToken(token);
            
            return ResponseUtils.success(userPayload, "User info retrieved successfully");
            
        } catch (Exception e) {
            log.error("Error getting current user: {}", e.getMessage(), e);
            return ResponseUtils.error("Failed to get user info", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
