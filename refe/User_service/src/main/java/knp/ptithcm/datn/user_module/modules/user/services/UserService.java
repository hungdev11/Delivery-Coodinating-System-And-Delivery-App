package knp.ptithcm.datn.user_module.modules.user.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import knp.ptithcm.datn.user_module.modules.user.dtos.requests.CreateUserRequest;
import knp.ptithcm.datn.user_module.modules.user.dtos.requests.UpdateUserRequest;
import knp.ptithcm.datn.user_module.modules.user.dtos.requests.RegisterByPhoneRequest;
import knp.ptithcm.datn.user_module.modules.user.dtos.requests.ResetPasswordWithOtpRequest;
import knp.ptithcm.datn.user_module.modules.user.dtos.requests.UpdateProfileRequest;
import knp.ptithcm.datn.user_module.modules.user.dtos.responses.LoginResponse;
import knp.ptithcm.datn.user_module.modules.user.entities.User;
import knp.ptithcm.datn.user_module.modules.user.mappers.UserMapper;
import knp.ptithcm.datn.user_module.modules.user.repositories.UserRepository;
import knp.ptithcm.datn.user_module.modules.user.enums.UserRole;
import knp.ptithcm.datn.user_module.modules.user.enums.UserStatus;
import knp.ptithcm.datn.user_module.modules.user.dtos.responses.UserResponse;
import knp.ptithcm.datn.user_module.modules.user.mappers.UserEntityMapper;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Service xử lý nghiệp vụ user, chỉ gọi KeycloakUserService cho các thao tác liên quan Keycloak.
 * Đảm bảo validate input, log rõ ràng, không để lộ KeycloakUserService ra ngoài.
 */
@Service
public class UserService {
    private static final Logger log = LoggerFactory.getLogger(UserService.class);
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private KeycloakUserService keycloakUserService;

    @Autowired
    private OtpService otpService;

    /**
     * Tạo user mới (đồng bộ Keycloak và DB).
     * @param dto Thông tin tạo user (không null)
     * @return User entity đã lưu
     */
    public User createUser(CreateUserRequest dto) {
        if (dto == null) throw new IllegalArgumentException("CreateUserRequest must not be null");
        log.debug("[UserService] Creating user: {}", dto.getUsername());
        String keycloakId = keycloakUserService.createUser(dto, dto.getRoles());
        User user = UserMapper.toUserEntity(dto, keycloakId);
        user.setStatus(UserStatus.ACTIVE);
        user = userRepository.save(user);
        // Lấy lại roles và createdAt thực tế từ Keycloak
        List<String> roles = keycloakUserService.getUserRoles(keycloakId);
        Map<String, Long> timestamps = keycloakUserService.getUserTimestamps(keycloakId);
        if (roles != null && !roles.isEmpty()) {
            log.debug("[UserService] Roles from Keycloak for user {}: {}", user.getUsername(), roles);
        }
        // Cập nhật lại createdAt nếu cần
        if (timestamps != null && timestamps.containsKey("createdAt")) {
            user.setCreatedAt(java.time.Instant.ofEpochMilli(timestamps.get("createdAt")).atZone(java.time.ZoneId.systemDefault()).toLocalDateTime());
            user.setUpdatedAt(user.getCreatedAt());
            userRepository.save(user);
        }
        log.info("[UserService] User created: {} (id={})", user.getUsername(), user.getId());
        return user;
    }

    /**
     * Cập nhật user (đồng bộ Keycloak và DB).
     * @param id UUID user
     * @param dto Thông tin cập nhật (không null)
     * @return User entity đã cập nhật
     */
    public User updateUser(UUID id, UpdateUserRequest dto) {
        if (id == null || dto == null) throw new IllegalArgumentException("id và UpdateUserRequest không được null");
        log.debug("[UserService] Updating user: {}", id);
        User user = userRepository.findById(id).orElseThrow(() -> {
            log.error("[UserService] User not found: {}", id);
            return new RuntimeException("User not found");
        });
        keycloakUserService.updateUser(user.getKeycloakId(), dto, dto.getRoles());
        UserMapper.updateUserEntity(user, dto);
        user.setUpdatedAt(LocalDateTime.now());
        log.info("[UserService] User updated: {} (id={})", user.getUsername(), user.getId());
        return userRepository.save(user);
    }

    /**
     * Xoá user (đồng bộ Keycloak và DB).
     * @param id UUID user
     */
    public void deleteUser(UUID id) {
        if (id == null) throw new IllegalArgumentException("id không được null");
        log.debug("[UserService] Deleting user: {}", id);
        User user = userRepository.findById(id).orElseThrow(() -> {
            log.error("[UserService] User not found for delete: {}", id);
            return new RuntimeException("User not found");
        });
        keycloakUserService.deleteUserById(user.getKeycloakId());
        userRepository.deleteById(id);
        log.info("[UserService] User deleted: {} (id={})", user.getUsername(), user.getId());
    }

    /**
     * Đồng bộ trạng thái user với Keycloak (nếu user không còn trên Keycloak thì block local user).
     * @param user User entity
     * @return User entity đã đồng bộ
     */
    private User syncStatusWithKeycloak(User user) {
        try {
            keycloakUserService.getUserById(user.getKeycloakId());
        } catch (Exception e) {
            log.warn("[UserService] Keycloak user not found, blocking local user: {} (id={})", user.getUsername(), user.getId());
            user.setStatus(UserStatus.BLOCKED);
            user.setUpdatedAt(LocalDateTime.now());
            userRepository.save(user);
        }
        return user;
    }

    /**
     * Lấy user theo id (đồng bộ trạng thái với Keycloak, đồng bộ roles từ Keycloak).
     * @param id UUID user
     * @return Optional<User>
     */
    public Optional<User> getUser(UUID id) {
        if (id == null) throw new IllegalArgumentException("id không được null");
        log.debug("[UserService] Getting user: {}", id);
        return userRepository.findById(id).map(user -> {
            User synced = syncStatusWithKeycloak(user);
            List<String> roles = keycloakUserService.getUserRoles(synced.getKeycloakId());
            UserResponse dto = UserEntityMapper.toUserDto(synced);
            dto.setRoles(roles);
            return synced;
        });
    }

    /**
     * Lấy danh sách user (đã mapping sang DTO, đồng bộ roles từ Keycloak).
     * @return List<UserResponse>
     */
    public List<UserResponse> listUsers() {
        log.debug("[UserService] Listing all users");
        return userRepository.findAll().stream()
            .map(user -> {
                UserResponse dto = UserEntityMapper.toUserDto(user);
                List<String> roles = keycloakUserService.getUserRoles(user.getKeycloakId());
                dto.setRoles(roles);
                return dto;
            })
            .collect(Collectors.toList());
    }

    /**
     * Lấy user theo username (đồng bộ trạng thái với Keycloak, đồng bộ roles từ Keycloak).
     * @param username username
     * @return Optional<User>
     */
    public Optional<User> getUserByUsername(String username) {
        if (username == null) throw new IllegalArgumentException("username không được null");
        log.debug("[UserService] Getting user by username: {}", username);
        return userRepository.findAll().stream()
            .filter(u -> username.equals(u.getUsername()))
            .findFirst()
            .map(user -> {
                User synced = syncStatusWithKeycloak(user);
                List<String> roles = keycloakUserService.getUserRoles(synced.getKeycloakId());
                UserResponse dto = UserEntityMapper.toUserDto(synced);
                dto.setRoles(roles);
                return synced;
            });
    }

    /**
     * Lấy user theo email (đồng bộ trạng thái với Keycloak, đồng bộ roles từ Keycloak).
     * @param email email
     * @return Optional<User>
     */
    public Optional<User> getUserByEmail(String email) {
        if (email == null) throw new IllegalArgumentException("email không được null");
        log.debug("[UserService] Getting user by email: {}", email);
        return userRepository.findAll().stream()
            .filter(u -> email.equals(u.getEmail()))
            .findFirst()
            .map(user -> {
                User synced = syncStatusWithKeycloak(user);
                List<String> roles = keycloakUserService.getUserRoles(synced.getKeycloakId());
                UserResponse dto = UserEntityMapper.toUserDto(synced);
                dto.setRoles(roles);
                return synced;
            });
    }

    /**
     * Cập nhật trạng thái user.
     * @param id UUID user
     * @param status trạng thái mới
     * @return User entity đã cập nhật
     */
    public User updateUserStatus(UUID id, UserStatus status) {
        if (id == null || status == null) throw new IllegalArgumentException("id và status không được null");
        log.debug("[UserService] Updating user status: {} -> {}", id, status);
        User user = userRepository.findById(id).orElseThrow(() -> {
            log.error("[UserService] User not found for status update: {}", id);
            return new RuntimeException("User not found");
        });
        user.setStatus(status);
        log.info("[UserService] User status updated: {} (id={}) -> {}", user.getUsername(), user.getId(), status);
        return userRepository.save(user);
    }

    /**
     * Cập nhật role user (chưa hỗ trợ).
     * @param id UUID user
     * @param role role mới
     * @return User entity
     */
    public User updateUserRole(UUID id, UserRole role) {
        log.warn("[UserService] updateUserRole not implemented: {} -> {}", id, role);
        throw new UnsupportedOperationException("Not implemented");
    }

    /**
     * Cập nhật mật khẩu user (đồng bộ Keycloak).
     * @param id UUID user
     * @param newPassword mật khẩu mới
     * @return User entity
     */
    public User updateUserPassword(UUID id, String newPassword) {
        if (id == null || newPassword == null) throw new IllegalArgumentException("id và newPassword không được null");
        log.debug("[UserService] Updating user password: {}", id);
        User user = userRepository.findById(id).orElseThrow(() -> {
            log.error("[UserService] User not found for password update: {}", id);
            return new RuntimeException("User not found");
        });
        keycloakUserService.updatePassword(user.getKeycloakId(), newPassword);
        log.info("[UserService] User password updated: {} (id={})", user.getUsername(), user.getId());
        return user;
    }

    /**
     * Lấy UserResponse DTO theo id (đồng bộ roles từ Keycloak).
     * @param id UUID user
     * @return Optional<UserResponse>
     */
    public Optional<UserResponse> getUserResponse(UUID id) {
        if (id == null) throw new IllegalArgumentException("id không được null");
        log.debug("[UserService] Getting user response DTO: {}", id);
        return userRepository.findById(id).map(user -> {
            User synced = syncStatusWithKeycloak(user);
            List<String> roles = keycloakUserService.getUserRoles(synced.getKeycloakId());
            UserResponse dto = UserEntityMapper.toUserDto(synced);
            dto.setRoles(roles);
            return dto;
        });
    }

    /**
     * Lấy roles của user theo keycloakId.
     * @param keycloakId id Keycloak
     * @return List<String> roles
     */
    public List<String> getUserRolesByKeycloakId(String keycloakId) {
        if (keycloakId == null) throw new IllegalArgumentException("keycloakId không được null");
        log.debug("[UserService] Getting user roles by keycloakId: {}", keycloakId);
        return keycloakUserService.getUserRoles(keycloakId);
    }

    // Đăng ký bằng số điện thoại (tạo user local + user Keycloak với username=phone, email rỗng)
    public User registerByPhone(RegisterByPhoneRequest request) {
        if (request == null) throw new IllegalArgumentException("request must not be null");
        if (request.getPhone() == null || request.getPhone().isBlank()) throw new IllegalArgumentException("phone is required");
        if (userRepository.existsByPhone(request.getPhone())) throw new RuntimeException("Phone already exists");
        CreateUserRequest dto = new CreateUserRequest();
        dto.setUsername(request.getPhone());
        dto.setEmail("");
        dto.setFirstName(request.getFirstName());
        dto.setLastName(request.getLastName());
        dto.setPhone(request.getPhone());
        dto.setPassword(request.getPassword());
        dto.setRoles(java.util.List.of("USER"));
        return createUser(dto);
    }

    // Kiểm tra tồn tại số điện thoại
    public boolean phoneExists(String phone) {
        if (phone == null || phone.isBlank()) return false;
        return userRepository.existsByPhone(phone);
    }

    // Gửi OTP (placeholder luôn trả 000000)
    public String sendOtp(String phone) {
        if (phone == null || phone.isBlank()) throw new IllegalArgumentException("phone is required");
        return otpService.sendOtp(phone);
    }

    // Xác thực OTP
    public boolean verifyOtp(String phone, String otp) {
        if (phone == null || otp == null) return false;
        return otpService.verifyOtp(phone, otp);
    }

    // Quên mật khẩu: xác minh OTP và đổi mật khẩu trên Keycloak
    public void resetPasswordWithOtp(ResetPasswordWithOtpRequest request) {
        if (request == null) throw new IllegalArgumentException("request must not be null");
        if (!otpService.verifyOtp(request.getPhone(), request.getOtp())) throw new RuntimeException("Invalid OTP");
        User user = userRepository.findByPhone(request.getPhone()).orElseThrow(() -> new RuntimeException("User not found"));
        keycloakUserService.updatePassword(user.getKeycloakId(), request.getNewPassword());
    }

    // Cập nhật profile (tạm thời cho phép truyền id trực tiếp)
    public User updateProfile(UpdateProfileRequest request) {
        if (request == null) throw new IllegalArgumentException("request must not be null");
        if (request.getId() == null) throw new IllegalArgumentException("id is required");
        UUID id = UUID.fromString(request.getId());
        User user = userRepository.findById(id).orElseThrow(() -> new RuntimeException("User not found"));
        if (request.getFirstName() != null) user.setFirstName(request.getFirstName());
        if (request.getLastName() != null) user.setLastName(request.getLastName());
        if (request.getPhone() != null) user.setPhone(request.getPhone());
        if (request.getAddress() != null) user.setAddress(request.getAddress());
        if (request.getIdentityNumber() != null) user.setIdentityNumber(request.getIdentityNumber());
        user.setUpdatedAt(LocalDateTime.now());
        return userRepository.save(user);
    }

    /**
     * Đăng nhập user với username và password
     * @param username username
     * @param password password
     * @return LoginResponse chứa token và thông tin user
     */
    public LoginResponse login(String username, String password) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username không được để trống");
        }
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("Password không được để trống");
        }
        
        log.debug("[UserService] Attempting login for user: {}", username);
        
        try {
            // Lấy thông tin user từ database trước
            User user = getUserByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found in database"));
            
            // Lấy roles từ Keycloak
            List<String> roles = getUserRolesByKeycloakId(user.getKeycloakId());
            
            // Tạo UserResponse DTO
            UserResponse userResponse = UserEntityMapper.toUserDto(user);
            userResponse.setRoles(roles != null ? roles : List.of());
            
            // Gọi Keycloak để xác thực và lấy token
            LoginResponse loginResponse = keycloakUserService.login(username, password);
            
            // Set user info vào response
            loginResponse.setUser(userResponse);
            
            log.info("[UserService] Login successful for user: {}", username);
            return loginResponse;
            
        } catch (Exception e) {
            log.error("[UserService] Login failed for user: {}, error: {}", username, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Refresh access token sử dụng refresh token
     * @param refreshToken refresh token
     * @return RefreshTokenResponse chứa tokens mới
     */
    public knp.ptithcm.datn.user_module.modules.user.dtos.responses.RefreshTokenResponse refreshToken(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new IllegalArgumentException("Refresh token không được để trống");
        }
        
        log.debug("[UserService] Refreshing token");
        
        try {
            knp.ptithcm.datn.user_module.modules.user.dtos.responses.RefreshTokenResponse response = keycloakUserService.refreshToken(refreshToken);
            log.info("[UserService] Token refreshed successfully");
            return response;
        } catch (Exception e) {
            log.error("[UserService] Token refresh failed: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Logout user bằng cách revoke refresh token
     * @param refreshToken refresh token cần revoke
     * @return LogoutResponse
     */
    public knp.ptithcm.datn.user_module.modules.user.dtos.responses.LogoutResponse logout(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new IllegalArgumentException("Refresh token không được để trống");
        }
        
        log.debug("[UserService] Logging out user");
        
        try {
            knp.ptithcm.datn.user_module.modules.user.dtos.responses.LogoutResponse response = keycloakUserService.logout(refreshToken);
            log.info("[UserService] User logged out successfully");
            return response;
        } catch (Exception e) {
            log.error("[UserService] Logout failed: {}", e.getMessage(), e);
            throw e;
        }
    }
}
