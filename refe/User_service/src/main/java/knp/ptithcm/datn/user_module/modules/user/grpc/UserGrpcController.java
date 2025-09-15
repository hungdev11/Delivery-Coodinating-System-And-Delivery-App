package knp.ptithcm.datn.user_module.modules.user.grpc;

import io.grpc.stub.StreamObserver;
import knp.ptithcm.datn.user_module.modules.base.grpc.*;
import knp.ptithcm.datn.user_module.modules.user.enums.UserRole;
import knp.ptithcm.datn.user_module.modules.user.enums.UserStatus;
import knp.ptithcm.datn.user_module.modules.user.mappers.UserGrpcMapper;
import knp.ptithcm.datn.user_module.modules.user.services.UserService;
import knp.ptithcm.datn.user_module.modules.user.dtos.responses.UserResponse;
import knp.ptithcm.datn.user_module.modules.user.entities.User;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.grpc.server.service.GrpcService;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * gRPC Controller cho user, chỉ gọi UserService, không gọi KeycloakUserService trực tiếp.
 * Thêm log cho mỗi endpoint, gom mapping, JavaDoc rõ ràng.
 */
@GrpcService
public class UserGrpcController extends UserServiceGrpc.UserServiceImplBase {
    private static final Logger log = LoggerFactory.getLogger(UserGrpcController.class);
    @Autowired
    private UserService userService;

    /**
     * Helper: Mapping User entity sang gRPC User proto.
     */
    private UserGrpc mapToProto(User user) {
        return UserGrpcMapper.toProto(user);
    }

    @Override
    public void getUserById(GetUserByIdRequestGrpc request, StreamObserver<UserResponseGrpc> responseObserver) {
        log.debug("[gRPC] getUserById called: {}", request.getId());
        try {
            UUID id = UUID.fromString(request.getId());
            Optional<User> userOpt = userService.getUser(id);
            if (userOpt.isPresent()) {
                UserResponseGrpc response = UserResponseGrpc.newBuilder()
                        .setMessage("OK")
                        .setUser(mapToProto(userOpt.get()))
                        .build();
                responseObserver.onNext(response);
                responseObserver.onCompleted();
                log.info("[gRPC] getUserById success: {}", request.getId());
            } else {
                log.warn("[gRPC] getUserById not found: {}", request.getId());
                responseObserver.onError(io.grpc.Status.NOT_FOUND
                        .withDescription("User not found with id: " + request.getId())
                        .asRuntimeException());
            }
        } catch (Exception e) {
            log.error("[gRPC] getUserById error: {}", e.getMessage(), e);
            responseObserver.onError(io.grpc.Status.INTERNAL
                    .withDescription("Internal server error: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void getUserByUsername(GetUserByUsernameRequestGrpc request, StreamObserver<UserResponseGrpc> responseObserver) {
        log.debug("[gRPC] getUserByUsername called: {}", request.getUsername());
        try {
            Optional<User> userOpt = userService.getUserByUsername(request.getUsername());
            if (userOpt.isPresent()) {
                UserResponseGrpc response = UserResponseGrpc.newBuilder()
                        .setMessage("OK")
                        .setUser(mapToProto(userOpt.get()))
                        .build();
                responseObserver.onNext(response);
                responseObserver.onCompleted();
                log.info("[gRPC] getUserByUsername success: {}", request.getUsername());
            } else {
                log.warn("[gRPC] getUserByUsername not found: {}", request.getUsername());
                responseObserver.onError(io.grpc.Status.NOT_FOUND
                        .withDescription("User not found with username: " + request.getUsername())
                        .asRuntimeException());
            }
        } catch (Exception e) {
            log.error("[gRPC] getUserByUsername error: {}", e.getMessage(), e);
            responseObserver.onError(io.grpc.Status.INTERNAL
                    .withDescription("Internal server error: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void getUserByEmail(GetUserByEmailRequestGrpc request, StreamObserver<UserResponseGrpc> responseObserver) {
        log.debug("[gRPC] getUserByEmail called: {}", request.getEmail());
        try {
            Optional<User> userOpt = userService.getUserByEmail(request.getEmail());
            if (userOpt.isPresent()) {
                UserResponseGrpc response = UserResponseGrpc.newBuilder()
                        .setMessage("OK")
                        .setUser(mapToProto(userOpt.get()))
                        .build();
                responseObserver.onNext(response);
                responseObserver.onCompleted();
                log.info("[gRPC] getUserByEmail success: {}", request.getEmail());
            } else {
                log.warn("[gRPC] getUserByEmail not found: {}", request.getEmail());
                responseObserver.onError(io.grpc.Status.NOT_FOUND
                        .withDescription("User not found with email: " + request.getEmail())
                        .asRuntimeException());
            }
        } catch (Exception e) {
            log.error("[gRPC] getUserByEmail error: {}", e.getMessage(), e);
            responseObserver.onError(io.grpc.Status.INTERNAL
                    .withDescription("Internal server error: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void listUsers(ListUsersRequestGrpc request, StreamObserver<ListUsersResponseGrpc> responseObserver) {
        log.debug("[gRPC] listUsers called");
        try {
            List<UserResponse> users = userService.listUsers();
            ListUsersResponseGrpc.Builder builder = ListUsersResponseGrpc.newBuilder();
            for (UserResponse dto : users) {
                builder.addUsers(UserGrpcMapper.toProto(dto));
            }
            builder.setTotal(users.size());
            builder.setMessage("OK");
            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
            log.info("[gRPC] listUsers success, total: {}", users.size());
        } catch (Exception e) {
            log.error("[gRPC] listUsers error: {}", e.getMessage(), e);
            responseObserver.onError(io.grpc.Status.INTERNAL
                    .withDescription("Internal server error: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void updateUserStatus(UpdateUserStatusRequestGrpc request, StreamObserver<UpdateUserStatusResponseGrpc> responseObserver) {
        log.debug("[gRPC] updateUserStatus called: {} -> {}", request.getId(), request.getStatus());
        try {
            UUID id = UUID.fromString(request.getId());
            User user = userService.updateUserStatus(id, UserStatus.values()[request.getStatus()]);
            UpdateUserStatusResponseGrpc response = UpdateUserStatusResponseGrpc.newBuilder()
                    .setMessage("OK")
                    .setUser(mapToProto(user))
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
            log.info("[gRPC] updateUserStatus success: {} -> {}", request.getId(), request.getStatus());
        } catch (Exception e) {
            log.error("[gRPC] updateUserStatus error: {}", e.getMessage(), e);
            responseObserver.onError(io.grpc.Status.INTERNAL
                    .withDescription("Internal server error: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void updateUserRole(UpdateUserRoleRequestGrpc request, StreamObserver<UpdateUserRoleResponseGrpc> responseObserver) {
        log.debug("[gRPC] updateUserRole called: {} -> {}", request.getId(), request.getRole());
        try {
            UUID id = UUID.fromString(request.getId());
            User user = userService.updateUserRole(id, UserRole.values()[request.getRole()]);
            UpdateUserRoleResponseGrpc response = UpdateUserRoleResponseGrpc.newBuilder()
                    .setMessage("OK")
                    .setUser(mapToProto(user))
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
            log.info("[gRPC] updateUserRole success: {} -> {}", request.getId(), request.getRole());
        } catch (Exception e) {
            log.error("[gRPC] updateUserRole error: {}", e.getMessage(), e);
            responseObserver.onError(io.grpc.Status.INTERNAL
                    .withDescription("Internal server error: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void createUser(CreateUserRequestGrpc request, StreamObserver<UserResponseGrpc> responseObserver) {
        log.debug("[gRPC] createUser called: {}", request.getUsername());
        try {
            knp.ptithcm.datn.user_module.modules.user.dtos.requests.CreateUserRequest dto = new knp.ptithcm.datn.user_module.modules.user.dtos.requests.CreateUserRequest();
            dto.setUsername(request.getUsername());
            dto.setEmail(request.getEmail());
            dto.setFirstName(request.getFirstName());
            dto.setLastName(request.getLastName());
            dto.setPhone(request.getPhone());
            dto.setAddress(request.getAddress());
            dto.setIdentityNumber(request.getIdentityNumber());
            dto.setPassword(request.getPassword());
            dto.setRoles(request.getRolesList());
            User user = userService.createUser(dto);
            // Lấy lại roles từ Keycloak
            List<String> roles = userService.getUserRolesByKeycloakId(user.getKeycloakId());
            knp.ptithcm.datn.user_module.modules.user.dtos.responses.UserResponse userDto = knp.ptithcm.datn.user_module.modules.user.mappers.UserEntityMapper.toUserDto(user);
            userDto.setRoles(roles != null ? roles : java.util.List.of());
            UserResponseGrpc response = UserResponseGrpc.newBuilder()
                    .setMessage("User created successfully")
                    .setUser(knp.ptithcm.datn.user_module.modules.user.mappers.UserGrpcMapper.toProto(userDto))
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
            log.info("[gRPC] createUser success: {}", request.getUsername());
        } catch (Exception e) {
            log.error("[gRPC] createUser error: {}", e.getMessage(), e);
            responseObserver.onError(io.grpc.Status.INTERNAL
                    .withDescription("Internal server error: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void updateUser(UpdateUserRequestGrpc request, StreamObserver<UserResponseGrpc> responseObserver) {
        log.debug("[gRPC] updateUser called: {}", request.getId());
        try {
            knp.ptithcm.datn.user_module.modules.user.dtos.requests.UpdateUserRequest dto = new knp.ptithcm.datn.user_module.modules.user.dtos.requests.UpdateUserRequest();
            dto.setId(request.getId());
            dto.setUsername(request.getUsername());
            dto.setEmail(request.getEmail());
            dto.setFullName(request.getFirstName() + " " + request.getLastName());
            // Add phone, address, identity_number if needed
            User user = userService.updateUser(UUID.fromString(request.getId()), dto);
            UserResponseGrpc response = UserResponseGrpc.newBuilder()
                    .setMessage("OK")
                    .setUser(mapToProto(user))
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
            log.info("[gRPC] updateUser success: {}", request.getId());
        } catch (Exception e) {
            log.error("[gRPC] updateUser error: {}", e.getMessage(), e);
            responseObserver.onError(io.grpc.Status.INTERNAL
                    .withDescription("Internal server error: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void updateUserPassword(UpdateUserPasswordRequestGrpc request, StreamObserver<UserResponseGrpc> responseObserver) {
        log.debug("[gRPC] updateUserPassword called: {}", request.getId());
        try {
            User user = userService.updateUserPassword(UUID.fromString(request.getId()), request.getNewPassword());
            UserResponseGrpc response = UserResponseGrpc.newBuilder()
                    .setMessage("Password updated successfully")
                    .setUser(mapToProto(user))
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
            log.info("[gRPC] updateUserPassword success: {}", request.getId());
        } catch (Exception e) {
            log.error("[gRPC] updateUserPassword error: {}", e.getMessage(), e);
            responseObserver.onError(io.grpc.Status.INTERNAL
                    .withDescription("Internal server error: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void registerByPhone(RegisterByPhoneRequestGrpc request, StreamObserver<UserResponseGrpc> responseObserver) {
        log.debug("[gRPC] registerByPhone called: {}", request.getPhone());
        try {
            knp.ptithcm.datn.user_module.modules.user.dtos.requests.RegisterByPhoneRequest dto = new knp.ptithcm.datn.user_module.modules.user.dtos.requests.RegisterByPhoneRequest();
            dto.setPhone(request.getPhone());
            dto.setPassword(request.getPassword());
            dto.setFirstName(request.getFirstName());
            dto.setLastName(request.getLastName());
            User user = userService.registerByPhone(dto);
            UserResponseGrpc response = UserResponseGrpc.newBuilder()
                    .setMessage("OK")
                    .setUser(mapToProto(user))
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
            log.info("[gRPC] registerByPhone success: {}", request.getPhone());
        } catch (Exception e) {
            log.error("[gRPC] registerByPhone error: {}", e.getMessage(), e);
            responseObserver.onError(io.grpc.Status.INTERNAL
                    .withDescription("Internal server error: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void phoneExists(PhoneExistsRequestGrpc request, StreamObserver<PhoneExistsResponseGrpc> responseObserver) {
        log.debug("[gRPC] phoneExists called: {}", request.getPhone());
        try {
            boolean exists = userService.phoneExists(request.getPhone());
            PhoneExistsResponseGrpc response = PhoneExistsResponseGrpc.newBuilder()
                    .setMessage("OK")
                    .setExists(exists)
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
            log.info("[gRPC] phoneExists success: {} -> {}", request.getPhone(), exists);
        } catch (Exception e) {
            log.error("[gRPC] phoneExists error: {}", e.getMessage(), e);
            responseObserver.onError(io.grpc.Status.INTERNAL
                    .withDescription("Internal server error: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void sendOtp(SendOtpRequestGrpc request, StreamObserver<SendOtpResponseGrpc> responseObserver) {
        log.debug("[gRPC] sendOtp called: {}", request.getPhone());
        try {
            String otp = userService.sendOtp(request.getPhone());
            SendOtpResponseGrpc response = SendOtpResponseGrpc.newBuilder()
                    .setMessage("OK")
                    .setCode(otp)
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
            log.info("[gRPC] sendOtp success: {}", request.getPhone());
        } catch (Exception e) {
            log.error("[gRPC] sendOtp error: {}", e.getMessage(), e);
            responseObserver.onError(io.grpc.Status.INTERNAL
                    .withDescription("Internal server error: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void verifyOtp(VerifyOtpRequestGrpc request, StreamObserver<VerifyOtpResponseGrpc> responseObserver) {
        log.debug("[gRPC] verifyOtp called: {}", request.getPhone());
        try {
            boolean ok = userService.verifyOtp(request.getPhone(), request.getOtp());
            VerifyOtpResponseGrpc response = VerifyOtpResponseGrpc.newBuilder()
                    .setMessage(ok ? "VALID" : "INVALID")
                    .setValid(ok)
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
            log.info("[gRPC] verifyOtp success: {} -> {}", request.getPhone(), ok);
        } catch (Exception e) {
            log.error("[gRPC] verifyOtp error: {}", e.getMessage(), e);
            responseObserver.onError(io.grpc.Status.INTERNAL
                    .withDescription("Internal server error: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void resetPasswordWithOtp(ResetPasswordWithOtpRequestGrpc request, StreamObserver<ResetPasswordWithOtpResponseGrpc> responseObserver) {
        log.debug("[gRPC] resetPasswordWithOtp called: {}", request.getPhone());
        try {
            knp.ptithcm.datn.user_module.modules.user.dtos.requests.ResetPasswordWithOtpRequest dto = new knp.ptithcm.datn.user_module.modules.user.dtos.requests.ResetPasswordWithOtpRequest();
            dto.setPhone(request.getPhone());
            dto.setOtp(request.getOtp());
            dto.setNewPassword(request.getNewPassword());
            userService.resetPasswordWithOtp(dto);
            ResetPasswordWithOtpResponseGrpc response = ResetPasswordWithOtpResponseGrpc.newBuilder()
                    .setMessage("OK")
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
            log.info("[gRPC] resetPasswordWithOtp success: {}", request.getPhone());
        } catch (Exception e) {
            log.error("[gRPC] resetPasswordWithOtp error: {}", e.getMessage(), e);
            responseObserver.onError(io.grpc.Status.INTERNAL
                    .withDescription("Internal server error: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void updateProfile(UpdateProfileRequestGrpc request, StreamObserver<UserResponseGrpc> responseObserver) {
        log.debug("[gRPC] updateProfile called: {}", request.getId());
        try {
            knp.ptithcm.datn.user_module.modules.user.dtos.requests.UpdateProfileRequest dto = new knp.ptithcm.datn.user_module.modules.user.dtos.requests.UpdateProfileRequest();
            dto.setId(request.getId());
            dto.setFirstName(request.getFirstName());
            dto.setLastName(request.getLastName());
            dto.setPhone(request.getPhone());
            dto.setAddress(request.getAddress());
            dto.setIdentityNumber(request.getIdentityNumber());
            User user = userService.updateProfile(dto);
            UserResponseGrpc response = UserResponseGrpc.newBuilder()
                    .setMessage("OK")
                    .setUser(mapToProto(user))
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
            log.info("[gRPC] updateProfile success: {}", request.getId());
        } catch (Exception e) {
            log.error("[gRPC] updateProfile error: {}", e.getMessage(), e);
            responseObserver.onError(io.grpc.Status.INTERNAL
                    .withDescription("Internal server error: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void login(LoginRequestGrpc request, StreamObserver<LoginResponseGrpc> responseObserver) {
        log.debug("[gRPC] login called: {}", request.getUsername());
        try {
            knp.ptithcm.datn.user_module.modules.user.dtos.responses.LoginResponse loginResponse = 
                userService.login(request.getUsername(), request.getPassword());
            
            LoginResponseGrpc response = LoginResponseGrpc.newBuilder()
                    .setMessage(loginResponse.getMessage())
                    .setAccessToken(loginResponse.getAccessToken())
                    .setRefreshToken(loginResponse.getRefreshToken())
                    .setTokenType(loginResponse.getTokenType())
                    .setExpiresIn(loginResponse.getExpiresIn())
                    .setUser(UserGrpcMapper.toProto(loginResponse.getUser()))
                    .build();
            
            responseObserver.onNext(response);
            responseObserver.onCompleted();
            log.info("[gRPC] login success: {}", request.getUsername());
        } catch (Exception e) {
            log.error("[gRPC] login error: {}", e.getMessage(), e);
            responseObserver.onError(io.grpc.Status.INTERNAL
                    .withDescription("Internal server error: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void refreshToken(RefreshTokenRequestGrpc request, StreamObserver<RefreshTokenResponseGrpc> responseObserver) {
        log.debug("[gRPC] refreshToken called");
        try {
            knp.ptithcm.datn.user_module.modules.user.dtos.responses.RefreshTokenResponse refreshResponse = 
                userService.refreshToken(request.getRefreshToken());
            
            RefreshTokenResponseGrpc response = RefreshTokenResponseGrpc.newBuilder()
                    .setMessage(refreshResponse.getMessage())
                    .setAccessToken(refreshResponse.getAccessToken())
                    .setRefreshToken(refreshResponse.getRefreshToken())
                    .setTokenType(refreshResponse.getTokenType())
                    .setExpiresIn(refreshResponse.getExpiresIn())
                    .build();
            
            responseObserver.onNext(response);
            responseObserver.onCompleted();
            log.info("[gRPC] refreshToken success");
        } catch (Exception e) {
            log.error("[gRPC] refreshToken error: {}", e.getMessage(), e);
            responseObserver.onError(io.grpc.Status.INTERNAL
                    .withDescription("Internal server error: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void logout(LogoutRequestGrpc request, StreamObserver<LogoutResponseGrpc> responseObserver) {
        log.debug("[gRPC] logout called");
        try {
            knp.ptithcm.datn.user_module.modules.user.dtos.responses.LogoutResponse logoutResponse = 
                userService.logout(request.getRefreshToken());
            
            LogoutResponseGrpc response = LogoutResponseGrpc.newBuilder()
                    .setMessage(logoutResponse.getMessage())
                    .build();
            
            responseObserver.onNext(response);
            responseObserver.onCompleted();
            log.info("[gRPC] logout success");
        } catch (Exception e) {
            log.error("[gRPC] logout error: {}", e.getMessage(), e);
            responseObserver.onError(io.grpc.Status.INTERNAL
                    .withDescription("Internal server error: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void deleteUser(DeleteUserRequestGrpc request, StreamObserver<DeleteUserResponseGrpc> responseObserver) {
        log.debug("[gRPC] deleteUser called: {}", request.getId());
        try {
            userService.deleteUser(UUID.fromString(request.getId()));
            DeleteUserResponseGrpc response = DeleteUserResponseGrpc.newBuilder()
                    .setMessage("User deleted successfully")
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
            log.info("[gRPC] deleteUser success: {}", request.getId());
        } catch (Exception e) {
            log.error("[gRPC] deleteUser error: {}", e.getMessage(), e);
            responseObserver.onError(io.grpc.Status.INTERNAL
                    .withDescription("Internal server error: " + e.getMessage())
                    .asRuntimeException());
        }
    }
}
