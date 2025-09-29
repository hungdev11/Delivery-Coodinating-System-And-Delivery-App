package ptithcm.graduation.apigateway.services.baseService.user.interfaces;

import ptithcm.graduation.apigateway.services.baseService.user.dto.*;
import java.util.concurrent.CompletableFuture;

public interface IBaseUserBusinessService {
    
    // User CRUD operations
    CompletableFuture<BaseUserResponseDto> getUserById(String id);
    CompletableFuture<BaseUserResponseDto> getUserByUsername(String username);
    CompletableFuture<BaseUserResponseDto> getUserByEmail(String email);
    CompletableFuture<BaseListUsersResponseDto> listUsers(BaseListUsersRequestDto request);
    CompletableFuture<BaseUserResponseDto> createUser(BaseCreateUserRequestDto request);
    CompletableFuture<BaseUserResponseDto> updateUser(BaseUpdateUserRequestDto request);
    CompletableFuture<BaseUserResponseDto> updateUserPassword(String id, String newPassword);
    CompletableFuture<String> deleteUser(String id);
    
    // User status and role management
    CompletableFuture<BaseUserResponseDto> updateUserStatus(String id, Integer status);
    CompletableFuture<BaseUserResponseDto> updateUserRole(String id, Integer role);
    
    // Profile management
    CompletableFuture<BaseUserResponseDto> updateProfile(String id, String firstName, String lastName, 
                                                     String phone, String address, String identityNumber);
    
    // Phone registration and OTP operations
    CompletableFuture<BaseUserResponseDto> registerByPhone(String phone, String password, String firstName, String lastName);
    CompletableFuture<BasePhoneOtpDto> phoneExists(String phone);
    CompletableFuture<BasePhoneOtpDto> sendOtp(String phone);
    CompletableFuture<BasePhoneOtpDto> verifyOtp(String phone, String otp);
    CompletableFuture<BasePhoneOtpDto> resetPasswordWithOtp(String phone, String otp, String newPassword);
    
    // Authentication methods
    CompletableFuture<BaseLoginResponseDto> login(BaseLoginRequestDto request);
    CompletableFuture<BaseRefreshTokenResponseDto> refreshToken(BaseRefreshTokenRequestDto request);
    CompletableFuture<BaseLogoutResponseDto> logout(BaseLogoutRequestDto request);
}
