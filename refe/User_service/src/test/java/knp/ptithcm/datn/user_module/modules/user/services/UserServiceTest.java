package knp.ptithcm.datn.user_module.modules.user.services;

import knp.ptithcm.datn.user_module.modules.user.dtos.requests.RegisterByPhoneRequest;
import knp.ptithcm.datn.user_module.modules.user.dtos.requests.ResetPasswordWithOtpRequest;
import knp.ptithcm.datn.user_module.modules.user.dtos.requests.UpdateProfileRequest;
import knp.ptithcm.datn.user_module.modules.user.entities.User;
import knp.ptithcm.datn.user_module.modules.user.enums.UserStatus;
import knp.ptithcm.datn.user_module.modules.user.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@Tag("service")
@ExtendWith(MockitoExtension.class)
class UserServiceTest {
	@Mock
	private UserRepository userRepository;

	@Mock
	private KeycloakUserService keycloakUserService;

	@Spy
	private OtpService otpService;

	@InjectMocks
	private UserService userService;

	@BeforeEach
	void setup() {
		// Ensure real behavior for OtpService
		otpService = new OtpService();
		userService = new UserService();
		// Inject mocks manually since fields are @Autowired in production
		try {
			java.lang.reflect.Field f1 = UserService.class.getDeclaredField("userRepository");
			f1.setAccessible(true);
			f1.set(userService, userRepository);
			java.lang.reflect.Field f2 = UserService.class.getDeclaredField("keycloakUserService");
			f2.setAccessible(true);
			f2.set(userService, keycloakUserService);
			java.lang.reflect.Field f3 = UserService.class.getDeclaredField("otpService");
			f3.setAccessible(true);
			f3.set(userService, otpService);
		} catch (Exception e) {
			fail("Failed to inject dependencies: " + e.getMessage());
		}
	}

	@Test
	void registerByPhone_success() {
		String phone = "+84123456789";
		when(userRepository.existsByPhone(phone)).thenReturn(false);
		when(keycloakUserService.createUser(any(), anyList())).thenReturn("kc-1");
		when(keycloakUserService.getUserRoles("kc-1")).thenReturn(java.util.List.of("USER"));
		when(keycloakUserService.getUserTimestamps("kc-1")).thenReturn(Map.of("createdAt", System.currentTimeMillis()));
		when(userRepository.save(any(User.class))).thenAnswer(inv -> {
			User u = inv.getArgument(0);
			u.setId(UUID.randomUUID());
			u.setCreatedAt(LocalDateTime.now());
			u.setUpdatedAt(u.getCreatedAt());
			return u;
		});

		RegisterByPhoneRequest req = new RegisterByPhoneRequest();
		req.setPhone(phone);
		req.setPassword("Password123!");
		req.setFirstName("Khoa");
		req.setLastName("Nguyen");

		User user = userService.registerByPhone(req);
		assertNotNull(user);
		assertEquals(phone, user.getPhone());
		assertEquals(UserStatus.ACTIVE, user.getStatus());
		verify(userRepository, times(2)).save(any(User.class));
		verify(keycloakUserService, times(1)).createUser(any(), anyList());
	}

	@Test
	void phoneExists_true() {
		when(userRepository.existsByPhone("p")).thenReturn(true);
		assertTrue(userService.phoneExists("p"));
	}

	@Test
	void otp_send_and_verify_fixed_code() {
		String phone = "+84999999999";
		String code = userService.sendOtp(phone);
		assertEquals("000000", code);
		assertTrue(userService.verifyOtp(phone, "000000"));
	}

	@Test
	void resetPasswordWithOtp_success() {
		String phone = "+84777777777";
		userService.sendOtp(phone);
		User user = new User();
		user.setId(UUID.randomUUID());
		user.setKeycloakId("kc-2");
		when(userRepository.findByPhone(phone)).thenReturn(Optional.of(user));

		ResetPasswordWithOtpRequest req = new ResetPasswordWithOtpRequest();
		req.setPhone(phone);
		req.setOtp("000000");
		req.setNewPassword("NewPassword456!");
		userService.resetPasswordWithOtp(req);
		verify(keycloakUserService, times(1)).updatePassword("kc-2", "NewPassword456!");
	}

	@Test
	void updateProfile_success() {
		UUID id = UUID.randomUUID();
		User user = new User();
		user.setId(id);
		user.setFirstName("Old");
		user.setLastName("Name");
		when(userRepository.findById(id)).thenReturn(Optional.of(user));
		when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

		UpdateProfileRequest req = new UpdateProfileRequest();
		req.setId(id.toString());
		req.setFirstName("New");
		req.setLastName("Name");
		req.setPhone("+8412345");
		req.setAddress("Addr");
		req.setIdentityNumber("ID123");

		User updated = userService.updateProfile(req);
		assertEquals("New", updated.getFirstName());
		assertEquals("Name", updated.getLastName());
		assertEquals("+8412345", updated.getPhone());
		assertEquals("Addr", updated.getAddress());
		assertEquals("ID123", updated.getIdentityNumber());
		verify(userRepository, times(1)).save(any(User.class));
	}
}
