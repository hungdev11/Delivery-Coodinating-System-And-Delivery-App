package ptithcm.graduation.apigateway.controllers.v1.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ptithcm.graduation.apigateway.annotations.AuthRequired;
import ptithcm.graduation.apigateway.annotations.PublicRoute;
import ptithcm.graduation.apigateway.models.BaseResponse;
import ptithcm.graduation.apigateway.models.PagedResult;
import ptithcm.graduation.apigateway.models.Paging;
import ptithcm.graduation.apigateway.services.v1.user.dto.CreateUserRequestDto;
import ptithcm.graduation.apigateway.services.v1.user.dto.ChangePasswordRequestDto;
import ptithcm.graduation.apigateway.services.v1.user.dto.UpdateUserRequestDto;
import ptithcm.graduation.apigateway.services.v1.user.dto.UserDto;
import ptithcm.graduation.apigateway.services.v1.user.interfaces.IUserService;
import jakarta.validation.Valid;
import java.util.concurrent.CompletableFuture;

@Slf4j
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@AuthRequired
public class UserController {

    private final IUserService userService;

    /**
     * Get user by ID
     */
    @GetMapping("/{id}")
    public CompletableFuture<ResponseEntity<BaseResponse<UserDto>>> getUserById(@PathVariable String id) {
        log.info("Get user request received for ID: {}", id);
        return userService.getUserById(id)  
                .thenApply(ResponseEntity::ok)
                .exceptionally(throwable -> {
                    log.error("Failed to get user with ID: {}", id, throwable);
                    BaseResponse<UserDto> errorResponse = new BaseResponse<>("Failed to get user: " + throwable.getMessage(), null);
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
                });
    }

    /**
     * Get user by username
     */
    @GetMapping("/username/{username}")
    public CompletableFuture<ResponseEntity<BaseResponse<UserDto>>> getUserByUsername(@PathVariable String username) {
        log.info("Get user by username request received: {}", username);
        return userService.getUserByUsername(username)
                .thenApply(ResponseEntity::ok)
                .exceptionally(throwable -> {
                    log.error("Failed to get user by username: {}", username, throwable);
                    BaseResponse<UserDto> errorResponse = new BaseResponse<>("Failed to get user: " + throwable.getMessage(), null);
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
                });
    }

    /**
     * Get user by email
     */
    @GetMapping("/email/{email}")
    public CompletableFuture<ResponseEntity<BaseResponse<UserDto>>> getUserByEmail(@PathVariable String email) {
        log.info("Get user by email request received: {}", email);
        return userService.getUserByEmail(email)
                .thenApply(ResponseEntity::ok)
                .exceptionally(throwable -> {
                    log.error("Failed to get user by email: {}", email, throwable);
                    BaseResponse<UserDto> errorResponse = new BaseResponse<>("Failed to get user: " + throwable.getMessage(), null);
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
                });
    }

    /**
     * List users with pagination
     */
    @GetMapping
    public CompletableFuture<ResponseEntity<BaseResponse<PagedResult<UserDto>>>> listUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("List users request received - page: {}, size: {}", page, size);
        
        Paging request = new Paging(page, size);    
        
        return userService.listUsers(request)
                .thenApply(ResponseEntity::ok)
                .exceptionally(throwable -> {
                    log.error("Failed to list users", throwable);
                    BaseResponse<PagedResult<UserDto>> errorResponse = new BaseResponse<>("Failed to list users: " + throwable.getMessage(), null);
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
                });
    }

    /**
     * Create new user
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public CompletableFuture<ResponseEntity<BaseResponse<UserDto>>> createUser(@Valid @RequestBody CreateUserRequestDto request) {
        log.info("Create user request received for username: {}", request.getUsername());
        return userService.createUser(request)
                .thenApply(ResponseEntity::ok)
                .exceptionally(throwable -> {
                    log.error("Failed to create user with username: {}", request.getUsername(), throwable);
                    BaseResponse<UserDto> errorResponse = new BaseResponse<>("Failed to create user: " + throwable.getMessage(), null);
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
                });
    }

    /**
     * Update user
     */
    @PreAuthorize("hasRole('ADMIN') or authentication.principal.subject == #id")
    @PutMapping("/{id}")
    public CompletableFuture<ResponseEntity<BaseResponse<UserDto>>> updateUser(
            @PathVariable String id,
            @Valid @RequestBody UpdateUserRequestDto request) {
        log.info("Update user request received for ID: {}", id);
        return userService.updateUser(request)
                .thenApply(ResponseEntity::ok)
                .exceptionally(throwable -> {
                    log.error("Failed to update user with ID: {}", id, throwable);
                    BaseResponse<UserDto> errorResponse = new BaseResponse<>("Failed to update user: " + throwable.getMessage(), null);
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
                });
    }

    /**
     * Update user status
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}/status")
    public CompletableFuture<ResponseEntity<BaseResponse<UserDto>>> updateUserStatus(
            @PathVariable String id,
            @RequestParam int status) {
        log.info("Update user status request received for ID: {}, status: {}", id, status);
        return userService.updateUserStatus(id, status)
                .thenApply(ResponseEntity::ok)
                .exceptionally(throwable -> {
                    log.error("Failed to update user status for ID: {}", id, throwable);
                    BaseResponse<UserDto> errorResponse = new BaseResponse<>("Failed to update user status: " + throwable.getMessage(), null);
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
                });
    }

    /**
     * Update user role
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}/role")
    public CompletableFuture<ResponseEntity<BaseResponse<UserDto>>> updateUserRole(
            @PathVariable String id,
            @RequestParam int role) {
        log.info("Update user role request received for ID: {}, role: {}", id, role);
        return userService.updateUserRole(id, role)
                .thenApply(ResponseEntity::ok)
                .exceptionally(throwable -> {
                    log.error("Failed to update user role for ID: {}", id, throwable);
                    BaseResponse<UserDto> errorResponse = new BaseResponse<>("Failed to update user role: " + throwable.getMessage(), null);
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
                });
    }

    /**
     * Update user password
     */
    @PreAuthorize("hasRole('ADMIN') or authentication.principal.subject == #id")
    @PatchMapping("/{id}/password")
    public CompletableFuture<ResponseEntity<BaseResponse<String>>> updateUserPassword(
            @PathVariable String id,
            @RequestParam String oldPassword,
            @RequestParam String newPassword) {
        log.info("Update user password request received for ID: {}", id);
        return userService.changePassword(new ChangePasswordRequestDto(id, oldPassword, newPassword))
                .thenApply(result -> ResponseEntity.ok(new BaseResponse<String>("Password updated successfully", null)))
                .exceptionally(throwable -> {
                    log.error("Failed to update user password for ID: {}", id, throwable);
                    BaseResponse<String> errorResponse = new BaseResponse<>("Failed to update user password: " + throwable.getMessage(), null);
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
                });
    }

    /**
     * Health check endpoint
     */
    @PublicRoute
    @GetMapping("/health")
    public ResponseEntity<BaseResponse<UserDto>> healthCheck() {
        return ResponseEntity.ok(new BaseResponse<UserDto>("User service is running", null));
    }
}
