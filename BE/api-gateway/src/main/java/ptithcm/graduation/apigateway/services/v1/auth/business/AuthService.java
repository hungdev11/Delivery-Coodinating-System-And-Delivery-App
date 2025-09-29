package ptithcm.graduation.apigateway.services.v1.auth.business;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ptithcm.graduation.apigateway.models.BaseResponse;
import ptithcm.graduation.apigateway.services.baseService.user.business.BaseUserBusinessService;
import ptithcm.graduation.apigateway.services.baseService.user.dto.BaseLoginRequestDto;
import ptithcm.graduation.apigateway.services.baseService.user.dto.BaseLogoutRequestDto;
import ptithcm.graduation.apigateway.services.baseService.user.dto.BaseRefreshTokenRequestDto;
import ptithcm.graduation.apigateway.services.baseService.user.interfaces.IBaseUserBusinessService;
import ptithcm.graduation.apigateway.services.v1.auth.interfaces.IAuthService;
import ptithcm.graduation.apigateway.services.v1.auth.mapper.AuthMapper;
import ptithcm.graduation.apigateway.services.v1.auth.dto.*;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService implements IAuthService {
    
    private final AuthMapper authMapper;
    private final IBaseUserBusinessService userBusinessService;
    
    @Override
    public CompletableFuture<LoginResponseDto> login(LoginRequestDto request) {
        try {
            log.info("Processing login request for user: {}", request.getUsername());
            
            // Convert v1 DTO to baseService DTO    
                BaseLoginRequestDto baseRequest = 
                    new BaseLoginRequestDto(
                    request.getUsername(), 
                    request.getPassword()
                );
            
            // Call baseService login method
            return userBusinessService.login(baseRequest)
                .thenApply(baseResponse -> {
                    // Convert baseService response to v1 response
                    AuthDto userAuth = AuthDto.builder()
                            .id(baseResponse.getUser().getId())
                            .keycloakId(baseResponse.getUser().getKeycloakId())
                            .username(baseResponse.getUser().getUsername())
                            .email(baseResponse.getUser().getEmail())
                            .firstName(baseResponse.getUser().getFirstName())
                            .lastName(baseResponse.getUser().getLastName())
                            .phone(baseResponse.getUser().getPhone())
                            .address(baseResponse.getUser().getAddress())
                            .identityNumber(baseResponse.getUser().getIdentityNumber())
                            .roles(baseResponse.getUser().getRoles())
                            .status(baseResponse.getUser().getStatus())
                            .build();
                    
                    return LoginResponseDto.builder()
                            .message(baseResponse.getMessage())
                            .accessToken(baseResponse.getAccessToken())
                            .refreshToken(baseResponse.getRefreshToken())
                            .tokenType(baseResponse.getTokenType())
                            .expiresIn(baseResponse.getExpiresIn())
                            .user(userAuth)
                            .build();
                });
        } catch (Exception e) {
            log.error("Error during login for user: {}", request.getUsername(), e);
            return CompletableFuture.failedFuture(e);
        }
    }
    
    @Override
    public CompletableFuture<BaseResponse<String>> logout(RefreshTokenRequestDto request) {
        try {
            log.info("Processing logout request");
            
            // Convert v1 DTO to baseService DTO
            BaseLogoutRequestDto baseRequest = 
                new BaseLogoutRequestDto(
                    request.getRefreshToken()
                );
            
            // Call baseService logout method
            return userBusinessService.logout(baseRequest)
                .thenApply(baseResponse -> new BaseResponse<>(baseResponse.getMessage(), "Logout successful"));
                
        } catch (Exception e) {
            log.error("Error during logout", e);
            return CompletableFuture.failedFuture(e);
        }
    }
    
    @Override
    public CompletableFuture<LoginResponseDto> refreshToken(RefreshTokenRequestDto request) {
        try {
            log.info("Processing refresh token request");
            
            // Convert v1 DTO to baseService DTO
            BaseRefreshTokenRequestDto baseRequest = 
                new BaseRefreshTokenRequestDto(
                    request.getRefreshToken()
                );
            
            // Call baseService refreshToken method
            return userBusinessService.refreshToken(baseRequest)
                .thenApply(baseResponse -> {
                    // Convert baseService response to v1 response
                    AuthDto userAuth = AuthDto.builder()
                            .id(baseResponse.getAccessToken()) // Using accessToken as placeholder for user ID
                            .keycloakId("")
                            .username("")
                            .email("")
                            .firstName("")
                            .lastName("")
                            .phone("")
                            .address("")
                            .identityNumber("")
                            .roles(null)
                            .status(0)
                            .build();
                    
                    return LoginResponseDto.builder()
                            .message(baseResponse.getMessage())
                            .accessToken(baseResponse.getAccessToken())
                            .refreshToken(baseResponse.getRefreshToken())
                            .tokenType(baseResponse.getTokenType())
                            .expiresIn(baseResponse.getExpiresIn())
                            .user(userAuth)
                            .build();
                });
                
        } catch (Exception e) {
            log.error("Error during token refresh", e);
            return CompletableFuture.failedFuture(e);
        }
    }
    
    @Override
    public CompletableFuture<BaseResponse<AuthDto>> registerByPhone(RegisterByPhoneRequestDto request) {
        try {
            log.info("Processing phone registration for phone: {}", request.getPhone());
            
            // Call baseService registerByPhone method
            return userBusinessService.registerByPhone(
                request.getPhone(), 
                request.getPassword(), 
                request.getFirstName(), 
                request.getLastName()
            ).thenApply(baseResponse -> {
                if (baseResponse.getUser() != null) {
                    AuthDto userAuth = AuthDto.builder()
                            .id(baseResponse.getUser().getId())
                            .keycloakId(baseResponse.getUser().getKeycloakId())
                            .username(baseResponse.getUser().getUsername())
                            .email(baseResponse.getUser().getEmail())
                            .firstName(baseResponse.getUser().getFirstName())
                            .lastName(baseResponse.getUser().getLastName())
                            .phone(baseResponse.getUser().getPhone())
                            .address(baseResponse.getUser().getAddress())
                            .identityNumber(baseResponse.getUser().getIdentityNumber())
                            .roles(baseResponse.getUser().getRoles())
                            .status(baseResponse.getUser().getStatus())
                            .build();
                    
                    return new BaseResponse<>(baseResponse.getMessage(), userAuth);
                } else {
                    throw new RuntimeException("Failed to create user");
                }
            });
            
        } catch (Exception e) {
            log.error("Error during phone registration for phone: {}", request.getPhone(), e);
            return CompletableFuture.failedFuture(e);
        }
    }
    
    @Override
    public CompletableFuture<BaseResponse<String>> sendOtp(OtpRequestDto request) {
        try {
            log.info("Processing send OTP request for phone: {}", request.getPhone());
            
            // Call baseService sendOtp method
            return userBusinessService.sendOtp(request.getPhone())
                .thenApply(baseResponse -> new BaseResponse<>(baseResponse.getMessage(), "OTP sent successfully"));
                
        } catch (Exception e) {
            log.error("Error sending OTP to phone: {}", request.getPhone(), e);
            return CompletableFuture.failedFuture(e);
        }
    }
    
    @Override
    public CompletableFuture<BaseResponse<Boolean>> verifyOtp(VerifyOtpRequestDto request) {
        try {
            log.info("Processing OTP verification for phone: {}", request.getPhone());
            
            // Call baseService verifyOtp method
            return userBusinessService.verifyOtp(request.getPhone(), request.getOtp())
                .thenApply(baseResponse -> new BaseResponse<>(baseResponse.getMessage(), baseResponse.getValid()));
                
        } catch (Exception e) {
            log.error("Error verifying OTP for phone: {}", request.getPhone(), e);
            return CompletableFuture.failedFuture(e);
        }
    }
    
    @Override
    public CompletableFuture<BaseResponse<String>> resetPasswordWithOtp(ResetPasswordWithOtpRequestDto request) {
        try {
            log.info("Processing password reset with OTP for phone: {}", request.getPhone());
            
            // Call baseService resetPasswordWithOtp method
            return userBusinessService.resetPasswordWithOtp(request.getPhone(), request.getOtp(), request.getNewPassword())
                .thenApply(baseResponse -> new BaseResponse<>(baseResponse.getMessage(), "Password reset successfully"));
                
        } catch (Exception e) {
            log.error("Error resetting password for phone: {}", request.getPhone(), e);
            return CompletableFuture.failedFuture(e);
        }
    }
    
    @Override
    public CompletableFuture<BaseResponse<Boolean>> checkPhoneExists(String phone) {
        try {
            log.info("Checking if phone exists: {}", phone);
            
            // Call baseService phoneExists method
            return userBusinessService.phoneExists(phone)
                .thenApply(baseResponse -> new BaseResponse<>(baseResponse.getMessage(), baseResponse.getExists()));
                
        } catch (Exception e) {
            log.error("Error checking phone existence: {}", phone, e);
            return CompletableFuture.failedFuture(e);
        }
    }
}
