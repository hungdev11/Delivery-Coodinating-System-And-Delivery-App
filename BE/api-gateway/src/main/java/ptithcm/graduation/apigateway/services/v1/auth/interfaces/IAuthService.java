package ptithcm.graduation.apigateway.services.v1.auth.interfaces;

import java.util.concurrent.CompletableFuture;
import ptithcm.graduation.apigateway.models.BaseResponse;
import ptithcm.graduation.apigateway.services.v1.auth.dto.*;

public interface IAuthService {
    
    // Authentication & Authorization
    CompletableFuture<LoginResponseDto> login(LoginRequestDto request);
    CompletableFuture<BaseResponse<String>> logout(RefreshTokenRequestDto request);
    CompletableFuture<LoginResponseDto> refreshToken(RefreshTokenRequestDto request);
    
    // Phone Registration & OTP Operations
    CompletableFuture<BaseResponse<AuthDto>> registerByPhone(RegisterByPhoneRequestDto request);
    CompletableFuture<BaseResponse<String>> sendOtp(OtpRequestDto request);
    CompletableFuture<BaseResponse<Boolean>> verifyOtp(VerifyOtpRequestDto request);
    CompletableFuture<BaseResponse<String>> resetPasswordWithOtp(ResetPasswordWithOtpRequestDto request);
    CompletableFuture<BaseResponse<Boolean>> checkPhoneExists(String phone);
}
