package com.ds.gateway.controllers.v1;

import com.ds.gateway.common.entities.dto.auth.*;
import com.ds.gateway.common.entities.dto.common.BaseResponse;
import com.ds.gateway.common.entities.dto.user.UserDto;
import com.ds.gateway.common.interfaces.IAuthServiceClient;
import com.ds.gateway.annotations.PublicRoute;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.CompletableFuture;

/**
 * Auth controller for handling authentication requests
 * All routes are public (no authentication required)
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    
    @Autowired
    private IAuthServiceClient authServiceClient;
    
    @PublicRoute
    @PostMapping("/login")
    public CompletableFuture<ResponseEntity<BaseResponse<LoginResponseDto>>> login(
            @Valid @RequestBody LoginRequestDto request) {
        log.info("Login request for username: {}", request.getUsername());
        
        return authServiceClient.login(request)
            .thenApply(response -> ResponseEntity.ok(BaseResponse.success("Login successful", response)))
            .exceptionally(ex -> {
                log.error("Login failed: {}", ex.getMessage());
                return ResponseEntity.badRequest().body(BaseResponse.error("Login failed: " + ex.getMessage()));
            });
    }
    
    @PublicRoute
    @PostMapping("/refresh-token")
    public CompletableFuture<ResponseEntity<BaseResponse<RefreshTokenResponseDto>>> refreshToken(
            @Valid @RequestBody RefreshTokenRequestDto request) {
        log.info("Refresh token request");
        
        return authServiceClient.refreshToken(request)
            .thenApply(response -> ResponseEntity.ok(BaseResponse.success("Token refreshed", response)))
            .exceptionally(ex -> {
                log.error("Token refresh failed: {}", ex.getMessage());
                return ResponseEntity.badRequest().body(BaseResponse.error("Token refresh failed: " + ex.getMessage()));
            });
    }
    
    @PublicRoute
    @PostMapping("/logout")
    public CompletableFuture<ResponseEntity<BaseResponse<Void>>> logout(
            @Valid @RequestBody RefreshTokenRequestDto request) {
        log.info("Logout request");
        
        return authServiceClient.logout(request.getRefreshToken())
            .thenApply(v -> ResponseEntity.ok(BaseResponse.<Void>success("Logout successful", null)))
            .exceptionally(ex -> {
                log.error("Logout failed: {}", ex.getMessage());
                return ResponseEntity.badRequest().body(BaseResponse.error("Logout failed: " + ex.getMessage()));
            });
    }
    
    @PublicRoute
    @PostMapping("/register")
    public CompletableFuture<ResponseEntity<BaseResponse<UserDto>>> register(
            @Valid @RequestBody RegisterRequestDto request) {
        log.info("Register request for username: {}", request.getUsername());
        
        return authServiceClient.register(request)
            .thenApply(user -> ResponseEntity.ok(BaseResponse.success("Registration successful", user)))
            .exceptionally(ex -> {
                log.error("Registration failed: {}", ex.getMessage());
                return ResponseEntity.badRequest().body(BaseResponse.error("Registration failed: " + ex.getMessage()));
            });
    }
}
