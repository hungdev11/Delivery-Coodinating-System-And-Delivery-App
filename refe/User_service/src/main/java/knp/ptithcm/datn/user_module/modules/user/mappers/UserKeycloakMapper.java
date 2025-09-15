package knp.ptithcm.datn.user_module.modules.user.mappers;

import knp.ptithcm.datn.user_module.modules.user.entities.KeycloakUser;
import knp.ptithcm.datn.user_module.modules.user.entities.User;
import knp.ptithcm.datn.user_module.modules.user.enums.UserStatus;

/**
 * Mapper chuyển đổi giữa User entity và KeycloakUser entity.
 * Chỉ dùng cho mapping entity <-> KeycloakUser.
 */
public class UserKeycloakMapper {
    /**
     * Chuyển User entity sang KeycloakUser entity.
     * @param user User entity (không null)
     * @return KeycloakUser
     */
    public static KeycloakUser toKeycloakUser(User user) {
        if (user == null) throw new IllegalArgumentException("User must not be null");
        return KeycloakUser.builder()
                .id(user.getKeycloakId())
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .enabled(user.getStatus() == UserStatus.ACTIVE)
                .build();
    }

    /**
     * Cập nhật User entity từ KeycloakUser entity.
     * @param user User entity (không null)
     * @param keycloakUser KeycloakUser entity (không null)
     */
    public static void updateUserFromKeycloak(User user, KeycloakUser keycloakUser) {
        if (user == null || keycloakUser == null) throw new IllegalArgumentException("User và KeycloakUser không được null");
        user.setUsername(keycloakUser.getUsername());
        user.setEmail(keycloakUser.getEmail());
        user.setFirstName(keycloakUser.getFirstName());
        user.setLastName(keycloakUser.getLastName());
        user.setKeycloakId(keycloakUser.getId());
    }
} 
