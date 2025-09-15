package knp.ptithcm.datn.user_module.modules.user.mappers;

import knp.ptithcm.datn.user_module.modules.user.dtos.responses.UserResponse;
import knp.ptithcm.datn.user_module.modules.user.dtos.responses.UserProfileResponse;
import knp.ptithcm.datn.user_module.modules.user.entities.User;
import java.util.List;
import java.time.LocalDateTime;

/**
 * Mapper chuyển đổi giữa User entity và các response DTO (UserReponse, UserProfileResponse).
 * Đảm bảo null-safe, chỉ dùng cho mapping entity <-> response DTO.
 */
public class UserEntityMapper {
    /**
     * Chuyển User entity sang UserProfileResponse với roles và thời gian tuỳ chỉnh.
     * @param user User entity (không null)
     * @param roles Danh sách role của user
     * @param createdAt Thời gian tạo
     * @param updatedAt Thời gian cập nhật
     * @return UserProfileResponse
     */
    public static UserProfileResponse toUserProfileDto(User user, List<String> roles, LocalDateTime createdAt, LocalDateTime updatedAt) {
        if (user == null) throw new IllegalArgumentException("User must not be null");
        UserProfileResponse dto = new UserProfileResponse();
        dto.setId(user.getId());
        dto.setEmail(user.getEmail());
        dto.setPhone(user.getPhone());
        dto.setAddress(user.getAddress());
        dto.setIdentityNumber(user.getIdentityNumber());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setKeycloakId(user.getKeycloakId());
        dto.setStatus(user.getStatus() != null ? user.getStatus().toString() : "ACTIVE");
        dto.setRoles(roles);
        dto.setCreatedAt(createdAt);
        dto.setUpdatedAt(updatedAt);
        return dto;
    }

    /**
     * Chuyển User entity sang UserReponse DTO (cho REST/gRPC).
     * @param user User entity (không null)
     * @return UserReponse
     */
    public static UserResponse toUserDto(User user) {
        if (user == null) throw new IllegalArgumentException("User must not be null");
        UserResponse dto = new UserResponse();
        dto.setId(user.getId() != null ? user.getId().toString() : "");
        dto.setUsername(user.getUsername() != null ? user.getUsername() : "");
        dto.setEmail(user.getEmail() != null ? user.getEmail() : "");
        dto.setFullName((user.getFirstName() != null ? user.getFirstName() : "") + (user.getLastName() != null ? (" " + user.getLastName()) : ""));
        dto.setKeycloakId(user.getKeycloakId() != null ? user.getKeycloakId() : "");
        dto.setStatus(user.getStatus() != null ? user.getStatus().toString() : "ACTIVE");
        dto.setCreatedAt(user.getCreatedAt());
        dto.setUpdatedAt(user.getUpdatedAt());
        dto.setPhone(user.getPhone());
        dto.setAddress(user.getAddress());
        dto.setIdentityNumber(user.getIdentityNumber());
        return dto;
    }

    /**
     * Chuyển User entity sang UserProfileResponse với roles rỗng và thời gian mặc định.
     * @param user User entity (không null)
     * @return UserProfileResponse
     */
    public static UserProfileResponse toUserProfileDto(User user) {
        return toUserProfileDto(user, List.of(), user.getCreatedAt(), user.getUpdatedAt());
    }
} 
