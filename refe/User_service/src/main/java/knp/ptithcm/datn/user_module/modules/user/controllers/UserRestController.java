package knp.ptithcm.datn.user_module.modules.user.controllers;

import knp.ptithcm.datn.user_module.modules.user.dtos.requests.*;
import knp.ptithcm.datn.user_module.modules.user.dtos.responses.*;
import knp.ptithcm.datn.user_module.modules.user.entities.User;
import knp.ptithcm.datn.user_module.modules.user.services.UserService;
import knp.ptithcm.datn.user_module.modules.user.mappers.UserEntityMapper;
import knp.ptithcm.datn.user_module.modules.user.enums.UserStatus;
import knp.ptithcm.datn.user_module.modules.user.enums.UserRole;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import knp.ptithcm.datn.user_module.modules.common.dtos.ApiResponse;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/user")
public class UserRestController {
    @Autowired
    private UserService userService;

    // Đăng ký tài khoản
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserResponse>> register(@RequestBody CreateUserRequest request) {
        User user = userService.createUser(request);
        return ResponseEntity.ok(new ApiResponse<>(HttpStatus.OK, "Đăng ký thành công", UserEntityMapper.toUserDto(user)));
    }

    // Lấy thông tin user theo id (cho admin)
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> getUser(@PathVariable String id) {
        User user = userService.getUser(UUID.fromString(id)).orElseThrow(() -> new RuntimeException("User not found"));
        return ResponseEntity.ok(new ApiResponse<>(HttpStatus.OK, "Lấy user thành công", UserEntityMapper.toUserDto(user)));
    }

    // Lấy danh sách user (cho admin)
    @GetMapping
    public ResponseEntity<ApiResponse<List<UserResponse>>> listUsers() {
        List<UserResponse> dtos = userService.listUsers();
        return ResponseEntity.ok(new ApiResponse<>(HttpStatus.OK, "Danh sách user", dtos));
    }   

    // Đổi trạng thái user (cho admin)
    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponse<UserResponse>> changeStatus(@PathVariable String id, @RequestBody ChangeUserStatusRequest request) {
        UserStatus status = UserStatus.valueOf(request.getStatus().toUpperCase());
        User user = userService.updateUserStatus(UUID.fromString(id), status);
        return ResponseEntity.ok(new ApiResponse<>(HttpStatus.OK, "Cập nhật trạng thái thành công", UserEntityMapper.toUserDto(user)));
    }

    // Đổi role user (cho admin)
    @PutMapping("/{id}/role")
    public ResponseEntity<ApiResponse<UserResponse>> changeRole(@PathVariable String id, @RequestBody AssignRoleRequest request) {
        try {
            UserRole role = UserRole.valueOf(request.getRole().toUpperCase());
            User user = userService.updateUserRole(UUID.fromString(id), role);
            return ResponseEntity.ok(new ApiResponse<>(HttpStatus.OK, "Cập nhật role thành công", UserEntityMapper.toUserDto(user)));
        } catch (UnsupportedOperationException e) {
            return ResponseEntity.status(501).body(new ApiResponse<>(HttpStatus.NOT_IMPLEMENTED, "Chức năng chưa hỗ trợ", null));
        }
    }

    // Lấy profile user hiện tại
    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getProfile() {
        // TODO: Lấy user từ context, trả về profile
        return ResponseEntity.ok(new ApiResponse<>(HttpStatus.OK, "Lấy profile thành công", null));
    }

    // Cập nhật profile user hiện tại
    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateProfile(@RequestBody UpdateProfileRequest request) {
        User user = userService.updateProfile(request);
        return ResponseEntity.ok(new ApiResponse<>(HttpStatus.OK, "Cập nhật profile thành công", UserEntityMapper.toUserProfileDto(user)));
    }

    // Đăng ký bằng số điện thoại
    @PostMapping("/register/phone")
    public ResponseEntity<ApiResponse<UserResponse>> registerByPhone(@RequestBody knp.ptithcm.datn.user_module.modules.user.dtos.requests.RegisterByPhoneRequest request) {
        User user = userService.registerByPhone(request);
        return ResponseEntity.ok(new ApiResponse<>(HttpStatus.OK, "Đăng ký bằng số điện thoại thành công", UserEntityMapper.toUserDto(user)));
    }

    // Kiểm tra số điện thoại tồn tại
    @GetMapping("/phone-exists")
    public ResponseEntity<ApiResponse<Boolean>> phoneExists(@RequestParam String phone) {
        boolean exists = userService.phoneExists(phone);
        return ResponseEntity.ok(new ApiResponse<>(HttpStatus.OK, "Kiểm tra số điện thoại", exists));
    }

    // Gửi OTP (placeholder 000000)
    @PostMapping("/otp/send")
    public ResponseEntity<ApiResponse<String>> sendOtp(@RequestBody knp.ptithcm.datn.user_module.modules.user.dtos.requests.ForgotPasswordRequest request) {
        String otp = userService.sendOtp(request.getPhone());
        return ResponseEntity.ok(new ApiResponse<>(HttpStatus.OK, "OTP đã được gửi", otp));
    }

    // Xác thực OTP
    @PostMapping("/otp/verify")
    public ResponseEntity<ApiResponse<Boolean>> verifyOtp(@RequestBody knp.ptithcm.datn.user_module.modules.user.dtos.requests.VerifyOtpRequest request) {
        boolean ok = userService.verifyOtp(request.getPhone(), request.getOtp());
        return ResponseEntity.ok(new ApiResponse<>(HttpStatus.OK, ok ? "OTP hợp lệ" : "OTP không hợp lệ", ok));
    }

    // Quên mật khẩu với OTP
    @PostMapping("/password/reset-otp")
    public ResponseEntity<ApiResponse<String>> resetPasswordWithOtp(@RequestBody knp.ptithcm.datn.user_module.modules.user.dtos.requests.ResetPasswordWithOtpRequest request) {
        userService.resetPasswordWithOtp(request);
        return ResponseEntity.ok(new ApiResponse<>(HttpStatus.OK, "Đặt lại mật khẩu thành công", "OK"));
    }
} 
