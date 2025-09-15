package knp.ptithcm.datn.user_module.modules.user.grpc;

import io.grpc.stub.StreamObserver;
import knp.ptithcm.datn.user_module.modules.base.grpc.*;
import knp.ptithcm.datn.user_module.modules.user.entities.User;
import knp.ptithcm.datn.user_module.modules.user.enums.UserStatus;
import knp.ptithcm.datn.user_module.modules.user.services.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@Tag("grpc")
@ExtendWith(MockitoExtension.class)
class UserGrpcControllerTest {
	@Mock
	private UserService userService;

	@InjectMocks
	private UserGrpcController controller;

	private static class CapturingObserver<T> implements StreamObserver<T> {
		T value;
		Throwable error;
		boolean completed;
		@Override public void onNext(T v) { this.value = v; }
		@Override public void onError(Throwable t) { this.error = t; }
		@Override public void onCompleted() { this.completed = true; }
	}

	private User stubUser(UUID id) {
		User u = new User();
		u.setId(id);
		u.setUsername("u" + id);
		u.setEmail("e@example.com");
		u.setFirstName("First");
		u.setLastName("Last");
		u.setKeycloakId("kc-" + id);
		u.setStatus(UserStatus.ACTIVE);
		u.setPhone("+8400000");
		return u;
	}

	@Test
	void registerByPhone_success() {
		UUID id = UUID.randomUUID();
		when(userService.registerByPhone(any())).thenReturn(stubUser(id));
		RegisterByPhoneRequestGrpc req = RegisterByPhoneRequestGrpc.newBuilder()
				.setPhone("+84123456789")
				.setPassword("Password123!")
				.setFirstName("Khoa")
				.setLastName("Nguyen")
				.build();
		CapturingObserver<UserResponseGrpc> obs = new CapturingObserver<>();
		controller.registerByPhone(req, obs);
		assertNull(obs.error);
		assertTrue(obs.completed);
		assertNotNull(obs.value);
		assertEquals("OK", obs.value.getMessage());
		assertEquals(id.toString(), obs.value.getUser().getId());
		verify(userService, times(1)).registerByPhone(any());
	}

	@Test
	void phoneExists_true() {
		when(userService.phoneExists("+84111111111")).thenReturn(true);
		PhoneExistsRequestGrpc req = PhoneExistsRequestGrpc.newBuilder().setPhone("+84111111111").build();
		CapturingObserver<PhoneExistsResponseGrpc> obs = new CapturingObserver<>();
		controller.phoneExists(req, obs);
		assertNull(obs.error);
		assertTrue(obs.completed);
		assertTrue(obs.value.getExists());
		verify(userService, times(1)).phoneExists("+84111111111");
	}

	@Test
	void sendOtp_returns_fixed_code() {
		when(userService.sendOtp("+84222222222")).thenReturn("000000");
		SendOtpRequestGrpc req = SendOtpRequestGrpc.newBuilder().setPhone("+84222222222").build();
		CapturingObserver<SendOtpResponseGrpc> obs = new CapturingObserver<>();
		controller.sendOtp(req, obs);
		assertNull(obs.error);
		assertTrue(obs.completed);
		assertEquals("000000", obs.value.getCode());
		verify(userService, times(1)).sendOtp("+84222222222");
	}

	@Test
	void verifyOtp_valid() {
		when(userService.verifyOtp("+84333333333", "000000")).thenReturn(true);
		VerifyOtpRequestGrpc req = VerifyOtpRequestGrpc.newBuilder().setPhone("+84333333333").setOtp("000000").build();
		CapturingObserver<VerifyOtpResponseGrpc> obs = new CapturingObserver<>();
		controller.verifyOtp(req, obs);
		assertNull(obs.error);
		assertTrue(obs.completed);
		assertTrue(obs.value.getValid());
		verify(userService, times(1)).verifyOtp("+84333333333", "000000");
	}

	@Test
	void resetPasswordWithOtp_ok() {
		doNothing().when(userService).resetPasswordWithOtp(any());
		ResetPasswordWithOtpRequestGrpc req = ResetPasswordWithOtpRequestGrpc.newBuilder()
				.setPhone("+84444444444")
				.setOtp("000000")
				.setNewPassword("NewPass@123")
				.build();
		CapturingObserver<ResetPasswordWithOtpResponseGrpc> obs = new CapturingObserver<>();
		controller.resetPasswordWithOtp(req, obs);
		assertNull(obs.error);
		assertTrue(obs.completed);
		assertEquals("OK", obs.value.getMessage());
		verify(userService, times(1)).resetPasswordWithOtp(any());
	}

	@Test
	void updateProfile_success() {
		UUID id = UUID.randomUUID();
		when(userService.updateProfile(any())).thenReturn(stubUser(id));
		UpdateProfileRequestGrpc req = UpdateProfileRequestGrpc.newBuilder()
				.setId(id.toString())
				.setFirstName("New")
				.setLastName("Name")
				.setPhone("+84555555555")
				.setAddress("Addr")
				.setIdentityNumber("ID123")
				.build();
		CapturingObserver<UserResponseGrpc> obs = new CapturingObserver<>();
		controller.updateProfile(req, obs);
		assertNull(obs.error);
		assertTrue(obs.completed);
		assertEquals(id.toString(), obs.value.getUser().getId());
		verify(userService, times(1)).updateProfile(any());
	}
}
