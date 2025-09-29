package ptithcm.graduation.apigateway.controllers.v1.auth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ptithcm.graduation.apigateway.annotations.AuthRequired;
import ptithcm.graduation.apigateway.annotations.PublicRoute;
import ptithcm.graduation.apigateway.models.BaseResponse;
import ptithcm.graduation.apigateway.services.v1.auth.dto.*;
import ptithcm.graduation.apigateway.services.v1.auth.interfaces.IAuthService;
import jakarta.validation.Valid;
import java.util.concurrent.CompletableFuture;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@AuthRequired
public class AuthController {

    private final IAuthService authService;

    /**
     * User login endpoint
     */
    @PublicRoute
    @PostMapping("/login")
    public CompletableFuture<ResponseEntity<LoginResponseDto>> login(@Valid @RequestBody LoginRequestDto request) {
        log.info("Login request received for user: {}", request.getUsername());
        return authService.login(request)
                .thenApply(ResponseEntity::ok)
                .exceptionally(throwable -> {
                    log.error("Login failed for user: {}", request.getUsername(), throwable);
                    LoginResponseDto errorResponse = LoginResponseDto.builder()
                            .message("Login failed: " + throwable.getMessage())
                            .build();
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
                });
    }

    /**
     * Phone registration endpoint
     */
    @PublicRoute
    @PostMapping("/register/phone")
    public CompletableFuture<ResponseEntity<BaseResponse<AuthDto>>> registerByPhone(@Valid @RequestBody RegisterByPhoneRequestDto request) {
        log.info("Phone registration request received for phone: {}", request.getPhone());
        return authService.registerByPhone(request)
                .thenApply(ResponseEntity::ok)
                .exceptionally(throwable -> {
                    log.error("Phone registration failed for phone: {}", request.getPhone(), throwable);
                    String errorMessage = throwable.getCause() instanceof UnsupportedOperationException 
                        ? throwable.getCause().getMessage() 
                        : "Phone registration failed: " + throwable.getMessage();
                    BaseResponse<AuthDto> errorResponse = new BaseResponse<>(errorMessage, null);
                    return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(errorResponse);
                });
    }

    /**
     * User logout endpoint
     */
    @PostMapping("/logout")
    public CompletableFuture<ResponseEntity<BaseResponse<String>>> logout(@Valid @RequestBody RefreshTokenRequestDto request) {
        log.info("Logout request received");
        return authService.logout(request)
                .thenApply(ResponseEntity::ok)
                .exceptionally(throwable -> {
                    log.error("Logout failed", throwable);
                    String errorMessage = throwable.getCause() instanceof UnsupportedOperationException 
                        ? throwable.getCause().getMessage() 
                        : "Logout failed: " + throwable.getMessage();
                    BaseResponse<String> errorResponse = new BaseResponse<>(errorMessage, null);
                    return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(errorResponse);
                });
    }

    /**
     * Refresh token endpoint
     */
    @PublicRoute
    @PostMapping("/refresh")
    public CompletableFuture<ResponseEntity<LoginResponseDto>> refreshToken(@Valid @RequestBody RefreshTokenRequestDto request) {
        log.info("Token refresh request received");
        return authService.refreshToken(request)
                .thenApply(ResponseEntity::ok)
                .exceptionally(throwable -> {
                    log.error("Token refresh failed", throwable);
                    String errorMessage = throwable.getCause() instanceof UnsupportedOperationException 
                        ? throwable.getCause().getMessage() 
                        : "Token refresh failed: " + throwable.getMessage();
                    LoginResponseDto errorResponse = LoginResponseDto.builder()
                            .message(errorMessage)
                            .build();
                    return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(errorResponse);
                });
    }

    /**
     * Send OTP endpoint
     */
    @PublicRoute
    @PostMapping("/otp/send")
    public CompletableFuture<ResponseEntity<BaseResponse<String>>> sendOtp(@Valid @RequestBody OtpRequestDto request) {
        log.info("Send OTP request received for phone: {}", request.getPhone());
        return authService.sendOtp(request)
                .thenApply(ResponseEntity::ok)
                .exceptionally(throwable -> {
                    log.error("Send OTP failed for phone: {}", request.getPhone(), throwable);
                    String errorMessage = throwable.getCause() instanceof UnsupportedOperationException 
                        ? throwable.getCause().getMessage() 
                        : "Send OTP failed: " + throwable.getMessage();
                    BaseResponse<String> errorResponse = new BaseResponse<>(errorMessage, null);
                    return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(errorResponse);
                });
    }

    /**
     * Verify OTP endpoint
     */
    @PublicRoute
    @PostMapping("/otp/verify")
    public CompletableFuture<ResponseEntity<BaseResponse<Boolean>>> verifyOtp(@Valid @RequestBody VerifyOtpRequestDto request) {
        log.info("Verify OTP request received for phone: {}", request.getPhone());
        return authService.verifyOtp(request)
                .thenApply(ResponseEntity::ok)
                .exceptionally(throwable -> {
                    log.error("Verify OTP failed for phone: {}", request.getPhone(), throwable);
                    String errorMessage = throwable.getCause() instanceof UnsupportedOperationException 
                        ? throwable.getCause().getMessage() 
                        : "Verify OTP failed: " + throwable.getMessage();
                    BaseResponse<Boolean> errorResponse = new BaseResponse<>(errorMessage, null);
                    return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(errorResponse);
                });
    }

    /**
     * Reset password with OTP endpoint
     */
    @PublicRoute
    @PostMapping("/password/reset")
    public CompletableFuture<ResponseEntity<BaseResponse<String>>> resetPasswordWithOtp(@Valid @RequestBody ResetPasswordWithOtpRequestDto request) {
        log.info("Password reset request received for phone: {}", request.getPhone());
        return authService.resetPasswordWithOtp(request)
                .thenApply(ResponseEntity::ok)
                .exceptionally(throwable -> {
                    log.error("Password reset failed for phone: {}", request.getPhone(), throwable);
                    String errorMessage = throwable.getCause() instanceof UnsupportedOperationException 
                        ? throwable.getCause().getMessage() 
                        : "Password reset failed: " + throwable.getMessage();
                    BaseResponse<String> errorResponse = new BaseResponse<>(errorMessage, null);
                    return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(errorResponse);
                });
    }

    /**
     * Check phone exists endpoint
     */
    @PublicRoute
    @GetMapping("/phone/exists/{phone}")
    public CompletableFuture<ResponseEntity<BaseResponse<Boolean>>> checkPhoneExists(@PathVariable String phone) {
        log.info("Check phone exists request received for phone: {}", phone);
        return authService.checkPhoneExists(phone)
                .thenApply(ResponseEntity::ok)
                .exceptionally(throwable -> {
                    log.error("Check phone exists failed for phone: {}", phone, throwable);
                    String errorMessage = throwable.getCause() instanceof UnsupportedOperationException 
                        ? throwable.getCause().getMessage() 
                        : "Check phone exists failed: " + throwable.getMessage();
                    BaseResponse<Boolean> errorResponse = new BaseResponse<>(errorMessage, null);
                    return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(errorResponse);
                });
    }
}
