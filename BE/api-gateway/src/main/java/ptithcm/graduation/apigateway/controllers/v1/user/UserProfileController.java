package ptithcm.graduation.apigateway.controllers.v1.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ptithcm.graduation.apigateway.annotations.AuthRequired;
import ptithcm.graduation.apigateway.models.BaseResponse;
import ptithcm.graduation.apigateway.services.v1.user.dto.*;
import ptithcm.graduation.apigateway.services.v1.user.interfaces.IUserService;
import jakarta.validation.Valid;
import java.util.concurrent.CompletableFuture;

@Slf4j
@RestController
@RequestMapping("/api/v1/user/profile")
@RequiredArgsConstructor
@AuthRequired
public class UserProfileController {

    private final IUserService userService;

    /**
     * Get current user profile
     */
    @GetMapping("/me")
    public CompletableFuture<ResponseEntity<BaseResponse<UserDto>>> getCurrentUser(@RequestParam String userId) {
        log.info("Get current user profile request received for user ID: {}", userId);
        return userService.getCurrentUser(userId)
                .thenApply(ResponseEntity::ok)
                .exceptionally(throwable -> {
                    log.error("Failed to get current user profile for user ID: {}", userId, throwable);
                    BaseResponse<UserDto> errorResponse = new BaseResponse<>(
                            "Failed to get user profile: " + throwable.getMessage(), null);
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
                });
    }

    /**
     * Update user profile
     */
    @PutMapping("/update")
    public CompletableFuture<ResponseEntity<BaseResponse<UserDto>>> updateProfile(
            @Valid @RequestBody UpdateUserRequestDto request) {
        log.info("Update profile request received for user ID: {}", request.getId());
        return userService.updateProfile(request)
                .thenApply(ResponseEntity::ok)
                .exceptionally(throwable -> {
                    log.error("Failed to update profile for user ID: {}", request.getId(), throwable);
                    String errorMessage = throwable.getCause() instanceof UnsupportedOperationException
                            ? throwable.getCause().getMessage()
                            : "Failed to update profile: " + throwable.getMessage();
                    BaseResponse<UserDto> errorResponse = new BaseResponse<>(errorMessage, null);
                    return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(errorResponse);
                });
    }

    /**
     * Change password
     */
    @PostMapping("/password/change")
    public CompletableFuture<ResponseEntity<BaseResponse<String>>> changePassword(
            @Valid @RequestBody ChangePasswordRequestDto request) {
        log.info("Change password request received for user ID: {}", request.getUserId());
        return userService.changePassword(request)
                .thenApply(ResponseEntity::ok)
                .exceptionally(throwable -> {
                    log.error("Failed to change password for user ID: {}", request.getUserId(), throwable);
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(new BaseResponse<String>("Failed to change password: " + throwable.getMessage(), null));
                });
    }
}
