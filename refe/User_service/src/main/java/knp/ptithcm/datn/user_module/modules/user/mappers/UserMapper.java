package knp.ptithcm.datn.user_module.modules.user.mappers;

import knp.ptithcm.datn.user_module.modules.user.dtos.requests.CreateUserRequest;
import knp.ptithcm.datn.user_module.modules.user.dtos.requests.UpdateUserRequest;
import knp.ptithcm.datn.user_module.modules.user.entities.User;
import knp.ptithcm.datn.user_module.modules.user.enums.UserStatus;

/**
 * Mapper chuyển đổi giữa request DTO (CreateUserRequest, UpdateUserRequest) và User entity.
 * Chỉ dùng cho mapping request DTO <-> entity.
 */
public class UserMapper {
	/**
	 * Chuyển CreateUserRequest sang User entity.
	 * @param dto CreateUserRequest (không null)
	 * @param keycloakId id Keycloak tương ứng
	 * @return User entity
	 */
	public static User toUserEntity(CreateUserRequest dto, String keycloakId) {
		if (dto == null) throw new IllegalArgumentException("CreateUserRequest must not be null");
		User user = new User();
		user.setKeycloakId(keycloakId);
		user.setUsername(dto.getUsername());
		user.setEmail(dto.getEmail());
		user.setFirstName(dto.getFirstName());
		user.setLastName(dto.getLastName());
		user.setPhone(dto.getPhone());
		user.setAddress(dto.getAddress());
		user.setIdentityNumber(dto.getIdentityNumber());
		user.setStatus(UserStatus.ACTIVE);
		return user;
	}

	/**
	 * Cập nhật User entity từ UpdateUserRequest.
	 * @param user User entity (không null)
	 * @param dto UpdateUserRequest (không null)
	 */
	public static void updateUserEntity(User user, UpdateUserRequest dto) {
		if (user == null || dto == null) throw new IllegalArgumentException("User và UpdateUserRequest không được null");
		if (dto.getUsername() != null) user.setUsername(dto.getUsername());
		if (dto.getEmail() != null) user.setEmail(dto.getEmail());
		if (dto.getFullName() != null) {
			String fullName = dto.getFullName().trim();
			if (!fullName.isEmpty()) {
				String[] names = fullName.split(" ", 2);
				user.setFirstName(names.length > 0 ? names[0] : "");
				if (names.length > 1) user.setLastName(names[1]);
			}
		}
		if (dto.getRole() != null) user.setStatus(UserStatus.valueOf(dto.getRole()));
	}
} 
