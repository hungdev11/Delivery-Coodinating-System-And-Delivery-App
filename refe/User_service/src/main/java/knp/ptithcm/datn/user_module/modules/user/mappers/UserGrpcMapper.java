package knp.ptithcm.datn.user_module.modules.user.mappers;

import knp.ptithcm.datn.user_module.modules.user.dtos.responses.UserResponse;
import knp.ptithcm.datn.user_module.modules.base.grpc.UserGrpc;
import knp.ptithcm.datn.user_module.modules.user.entities.User;

/**
 * Mapper chuyển đổi giữa UserReponse DTO và gRPC User proto.
 * Chỉ dùng cho mapping DTO <-> gRPC proto.
 */
public class UserGrpcMapper {
	/**
	 * Chuyển UserReponse DTO sang gRPC User proto.
	 * @param dto UserReponse DTO (không null)
	 * @return User proto
	 */
	public static UserGrpc toProto(UserResponse dto) {
		if (dto == null) throw new IllegalArgumentException("UserReponse must not be null");
		String fullName = dto.getFullName() != null ? dto.getFullName().trim() : "";
		String firstName = "";
		String lastName = "";
		if (!fullName.isEmpty()) {
			String[] parts = fullName.split(" ", 2);
			firstName = parts.length > 0 ? parts[0] : "";
			lastName = parts.length > 1 ? parts[1] : "";
		}
		UserGrpc.Builder builder = UserGrpc.newBuilder()
				.setId(dto.getId() != null ? dto.getId() : "")
				.setUsername(dto.getUsername() != null ? dto.getUsername() : "")
				.setEmail(dto.getEmail() != null ? dto.getEmail() : "")
				.setFirstName(firstName)
				.setLastName(lastName)
				.setKeycloakId(dto.getKeycloakId() != null ? dto.getKeycloakId() : "")
				.setStatus(getStatusOrdinal(dto.getStatus()))
				.setCreatedAt(dto.getCreatedAt() != null ? dto.getCreatedAt().toString() : "")
				.setUpdatedAt(dto.getUpdatedAt() != null ? dto.getUpdatedAt().toString() : "");
		if (dto.getRoles() != null) {
			builder.addAllRoles(dto.getRoles());
		}
		if (dto.getPhone() != null) {
			builder.setPhone(dto.getPhone());
		}
		if (dto.getAddress() != null) {
			builder.setAddress(dto.getAddress());
		}
		if (dto.getIdentityNumber() != null) {
			builder.setIdentityNumber(dto.getIdentityNumber());
		}
		return builder.build();
	}

	/**
	 * Chuyển User entity sang gRPC UserGrpc proto.
	 * @param user User entity (không null)
	 * @return UserGrpc proto
	 */
	public static UserGrpc toProto(User user) {
		if (user == null) throw new IllegalArgumentException("User must not be null");
		UserGrpc.Builder builder = UserGrpc.newBuilder()
				.setId(user.getId() != null ? user.getId().toString() : "")
				.setUsername(user.getUsername() != null ? user.getUsername() : "")
				.setEmail(user.getEmail() != null ? user.getEmail() : "")
				.setFirstName(user.getFirstName() != null ? user.getFirstName() : "")
				.setLastName(user.getLastName() != null ? user.getLastName() : "")
				.setKeycloakId(user.getKeycloakId() != null ? user.getKeycloakId() : "")
				.setStatus(user.getStatus() != null ? user.getStatus().ordinal() : 0)
				.setCreatedAt(user.getCreatedAt() != null ? user.getCreatedAt().toString() : "")
				.setUpdatedAt(user.getUpdatedAt() != null ? user.getUpdatedAt().toString() : "");
		if (user.getPhone() != null) builder.setPhone(user.getPhone());
		if (user.getAddress() != null) builder.setAddress(user.getAddress());
		if (user.getIdentityNumber() != null) builder.setIdentityNumber(user.getIdentityNumber());
		// Roles sẽ được set ở nơi gọi nếu cần
		return builder.build();
	}

	/**
	 * Lấy ordinal của status từ string (null-safe).
	 * @param status tên status
	 * @return ordinal int
	 */
	private static int getStatusOrdinal(String status) {
		if (status == null) return 0;
		try {
			return knp.ptithcm.datn.user_module.modules.user.enums.UserStatus.valueOf(status).ordinal();
		} catch (Exception e) {
			return 0;
		}
	}
} 
