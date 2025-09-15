package ptithcm.graduation.apigateway.services.baseService.user.business;

import ptithcm.graduation.apigateway.services.baseService.user.dto.*;
import ptithcm.graduation.apigateway.services.baseService.user.interfaces.IBaseUserBusinessService;
import ptithcm.graduation.apigateway.services.baseService.user.mapper.BaseUserMapper;
import knp.ptithcm.datn.user_module.modules.base.grpc.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import net.devh.boot.grpc.client.inject.GrpcClient;
import java.util.concurrent.CompletableFuture;

@Service
public class BaseUserBusinessService implements IBaseUserBusinessService {

    @GrpcClient("user-service")
    private UserServiceGrpc.UserServiceFutureStub userServiceStub;

    @Autowired
    private BaseUserMapper userMapper;

    @Override
    public CompletableFuture<BaseUserResponseDto> getUserById(String id) {
        GetUserByIdRequestGrpc request = userMapper.toGetByIdProto(id);
        return CompletableFuture.supplyAsync(() -> {
            try {
                UserResponseGrpc response = userServiceStub.getUserById(request).get();
                return userMapper.toDto(response);
            } catch (Exception e) {
                throw new RuntimeException("Error getting user by id: " + e.getMessage(), e);
            }
        });
    }

    @Override
    public CompletableFuture<BaseUserResponseDto> getUserByUsername(String username) {
        GetUserByUsernameRequestGrpc request = userMapper.toGetByUsernameProto(username);
        return CompletableFuture.supplyAsync(() -> {
            try {
                UserResponseGrpc response = userServiceStub.getUserByUsername(request).get();
                return userMapper.toDto(response);
            } catch (Exception e) {
                throw new RuntimeException("Error getting user by username: " + e.getMessage(), e);
            }
        });
    }

    @Override
    public CompletableFuture<BaseUserResponseDto> getUserByEmail(String email) {
        GetUserByEmailRequestGrpc request = userMapper.toGetByEmailProto(email);
        return CompletableFuture.supplyAsync(() -> {
            try {
                UserResponseGrpc response = userServiceStub.getUserByEmail(request).get();
                return userMapper.toDto(response);
            } catch (Exception e) {
                throw new RuntimeException("Error getting user by email: " + e.getMessage(), e);
            }
        });
    }

    @Override
    public CompletableFuture<BaseListUsersResponseDto> listUsers(BaseListUsersRequestDto request) {
        ListUsersRequestGrpc grpcRequest = userMapper.toProto(request);
        return CompletableFuture.supplyAsync(() -> {
            try {
                ListUsersResponseGrpc response = userServiceStub.listUsers(grpcRequest).get();
                return userMapper.toDto(response);
            } catch (Exception e) {
                throw new RuntimeException("Error listing users: " + e.getMessage(), e);
            }
        });
    }

    @Override
    public CompletableFuture<BaseUserResponseDto> createUser(BaseCreateUserRequestDto request) {
        CreateUserRequestGrpc grpcRequest = userMapper.toProto(request);
        return CompletableFuture.supplyAsync(() -> {
            try {
                UserResponseGrpc response = userServiceStub.createUser(grpcRequest).get();
                return userMapper.toDto(response);
            } catch (Exception e) {
                throw new RuntimeException("Error creating user: " + e.getMessage(), e);
            }
        });
    }

    @Override
    public CompletableFuture<BaseUserResponseDto> updateUser(BaseUpdateUserRequestDto request) {
        UpdateUserRequestGrpc grpcRequest = userMapper.toProto(request);
        return CompletableFuture.supplyAsync(() -> {
            try {
                UserResponseGrpc response = userServiceStub.updateUser(grpcRequest).get();
                return userMapper.toDto(response);
            } catch (Exception e) {
                throw new RuntimeException("Error updating user: " + e.getMessage(), e);
            }
        });
    }

    @Override
    public CompletableFuture<BaseUserResponseDto> updateUserPassword(String id, String newPassword) {
        UpdateUserPasswordRequestGrpc request = userMapper.toUpdatePasswordProto(id, newPassword);
        return CompletableFuture.supplyAsync(() -> {
            try {
                UserResponseGrpc response = userServiceStub.updateUserPassword(request).get();
                return userMapper.toDto(response);
            } catch (Exception e) {
                throw new RuntimeException("Error updating user password: " + e.getMessage(), e);
            }
        });
    }

    @Override
    public CompletableFuture<String> deleteUser(String id) {
        DeleteUserRequestGrpc request = userMapper.toDeleteProto(id);
        return CompletableFuture.supplyAsync(() -> {
            try {
                DeleteUserResponseGrpc response = userServiceStub.deleteUser(request).get();
                return response.getMessage();
            } catch (Exception e) {
                throw new RuntimeException("Error deleting user: " + e.getMessage(), e);
            }
        });
    }

    @Override
    public CompletableFuture<BaseUserResponseDto> updateUserStatus(String id, Integer status) {
        UpdateUserStatusRequestGrpc request = userMapper.toUpdateStatusProto(id, status);
        return CompletableFuture.supplyAsync(() -> {
            try {
                UpdateUserStatusResponseGrpc response = userServiceStub.updateUserStatus(request).get();
                // Map UpdateUserStatusResponseGrpc to UserResponseDto
                BaseUserResponseDto dto = new BaseUserResponseDto();
                dto.setMessage(response.getMessage());
                dto.setUser(userMapper.toDto(response.getUser()));
                return dto;
            } catch (Exception e) {
                throw new RuntimeException("Error updating user status: " + e.getMessage(), e);
            }
        });
    }

    @Override
    public CompletableFuture<BaseUserResponseDto> updateUserRole(String id, Integer role) {
        UpdateUserRoleRequestGrpc request = userMapper.toUpdateRoleProto(id, role);
        return CompletableFuture.supplyAsync(() -> {
            try {
                UpdateUserRoleResponseGrpc response = userServiceStub.updateUserRole(request).get();
                // Map UpdateUserRoleResponseGrpc to UserResponseDto
                BaseUserResponseDto dto = new BaseUserResponseDto();
                dto.setMessage(response.getMessage());
                dto.setUser(userMapper.toDto(response.getUser()));
                return dto;
            } catch (Exception e) {
                throw new RuntimeException("Error updating user role: " + e.getMessage(), e);
            }
        });
    }

    @Override
    public CompletableFuture<BaseUserResponseDto> registerByPhone(String phone, String password, String firstName, String lastName) {
        RegisterByPhoneRequestGrpc request = userMapper.toRegisterByPhoneProto(phone, password, firstName, lastName);
        return CompletableFuture.supplyAsync(() -> {
            try {
                UserResponseGrpc response = userServiceStub.registerByPhone(request).get();
                return userMapper.toDto(response);
            } catch (Exception e) {
                throw new RuntimeException("Error registering by phone: " + e.getMessage(), e);
            }
        });
    }

    @Override
    public CompletableFuture<BasePhoneOtpDto> phoneExists(String phone) {
        PhoneExistsRequestGrpc request = userMapper.toPhoneExistsProto(phone);
        return CompletableFuture.supplyAsync(() -> {
            try {
                PhoneExistsResponseGrpc response = userServiceStub.phoneExists(request).get();
                return userMapper.toDto(response);
            } catch (Exception e) {
                throw new RuntimeException("Error checking phone exists: " + e.getMessage(), e);
            }
        });
    }

    @Override
    public CompletableFuture<BasePhoneOtpDto> sendOtp(String phone) {
        SendOtpRequestGrpc request = userMapper.toSendOtpProto(phone);
        return CompletableFuture.supplyAsync(() -> {
            try {
                SendOtpResponseGrpc response = userServiceStub.sendOtp(request).get();
                return userMapper.toDto(response);
            } catch (Exception e) {
                throw new RuntimeException("Error sending OTP: " + e.getMessage(), e);
            }
        });
    }

    @Override
    public CompletableFuture<BasePhoneOtpDto> verifyOtp(String phone, String otp) {
        VerifyOtpRequestGrpc request = userMapper.toVerifyOtpProto(phone, otp);
        return CompletableFuture.supplyAsync(() -> {
            try {
                VerifyOtpResponseGrpc response = userServiceStub.verifyOtp(request).get();
                return userMapper.toDto(response);
            } catch (Exception e) {
                throw new RuntimeException("Error verifying OTP: " + e.getMessage(), e);
            }
        });
    }

    @Override
    public CompletableFuture<BasePhoneOtpDto> resetPasswordWithOtp(String phone, String otp, String newPassword) {
        ResetPasswordWithOtpRequestGrpc request = userMapper.toResetPasswordProto(phone, otp, newPassword);
        return CompletableFuture.supplyAsync(() -> {
            try {
                ResetPasswordWithOtpResponseGrpc response = userServiceStub.resetPasswordWithOtp(request).get();
                BasePhoneOtpDto dto = new BasePhoneOtpDto();
                dto.setMessage(response.getMessage());
                return dto;
            } catch (Exception e) {
                throw new RuntimeException("Error resetting password with OTP: " + e.getMessage(), e);
            }
        });
    }

    @Override
    public CompletableFuture<BaseUserResponseDto> updateProfile(String id, String firstName, String lastName, 
                                                           String phone, String address, String identityNumber) {
        UpdateProfileRequestGrpc request = userMapper.toUpdateProfileProto(id, firstName, lastName, phone, address, identityNumber);
        return CompletableFuture.supplyAsync(() -> {
            try {
                UserResponseGrpc response = userServiceStub.updateProfile(request).get();
                return userMapper.toDto(response);
            } catch (Exception e) {
                throw new RuntimeException("Error updating profile: " + e.getMessage(), e);
            }
        });
    }

    // Authentication methods implementation
    @Override
    public CompletableFuture<BaseLoginResponseDto> login(BaseLoginRequestDto request) {
        LoginRequestGrpc grpcRequest = userMapper.toLoginProto(request);
        return CompletableFuture.supplyAsync(() -> {
            try {
                LoginResponseGrpc response = userServiceStub.login(grpcRequest).get();
                return userMapper.toLoginDto(response);
            } catch (Exception e) {
                throw new RuntimeException("Error during login: " + e.getMessage(), e);
            }
        });
    }

    @Override
    public CompletableFuture<BaseRefreshTokenResponseDto> refreshToken(BaseRefreshTokenRequestDto request) {
        RefreshTokenRequestGrpc grpcRequest = userMapper.toRefreshTokenProto(request);
        return CompletableFuture.supplyAsync(() -> {
            try {
                RefreshTokenResponseGrpc response = userServiceStub.refreshToken(grpcRequest).get();
                return userMapper.toRefreshTokenDto(response);
            } catch (Exception e) {
                throw new RuntimeException("Error refreshing token: " + e.getMessage(), e);
            }
        });
    }

    @Override
    public CompletableFuture<BaseLogoutResponseDto> logout(BaseLogoutRequestDto request) {
        LogoutRequestGrpc grpcRequest = userMapper.toLogoutProto(request);
        return CompletableFuture.supplyAsync(() -> {
            try {
                LogoutResponseGrpc response = userServiceStub.logout(grpcRequest).get();
                return userMapper.toLogoutDto(response);
            } catch (Exception e) {
                throw new RuntimeException("Error during logout: " + e.getMessage(), e);
            }
        });
    }
}
