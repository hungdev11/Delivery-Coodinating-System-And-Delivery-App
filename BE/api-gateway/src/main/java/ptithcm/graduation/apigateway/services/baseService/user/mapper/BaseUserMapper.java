package ptithcm.graduation.apigateway.services.baseService.user.mapper;

import ptithcm.graduation.apigateway.services.baseService.user.dto.*;
import knp.ptithcm.datn.user_module.modules.base.grpc.*;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class BaseUserMapper {

    // Proto to DTO mappings
    public BaseUserDto toDto(UserGrpc proto) {
        if (proto == null) return null;
        
        return new BaseUserDto(
            proto.getId(),
            proto.getKeycloakId(),
            proto.getUsername(),
            proto.getEmail(),
            proto.getFirstName(),
            proto.getLastName(),
            proto.getPhone(),
            proto.getAddress(),
            proto.getIdentityNumber(),
            proto.getRolesList(),
            proto.getStatus(),
            LocalDateTime.parse(proto.getCreatedAt()),
            LocalDateTime.parse(proto.getUpdatedAt())
        );
    }

    public BaseUserResponseDto toDto(UserResponseGrpc proto) {
        if (proto == null) return null;
        
        return new BaseUserResponseDto(
            proto.getMessage(),
            toDto(proto.getUser())
        );
    }

    public BaseListUsersResponseDto toDto(ListUsersResponseGrpc proto) {
        if (proto == null) return null;
        
        List<BaseUserDto> users = proto.getUsersList().stream()
            .map(this::toDto)
            .collect(Collectors.toList());
            
        return new BaseListUsersResponseDto(
            proto.getMessage(),
            users,
            proto.getTotal()
        );
    }

    public BasePhoneOtpDto toDto(PhoneExistsResponseGrpc proto) {
        if (proto == null) return null;
        
        BasePhoneOtpDto dto = new BasePhoneOtpDto();
        dto.setMessage(proto.getMessage());
        dto.setExists(proto.getExists());
        return dto;
    }

    public BasePhoneOtpDto toDto(SendOtpResponseGrpc proto) {
        if (proto == null) return null;
        
        BasePhoneOtpDto dto = new BasePhoneOtpDto();
        dto.setMessage(proto.getMessage());
        dto.setOtp(proto.getCode());
        return dto;
    }

    public BasePhoneOtpDto toDto(VerifyOtpResponseGrpc proto) {
        if (proto == null) return null;
        
        BasePhoneOtpDto dto = new BasePhoneOtpDto();
        dto.setMessage(proto.getMessage());
        dto.setValid(proto.getValid());
        return dto;
    }

    // DTO to Proto mappings
    public CreateUserRequestGrpc toProto(BaseCreateUserRequestDto dto) {
        if (dto == null) return null;
        
        CreateUserRequestGrpc.Builder builder = CreateUserRequestGrpc.newBuilder()
            .setUsername(dto.getUsername() != null ? dto.getUsername() : "")
            .setEmail(dto.getEmail() != null ? dto.getEmail() : "")
            .setFirstName(dto.getFirstName() != null ? dto.getFirstName() : "")
            .setLastName(dto.getLastName() != null ? dto.getLastName() : "")
            .setPhone(dto.getPhone() != null ? dto.getPhone() : "")
            .setAddress(dto.getAddress() != null ? dto.getAddress() : "")
            .setIdentityNumber(dto.getIdentityNumber() != null ? dto.getIdentityNumber() : "")
            .setPassword(dto.getPassword() != null ? dto.getPassword() : "");
            
        if (dto.getRoles() != null) {
            builder.addAllRoles(dto.getRoles());
        }
        
        return builder.build();
    }

    public UpdateUserRequestGrpc toProto(BaseUpdateUserRequestDto dto) {
        if (dto == null) return null;
        
        UpdateUserRequestGrpc.Builder builder = UpdateUserRequestGrpc.newBuilder()
            .setId(dto.getId() != null ? dto.getId() : "")
            .setUsername(dto.getUsername() != null ? dto.getUsername() : "")
            .setEmail(dto.getEmail() != null ? dto.getEmail() : "")
            .setFirstName(dto.getFirstName() != null ? dto.getFirstName() : "")
            .setLastName(dto.getLastName() != null ? dto.getLastName() : "")
            .setPhone(dto.getPhone() != null ? dto.getPhone() : "")
            .setAddress(dto.getAddress() != null ? dto.getAddress() : "")
            .setIdentityNumber(dto.getIdentityNumber() != null ? dto.getIdentityNumber() : "");
            
        if (dto.getRoles() != null) {
            builder.addAllRoles(dto.getRoles());
        }
        
        return builder.build();
    }

    public ListUsersRequestGrpc toProto(BaseListUsersRequestDto dto) {
        if (dto == null) return null;
        
        return ListUsersRequestGrpc.newBuilder()
            .setPage(dto.getPage() != null ? dto.getPage() : 0)
            .setSize(dto.getSize() != null ? dto.getSize() : 10)
            .build();
    }

    public GetUserByIdRequestGrpc toGetByIdProto(String id) {
        return GetUserByIdRequestGrpc.newBuilder()
            .setId(id != null ? id : "")
            .build();
    }

    public GetUserByUsernameRequestGrpc toGetByUsernameProto(String username) {
        return GetUserByUsernameRequestGrpc.newBuilder()
            .setUsername(username != null ? username : "")
            .build();
    }

    public GetUserByEmailRequestGrpc toGetByEmailProto(String email) {
        return GetUserByEmailRequestGrpc.newBuilder()
            .setEmail(email != null ? email : "")
            .build();
    }

    public UpdateUserStatusRequestGrpc toUpdateStatusProto(String id, Integer status) {
        return UpdateUserStatusRequestGrpc.newBuilder()
            .setId(id != null ? id : "")
            .setStatus(status != null ? status : 0)
            .build();
    }

    public UpdateUserPasswordRequestGrpc toUpdatePasswordProto(String id, String newPassword) {
        return UpdateUserPasswordRequestGrpc.newBuilder()
            .setId(id != null ? id : "")
            .setNewPassword(newPassword != null ? newPassword : "")
            .build();
    }

    public DeleteUserRequestGrpc toDeleteProto(String id) {
        return DeleteUserRequestGrpc.newBuilder()
            .setId(id != null ? id : "")
            .build();
    }

    public RegisterByPhoneRequestGrpc toRegisterByPhoneProto(String phone, String password, String firstName, String lastName) {
        return RegisterByPhoneRequestGrpc.newBuilder()
            .setPhone(phone != null ? phone : "")
            .setPassword(password != null ? password : "")
            .setFirstName(firstName != null ? firstName : "")
            .setLastName(lastName != null ? lastName : "")
            .build();
    }

    public PhoneExistsRequestGrpc toPhoneExistsProto(String phone) {
        return PhoneExistsRequestGrpc.newBuilder()
            .setPhone(phone != null ? phone : "")
            .build();
    }

    public SendOtpRequestGrpc toSendOtpProto(String phone) {
        return SendOtpRequestGrpc.newBuilder()
            .setPhone(phone != null ? phone : "")
            .build();
    }

    public VerifyOtpRequestGrpc toVerifyOtpProto(String phone, String otp) {
        return VerifyOtpRequestGrpc.newBuilder()
            .setPhone(phone != null ? phone : "")
            .setOtp(otp != null ? otp : "")
            .build();
    }

    public ResetPasswordWithOtpRequestGrpc toResetPasswordProto(String phone, String otp, String newPassword) {
        return ResetPasswordWithOtpRequestGrpc.newBuilder()
            .setPhone(phone != null ? phone : "")
            .setOtp(otp != null ? otp : "")
            .setNewPassword(newPassword != null ? newPassword : "")
            .build();
    }

    public UpdateProfileRequestGrpc toUpdateProfileProto(String id, String firstName, String lastName, 
                                                         String phone, String address, String identityNumber) {
        return UpdateProfileRequestGrpc.newBuilder()
            .setId(id != null ? id : "")
            .setFirstName(firstName != null ? firstName : "")
            .setLastName(lastName != null ? lastName : "")
            .setPhone(phone != null ? phone : "")
            .setAddress(address != null ? address : "")
            .setIdentityNumber(identityNumber != null ? identityNumber : "")
            .build();
    }

    public UpdateUserRoleRequestGrpc toUpdateRoleProto(String id, Integer role) {
        return UpdateUserRoleRequestGrpc.newBuilder()
            .setId(id != null ? id : "")
            .setRole(role != null ? role : 0)
            .build();
    }

    // Authentication mappings
    public LoginRequestGrpc toLoginProto(BaseLoginRequestDto dto) {
        if (dto == null) return null;
        
        return LoginRequestGrpc.newBuilder()
            .setUsername(dto.getUsername() != null ? dto.getUsername() : "")
            .setPassword(dto.getPassword() != null ? dto.getPassword() : "")
            .build();
    }

    public BaseLoginResponseDto toLoginDto(LoginResponseGrpc proto) {
        if (proto == null) return null;
        
        return new BaseLoginResponseDto(
            proto.getMessage(),
            proto.getAccessToken(),
            proto.getRefreshToken(),
            proto.getTokenType(),
            proto.getExpiresIn(),
            toDto(proto.getUser())
        );
    }

    public RefreshTokenRequestGrpc toRefreshTokenProto(BaseRefreshTokenRequestDto dto) {
        if (dto == null) return null;
        
        return RefreshTokenRequestGrpc.newBuilder()
            .setRefreshToken(dto.getRefreshToken() != null ? dto.getRefreshToken() : "")
            .build();
    }

    public BaseRefreshTokenResponseDto toRefreshTokenDto(RefreshTokenResponseGrpc proto) {
        if (proto == null) return null;
        
        return new BaseRefreshTokenResponseDto(
            proto.getMessage(),
            proto.getAccessToken(),
            proto.getRefreshToken(),
            proto.getTokenType(),
            proto.getExpiresIn()
        );
    }

    public LogoutRequestGrpc toLogoutProto(BaseLogoutRequestDto dto) {
        if (dto == null) return null;
        
        return LogoutRequestGrpc.newBuilder()
            .setRefreshToken(dto.getRefreshToken() != null ? dto.getRefreshToken() : "")
            .build();
    }

    public BaseLogoutResponseDto toLogoutDto(LogoutResponseGrpc proto) {
        if (proto == null) return null;
        
        return new BaseLogoutResponseDto(proto.getMessage());
    }
}
