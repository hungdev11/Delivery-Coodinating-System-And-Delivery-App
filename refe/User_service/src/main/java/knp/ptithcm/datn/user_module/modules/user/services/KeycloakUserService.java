package knp.ptithcm.datn.user_module.modules.user.services;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.keycloak.representations.idm.RoleRepresentation;

import jakarta.ws.rs.core.Response;
import knp.ptithcm.datn.user_module.modules.user.dtos.requests.CreateUserRequest;
import knp.ptithcm.datn.user_module.modules.user.dtos.requests.UpdateUserRequest;
import knp.ptithcm.datn.user_module.modules.user.records.UserRegistrationRecord;

import java.util.*;

/**
 * Service chuyên xử lý các thao tác với Keycloak (tạo, cập nhật, xoá user, role, password).
 * Chỉ được gọi bởi UserService, không expose ra ngoài.
 * Đảm bảo validate input, log rõ ràng, không để lộ logic Keycloak ra ngoài.
 */
@Service
public class KeycloakUserService {
	private static final Logger log = LoggerFactory.getLogger(KeycloakUserService.class);

	@Value("${keycloak.realm}")
	private String realm;
	@Value("${keycloak.resource}")
	private String clientId;
	@Value("${keycloak.credentials.secret}")
	private String clientSecret;
	@Value("${keycloak.auth-server-url}")
	private String serverUrl;
	private final Keycloak keycloak;

	public KeycloakUserService(Keycloak keycloak) {
		this.keycloak = keycloak;
	}

	private UsersResource getUsersResource() {
		RealmResource realmResource = keycloak.realm(realm);
		return realmResource.users();
	}

	/**
	 * Tạo user trên Keycloak từ UserRegistrationRecord.
	 * @param userRegistrationRecord thông tin đăng ký user (không null)
	 * @return UserRepresentation
	 */
	public UserRepresentation createUser(UserRegistrationRecord userRegistrationRecord) {
		if (userRegistrationRecord == null) throw new IllegalArgumentException("UserRegistrationRecord must not be null");
		UserRepresentation user = new UserRepresentation();
		user.setEnabled(true);
		user.setUsername(userRegistrationRecord.username());
		user.setEmail(userRegistrationRecord.email());
		user.setFirstName(userRegistrationRecord.firstName());
		user.setLastName(userRegistrationRecord.lastName());
		user.setEmailVerified(false);

		CredentialRepresentation credential = new CredentialRepresentation();
		credential.setType(CredentialRepresentation.PASSWORD);
		credential.setValue(userRegistrationRecord.password());
		credential.setTemporary(false);

		user.setCredentials(Collections.singletonList(credential));

		UsersResource usersResource = getUsersResource();
		Response response = usersResource.create(user);

		if (response.getStatus() == 201) {
			List<UserRepresentation> users = usersResource.searchByUsername(userRegistrationRecord.username(), true);
			if (!users.isEmpty()) {
				UserRepresentation createdUser = users.get(0);
				log.info("Email was sent to user id {}", createdUser.getId());
				try {
					emailVerification(createdUser.getId());
					log.info("Verification email sent to user id {}", createdUser.getId());
				} catch (Exception e) {
					log.warn("Failed to send verification email to user id {}: {}", createdUser.getId(), e.getMessage());
				}
			}
			return user;
		} else {
			log.error("Error while creating user: {}", response.getStatus());
			throw new RuntimeException("Error creating user: " + response.getStatus());
		}
	}

	/**
	 * Lấy user Keycloak theo id.
	 * @param userId id Keycloak
	 * @return UserRepresentation
	 */
	public UserRepresentation getUserById(String userId) {
		if (userId == null) throw new IllegalArgumentException("userId must not be null");
		log.debug("[KeycloakUserService] Getting user by id: {}", userId);
		return getUsersResource().get(userId).toRepresentation();
	}

	/**
	 * Xoá user Keycloak theo id.
	 * @param userId id Keycloak
	 */
	public void deleteUserById(String userId) {
		if (userId == null) throw new IllegalArgumentException("userId must not be null");
		log.debug("[KeycloakUserService] Deleting user by id: {}", userId);
		getUsersResource().delete(userId);
	}

	/**
	 * Gửi email xác thực cho user Keycloak.
	 * @param userId id Keycloak
	 */
	public void emailVerification(String userId) {
		if (userId == null) throw new IllegalArgumentException("userId must not be null");
		log.debug("[KeycloakUserService] Sending verification email to user id: {}", userId);
		getUsersResource().get(userId).sendVerifyEmail();
	}

	/**
	 * Lấy UserResource Keycloak theo id.
	 * @param userId id Keycloak
	 * @return UserResource
	 */
	public UserResource getUserResource(String userId) {
		if (userId == null) throw new IllegalArgumentException("userId must not be null");
		log.debug("[KeycloakUserService] Getting user resource for id: {}", userId);
		return getUsersResource().get(userId);
	}

	/**
	 * Đổi mật khẩu user Keycloak.
	 * @param userId id Keycloak
	 * @param newPassword mật khẩu mới
	 */
	public void updatePassword(String userId, String newPassword) {
		if (userId == null || newPassword == null) throw new IllegalArgumentException("userId và newPassword không được null");
		log.debug("[KeycloakUserService] Updating password for user id: {}", userId);
		UserResource userResource = getUserResource(userId);
		CredentialRepresentation credential = new CredentialRepresentation();
		credential.setType(CredentialRepresentation.PASSWORD);
		credential.setValue(newPassword);
		credential.setTemporary(false);
		userResource.resetPassword(credential);
		log.info("[KeycloakUserService] Password updated for user id: {}", userId);
	}

	/**
	 * Gửi email reset password cho user Keycloak.
	 * @param username username Keycloak
	 */
	public void forgotPassword(String username) {
		if (username == null) throw new IllegalArgumentException("username must not be null");
		log.debug("[KeycloakUserService] Forgot password for username: {}", username);
		UsersResource usersResource = getUsersResource();
		List<UserRepresentation> userRepresentations = usersResource.searchByUsername(username, true);
		UserRepresentation userRepresentation = userRepresentations.stream().findFirst().orElse(null);
		if (userRepresentation != null) {
			UserResource userResource = usersResource.get(userRepresentation.getId());
			List<String> actions = new ArrayList<>();
			actions.add("UPDATE_PASSWORD");
			userResource.executeActionsEmail(actions);
			log.info("[KeycloakUserService] Password reset email sent to user: {}", username);
			return;
		}
		log.error("[KeycloakUserService] Username not found for forgot password: {}", username);
		throw new RuntimeException("Username not found");
	}

	/**
	 * Tạo user trên Keycloak từ CreateUserRequest và gán roles.
	 * @param dto CreateUserRequest (không null)
	 * @param roles danh sách role
	 * @return id Keycloak
	 */
	public String createUser(CreateUserRequest dto, List<String> roles) {
		if (dto == null) throw new IllegalArgumentException("CreateUserRequest must not be null");
		try {
			log.info("Creating user in Keycloak: username={}, email={}, password={}", dto.getUsername(), dto.getEmail(), dto.getPassword());
			UserRepresentation user = new UserRepresentation();
			user.setEnabled(true);
			user.setUsername(dto.getUsername());
			user.setEmail(dto.getEmail());
			user.setFirstName(dto.getFirstName());
			user.setLastName(dto.getLastName());

			CredentialRepresentation credential = new CredentialRepresentation();
			credential.setType(CredentialRepresentation.PASSWORD);
			credential.setValue(dto.getPassword());
			credential.setTemporary(false);
			user.setCredentials(Collections.singletonList(credential));

			UsersResource usersResource = getUsersResource();
			Response response = usersResource.create(user);
			if (response.getStatus() == 201) {
				List<UserRepresentation> users = usersResource.searchByUsername(dto.getUsername(), true);
				if (!users.isEmpty()) {
					UserRepresentation createdUser = users.get(0);
					assignRolesToUser(createdUser.getId(), roles);
					log.info("Email was sent to user id {}", createdUser.getId());
					try {
						emailVerification(createdUser.getId());
						log.info("Verification email sent to user id {}", createdUser.getId());
					} catch (Exception e) {
						log.warn("Failed to send verification email to user id {}: {}", createdUser.getId(), e.getMessage());
					}
					return createdUser.getId();
				}
			} else {
				String errorMessage = response.readEntity(String.class);
				log.error("Error while creating user: {} - {}", response.getStatus(), errorMessage);
				throw new RuntimeException("Error creating user: " + response.getStatus() + " - " + errorMessage);
			}
			throw new RuntimeException("Unknown error creating user");
		} catch (Exception e) {
			log.error("Exception while creating user in Keycloak: {}", e.getMessage(), e);
			throw new RuntimeException("Exception while creating user in Keycloak: " + e.getMessage(), e);
		}
	}

	/**
	 * Cập nhật user Keycloak từ UpdateUserRequest và roles.
	 * @param keycloakId id Keycloak
	 * @param dto UpdateUserRequest (không null)
	 * @param roles danh sách role
	 */
	public void updateUser(String keycloakId, UpdateUserRequest dto, List<String> roles) {
		if (keycloakId == null || dto == null) throw new IllegalArgumentException("keycloakId và UpdateUserRequest không được null");
		UserResource userResource = getUserResource(keycloakId);
		UserRepresentation user = userResource.toRepresentation();
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
		userResource.update(user);
		if (roles != null && !roles.isEmpty()) {
			assignRolesToUser(keycloakId, roles);
		}
	}

	/**
	 * Lấy roles của user Keycloak.
	 * @param keycloakId id Keycloak
	 * @return List<String> roles
	 */
	public List<String> getUserRoles(String keycloakId) {
		if (keycloakId == null) throw new IllegalArgumentException("keycloakId must not be null");
		UserResource userResource = getUserResource(keycloakId);
		// Lấy client resource
		var client = keycloak.realm(realm).clients().findByClientId(clientId).stream().findFirst();
		if (client.isEmpty()) {
			log.error("Client '{}' not found in realm '{}', cannot get client roles", clientId, realm);
			return Collections.emptyList();
		}
		String clientUuid = client.get().getId();
		List<RoleRepresentation> roles = userResource.roles().clientLevel(clientUuid).listAll();
		List<String> roleNames = new ArrayList<>();
		for (RoleRepresentation role : roles) {
			roleNames.add(role.getName());
		}
		log.info("Getting roles for user {} with clientId {}", keycloakId, clientId);
		log.info("Roles found: {}", roleNames);
		return roleNames;
	}

	/**
	 * Lấy timestamps của user Keycloak.
	 * @param keycloakId id Keycloak
	 * @return Map<String, Long> timestamps
	 */
	public Map<String, Long> getUserTimestamps(String keycloakId) {
		if (keycloakId == null) throw new IllegalArgumentException("keycloakId must not be null");
		UserRepresentation user = getUserById(keycloakId);
		Map<String, Long> timestamps = new HashMap<>();
		if (user.getCreatedTimestamp() != null) {
			timestamps.put("createdAt", user.getCreatedTimestamp());
		}
		// Keycloak không có updatedTimestamp, chỉ có createdTimestamp
		return timestamps;
	}

	/**
	 * Gán roles cho user Keycloak.
	 * @param keycloakId id Keycloak
	 * @param roles danh sách role
	 */
	public void assignRolesToUser(String keycloakId, List<String> roles) {
		if (keycloakId == null) throw new IllegalArgumentException("keycloakId must not be null");
		if (roles == null || roles.isEmpty()) return;
		UserResource userResource = getUserResource(keycloakId);
		// Lấy client resource
		var client = keycloak.realm(realm).clients().findByClientId(clientId).stream().findFirst();
		if (client.isEmpty()) {
			log.error("Client '{}' not found in realm '{}', cannot assign client roles", clientId, realm);
			return;
		}
		String clientUuid = client.get().getId();
		var clientRolesResource = keycloak.realm(realm).clients().get(clientUuid).roles();
		List<RoleRepresentation> toAssign = new ArrayList<>();
		for (String roleName : roles) {
			RoleRepresentation role = null;
			try {
				role = clientRolesResource.get(roleName).toRepresentation();
			} catch (Exception e) {
				log.warn("Client role '{}' does not exist in client '{}', cannot assign to user {}", roleName, clientId, keycloakId);
			}
			if (role != null) {
				toAssign.add(role);
			}
		}
		if (!toAssign.isEmpty()) {
			userResource.roles().clientLevel(clientUuid).add(toAssign);
		}
	}

	/**
	 * Đăng nhập user với username và password
	 * @param username username
	 * @param password password
	 * @return LoginResponse chứa token và thông tin user
	 */
	public knp.ptithcm.datn.user_module.modules.user.dtos.responses.LoginResponse login(String username, String password) {
		if (username == null || username.isBlank()) {
			throw new IllegalArgumentException("Username không được để trống");
		}
		if (password == null || password.isBlank()) {
			throw new IllegalArgumentException("Password không được để trống");
		}
		
		log.debug("[KeycloakUserService] Attempting login for user: {}", username);
		
		try {
			// Tạo Keycloak instance cho user login
			Keycloak userKeycloak = KeycloakBuilder.builder()
				.serverUrl(serverUrl) // Sử dụng biến serverUrl đã được khai báo trong service
				.realm(realm)
				.username(username)
				.password(password)
				.clientId(clientId)
				.clientSecret(clientSecret)
				.grantType("password")
				.build();
			
			// Lấy token
			org.keycloak.representations.AccessTokenResponse tokenResponse = userKeycloak.tokenManager().getAccessToken();
			
			// Tạo LoginResponse
			knp.ptithcm.datn.user_module.modules.user.dtos.responses.LoginResponse loginResponse = knp.ptithcm.datn.user_module.modules.user.dtos.responses.LoginResponse.builder()
				.message("Đăng nhập thành công")
				.accessToken(tokenResponse.getToken())
				.refreshToken(tokenResponse.getRefreshToken())
				.tokenType(tokenResponse.getTokenType())
				.expiresIn((int)tokenResponse.getExpiresIn())
				.user(null) // User info sẽ được set bởi UserService
				.build();
			
			log.info("[KeycloakUserService] Login successful for user: {}", username);
			return loginResponse;
			
		} catch (Exception e) {
			log.error("[KeycloakUserService] Login failed for user: {}, error: {}", username, e.getMessage(), e);
			throw new RuntimeException("Đăng nhập thất bại: " + e.getMessage());
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
		
		log.debug("[KeycloakUserService] Refreshing token");
		
		try {
			// Sử dụng HTTP client để gọi trực tiếp Keycloak token endpoint
			java.net.http.HttpClient httpClient = java.net.http.HttpClient.newHttpClient();
			
			// Tạo form data cho refresh token request
			String formData = String.format(
				"grant_type=refresh_token&refresh_token=%s&client_id=%s&client_secret=%s",
				java.net.URLEncoder.encode(refreshToken, "UTF-8"),
				java.net.URLEncoder.encode(clientId, "UTF-8"),
				java.net.URLEncoder.encode(clientSecret, "UTF-8")
			);
			
			java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
				.uri(java.net.URI.create(serverUrl + "/realms/" + realm + "/protocol/openid-connect/token"))
				.header("Content-Type", "application/x-www-form-urlencoded")
				.POST(java.net.http.HttpRequest.BodyPublishers.ofString(formData))
				.build();
			
			java.net.http.HttpResponse<String> response = httpClient.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
			
			if (response.statusCode() == 200) {
				// Parse JSON response
				com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
				com.fasterxml.jackson.databind.JsonNode jsonResponse = mapper.readTree(response.body());
				
				// Tạo RefreshTokenResponse
				knp.ptithcm.datn.user_module.modules.user.dtos.responses.RefreshTokenResponse refreshResponse = knp.ptithcm.datn.user_module.modules.user.dtos.responses.RefreshTokenResponse.builder()
					.message("Refresh token thành công")
					.accessToken(jsonResponse.get("access_token").asText())
					.refreshToken(jsonResponse.get("refresh_token").asText())
					.tokenType(jsonResponse.get("token_type").asText())
					.expiresIn(jsonResponse.get("expires_in").asInt())
					.build();
				
				log.info("[KeycloakUserService] Token refreshed successfully");
				return refreshResponse;
			} else {
				log.error("[KeycloakUserService] Token refresh failed with status: {}", response.statusCode());
				throw new RuntimeException("Token refresh failed with status: " + response.statusCode());
			}
			
		} catch (Exception e) {
			log.error("[KeycloakUserService] Token refresh failed: {}", e.getMessage(), e);
			throw new RuntimeException("Refresh token thất bại: " + e.getMessage());
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
		
		log.debug("[KeycloakUserService] Logging out user");
		
		try {
			// Sử dụng HTTP client để gọi trực tiếp Keycloak logout endpoint
			java.net.http.HttpClient httpClient = java.net.http.HttpClient.newHttpClient();
			
			// Tạo form data cho logout request
			String formData = String.format(
				"client_id=%s&client_secret=%s&refresh_token=%s",
				java.net.URLEncoder.encode(clientId, "UTF-8"),
				java.net.URLEncoder.encode(clientSecret, "UTF-8"),
				java.net.URLEncoder.encode(refreshToken, "UTF-8")
			);
			
			java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
				.uri(java.net.URI.create(serverUrl + "/realms/" + realm + "/protocol/openid-connect/logout"))
				.header("Content-Type", "application/x-www-form-urlencoded")
				.POST(java.net.http.HttpRequest.BodyPublishers.ofString(formData))
				.build();
			
			java.net.http.HttpResponse<String> response = httpClient.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
			
			// Keycloak logout endpoint thường trả về 204 (No Content) hoặc 200
			if (response.statusCode() == 200 || response.statusCode() == 204) {
				// Tạo LogoutResponse
				knp.ptithcm.datn.user_module.modules.user.dtos.responses.LogoutResponse logoutResponse = knp.ptithcm.datn.user_module.modules.user.dtos.responses.LogoutResponse.builder()
					.message("Đăng xuất thành công")
					.build();
				
				log.info("[KeycloakUserService] User logged out successfully");
				return logoutResponse;
			} else {
				log.error("[KeycloakUserService] Logout failed with status: {}", response.statusCode());
				throw new RuntimeException("Logout failed with status: " + response.statusCode());
			}
			
		} catch (Exception e) {
			log.error("[KeycloakUserService] Logout failed: {}", e.getMessage(), e);
			throw new RuntimeException("Đăng xuất thất bại: " + e.getMessage());
		}
	}
} 
