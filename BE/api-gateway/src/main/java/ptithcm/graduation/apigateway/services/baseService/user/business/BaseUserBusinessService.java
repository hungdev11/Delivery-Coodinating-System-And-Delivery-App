package ptithcm.graduation.apigateway.services.baseService.user.business;

import ptithcm.graduation.apigateway.services.baseService.user.dto.*;
import ptithcm.graduation.apigateway.services.baseService.user.interfaces.IBaseUserBusinessService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import java.util.concurrent.CompletableFuture;

@Service
public class BaseUserBusinessService implements IBaseUserBusinessService {

    @Autowired
    private WebClient userServiceWebClient;

    

    @Override
    public CompletableFuture<BaseUserResponseDto> getUserById(String id) {
        return userServiceWebClient.get()
                .uri(uriBuilder -> uriBuilder.path("/user/{id}").build(id))
                .retrieve()
                .bodyToMono(BaseUserResponseDto.class)
                .toFuture();
    }

    @Override
    public CompletableFuture<BaseUserResponseDto> getUserByUsername(String username) {
        return userServiceWebClient.get()
                .uri(uriBuilder -> uriBuilder.path("/user").queryParam("username", username).build())
                .retrieve()
                .bodyToMono(BaseUserResponseDto.class)
                .toFuture();
    }

    @Override
    public CompletableFuture<BaseUserResponseDto> getUserByEmail(String email) {
        return userServiceWebClient.get()
                .uri(uriBuilder -> uriBuilder.path("/user").queryParam("email", email).build())
                .retrieve()
                .bodyToMono(BaseUserResponseDto.class)
                .toFuture();
    }

    @Override
    public CompletableFuture<BaseListUsersResponseDto> listUsers(BaseListUsersRequestDto request) {
        return userServiceWebClient.get()
                .uri(uriBuilder -> uriBuilder.path("/user")
                        .queryParam("page", request.getPage())
                        .queryParam("size", request.getSize())
                        .build())
                .retrieve()
                .bodyToMono(BaseListUsersResponseDto.class)
                .toFuture();
    }

    @Override
    public CompletableFuture<BaseUserResponseDto> createUser(BaseCreateUserRequestDto request) {
        return userServiceWebClient.post()
                .uri("/user/register")
                .body(Mono.just(request), BaseCreateUserRequestDto.class)
                .retrieve()
                .bodyToMono(BaseUserResponseDto.class)
                .toFuture();
    }

    @Override
    public CompletableFuture<BaseUserResponseDto> updateUser(BaseUpdateUserRequestDto request) {
        return userServiceWebClient.put()
                .uri("/user/profile")
                .body(Mono.just(request), BaseUpdateUserRequestDto.class)
                .retrieve()
                .bodyToMono(BaseUserResponseDto.class)
                .toFuture();
    }

    @Override
    public CompletableFuture<BaseUserResponseDto> updateUserPassword(String id, String newPassword) {
        return userServiceWebClient.put()
                .uri(uriBuilder -> uriBuilder.path("/user/{id}/password").build(id))
                .bodyValue(newPassword)
                .retrieve()
                .bodyToMono(BaseUserResponseDto.class)
                .toFuture();
    }

    @Override
    public CompletableFuture<String> deleteUser(String id) {
        return userServiceWebClient.delete()
                .uri(uriBuilder -> uriBuilder.path("/user/{id}").build(id))
                .retrieve()
                .bodyToMono(String.class)
                .toFuture();
    }

    @Override
    public CompletableFuture<BaseUserResponseDto> updateUserStatus(String id, Integer status) {
        return userServiceWebClient.put()
                .uri(uriBuilder -> uriBuilder.path("/user/{id}/status").build(id))
                .bodyValue(status)
                .retrieve()
                .bodyToMono(BaseUserResponseDto.class)
                .toFuture();
    }

    @Override
    public CompletableFuture<BaseUserResponseDto> updateUserRole(String id, Integer role) {
        return userServiceWebClient.put()
                .uri(uriBuilder -> uriBuilder.path("/user/{id}/role").build(id))
                .bodyValue(role)
                .retrieve()
                .bodyToMono(BaseUserResponseDto.class)
                .toFuture();
    }

    @Override
    public CompletableFuture<BaseUserResponseDto> registerByPhone(String phone, String password, String firstName, String lastName) {
        BaseCreateUserRequestDto req = new BaseCreateUserRequestDto();
        req.setPhone(phone);
        req.setPassword(password);
        req.setFirstName(firstName);
        req.setLastName(lastName);
        return userServiceWebClient.post()
                .uri("/user/register/phone")
                .body(Mono.just(req), BaseCreateUserRequestDto.class)
                .retrieve()
                .bodyToMono(BaseUserResponseDto.class)
                .toFuture();
    }

    @Override
    public CompletableFuture<BasePhoneOtpDto> phoneExists(String phone) {
        return userServiceWebClient.get()
                .uri(uriBuilder -> uriBuilder.path("/user/phone-exists").queryParam("phone", phone).build())
                .retrieve()
                .bodyToMono(BasePhoneOtpDto.class)
                .toFuture();
    }

    @Override
    public CompletableFuture<BasePhoneOtpDto> sendOtp(String phone) {
        return userServiceWebClient.post()
                .uri("/user/otp/send")
                .bodyValue(phone)
                .retrieve()
                .bodyToMono(BasePhoneOtpDto.class)
                .toFuture();
    }

    @Override
    public CompletableFuture<BasePhoneOtpDto> verifyOtp(String phone, String otp) {
        return userServiceWebClient.post()
                .uri("/user/otp/verify")
                .bodyValue(new BasePhoneOtpDto(phone, otp))
                .retrieve()
                .bodyToMono(BasePhoneOtpDto.class)
                .toFuture();
    }

    @Override
    public CompletableFuture<BasePhoneOtpDto> resetPasswordWithOtp(String phone, String otp, String newPassword) {
        BasePhoneOtpDto req = new BasePhoneOtpDto();
        req.setOtp(otp);
        req.setPhone(phone);
        return userServiceWebClient.post()
                .uri("/user/password/reset-otp")
                .bodyValue(req)
                .retrieve()
                .bodyToMono(BasePhoneOtpDto.class)
                .toFuture();
    }

    @Override
    public CompletableFuture<BaseUserResponseDto> updateProfile(String id, String firstName, String lastName, 
                                                           String phone, String address, String identityNumber) {
        BaseUpdateUserRequestDto req = new BaseUpdateUserRequestDto();
        req.setId(id);
        req.setFirstName(firstName);
        req.setLastName(lastName);
        req.setPhone(phone);
        req.setAddress(address);
        req.setIdentityNumber(identityNumber);
        return userServiceWebClient.put()
                .uri("/user/profile")
                .bodyValue(req)
                .retrieve()
                .bodyToMono(BaseUserResponseDto.class)
                .toFuture();
    }

    // Authentication methods implementation
    @Override
    public CompletableFuture<BaseLoginResponseDto> login(BaseLoginRequestDto request) {
        return userServiceWebClient.post()
                .uri("/api/auth/login")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(BaseLoginResponseDto.class)
                .toFuture();
    }

    @Override
    public CompletableFuture<BaseRefreshTokenResponseDto> refreshToken(BaseRefreshTokenRequestDto request) {
        return userServiceWebClient.post()
                .uri("/api/auth/refresh")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(BaseRefreshTokenResponseDto.class)
                .toFuture();
    }

    @Override
    public CompletableFuture<BaseLogoutResponseDto> logout(BaseLogoutRequestDto request) {
        return userServiceWebClient.post()
                .uri("/api/auth/logout")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(BaseLogoutResponseDto.class)
                .toFuture();
    }
}
