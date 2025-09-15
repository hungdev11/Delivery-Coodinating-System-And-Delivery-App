package knp.ptithcm.datn.user_module.modules.user.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.validation.Valid;
import knp.ptithcm.datn.user_module.modules.user.dtos.requests.LoginRequest;
import knp.ptithcm.datn.user_module.modules.user.dtos.requests.RefreshTokenRequest;
import knp.ptithcm.datn.user_module.modules.user.dtos.requests.LogoutRequest;
import knp.ptithcm.datn.user_module.modules.user.dtos.responses.LoginResponse;
import knp.ptithcm.datn.user_module.modules.user.dtos.responses.RefreshTokenResponse;
import knp.ptithcm.datn.user_module.modules.user.dtos.responses.LogoutResponse;
import knp.ptithcm.datn.user_module.modules.user.services.UserService;
import knp.ptithcm.datn.user_module.modules.common.dtos.ApiResponse;

/**
 * Controller xử lý authentication (login, logout, refresh token)
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private static final Logger log = LoggerFactory.getLogger(AuthController.class);
    
    @Autowired
    private UserService userService;
    
    /**
     * Đăng nhập user
     * @param request LoginRequest chứa username và password
     * @return ResponseEntity<ApiResponse<LoginResponse>>
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        log.debug("[AuthController] Login request for user: {}", request.getUsername());
        
        try {
            LoginResponse loginResponse = userService.login(request.getUsername(), request.getPassword());
            
            ApiResponse<LoginResponse> response = new ApiResponse<>(
                HttpStatus.OK, 
                "Đăng nhập thành công", 
                loginResponse
            );
            
            log.info("[AuthController] Login successful for user: {}", request.getUsername());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("[AuthController] Login failed for user: {}, error: {}", request.getUsername(), e.getMessage(), e);
            
            ApiResponse<LoginResponse> response = new ApiResponse<>(
                HttpStatus.UNAUTHORIZED, 
                "Đăng nhập thất bại: " + e.getMessage(), 
                null
            );
            
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
    }
    
    /**
     * Kiểm tra trạng thái authentication
     * @return ResponseEntity<ApiResponse<String>>
     */
    @GetMapping("/hello")
    public ResponseEntity<ApiResponse<String>> hello() {
        log.debug("[AuthController] Hello endpoint called");
        
        ApiResponse<String> response = new ApiResponse<>(
            HttpStatus.OK, 
            "Authentication service is running", 
            "Hello from Auth Service!"
        );
        
        return ResponseEntity.ok(response);
    }

    /**
     * Refresh access token
     * @param request RefreshTokenRequest chứa refresh token
     * @return ResponseEntity<ApiResponse<RefreshTokenResponse>>
     */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<RefreshTokenResponse>> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        log.debug("[AuthController] Refresh token request");
        
        try {
            RefreshTokenResponse refreshResponse = userService.refreshToken(request.getRefreshToken());
            
            ApiResponse<RefreshTokenResponse> response = new ApiResponse<>(
                HttpStatus.OK, 
                "Refresh token thành công", 
                refreshResponse
            );
            
            log.info("[AuthController] Token refreshed successfully");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("[AuthController] Token refresh failed: {}", e.getMessage(), e);
            
            ApiResponse<RefreshTokenResponse> response = new ApiResponse<>(
                HttpStatus.UNAUTHORIZED, 
                "Refresh token thất bại: " + e.getMessage(), 
                null
            );
            
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
    }

    /**
     * Logout user
     * @param request LogoutRequest chứa refresh token
     * @return ResponseEntity<ApiResponse<LogoutResponse>>
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<LogoutResponse>> logout(@Valid @RequestBody LogoutRequest request) {
        log.debug("[AuthController] Logout request");
        
        try {
            LogoutResponse logoutResponse = userService.logout(request.getRefreshToken());
            
            ApiResponse<LogoutResponse> response = new ApiResponse<>(
                HttpStatus.OK, 
                "Đăng xuất thành công", 
                logoutResponse
            );
            
            log.info("[AuthController] User logged out successfully");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("[AuthController] Logout failed: {}", e.getMessage(), e);
            
            ApiResponse<LogoutResponse> response = new ApiResponse<>(
                HttpStatus.INTERNAL_SERVER_ERROR, 
                "Đăng xuất thất bại: " + e.getMessage(), 
                null
            );
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}
