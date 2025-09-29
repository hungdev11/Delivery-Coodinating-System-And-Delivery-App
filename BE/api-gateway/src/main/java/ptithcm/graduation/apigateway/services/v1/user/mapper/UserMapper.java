package ptithcm.graduation.apigateway.services.v1.user.mapper;

import knp.ptithcm.datn.user_module.modules.base.grpc.UserGrpc;
import ptithcm.graduation.apigateway.services.baseService.user.dto.BaseUserDto;
import ptithcm.graduation.apigateway.services.v1.user.dto.UserDto;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class UserMapper {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public UserDto toDto(UserGrpc proto) {
        if (proto == null) {
            return null;
        }

        UserDto dto = new UserDto();
        dto.setId(proto.getId());
        dto.setKeycloakId(proto.getKeycloakId());
        dto.setUsername(proto.getUsername());
        dto.setEmail(proto.getEmail());
        dto.setFirstName(proto.getFirstName());
        dto.setLastName(proto.getLastName());
        dto.setPhone(proto.getPhone());
        dto.setAddress(proto.getAddress());
        dto.setIdentityNumber(proto.getIdentityNumber());
        dto.setRoles(proto.getRolesList());
        dto.setStatus(proto.getStatus());
        dto.setCreatedAt(parseDateTime(proto.getCreatedAt()));
        dto.setUpdatedAt(parseDateTime(proto.getUpdatedAt()));
        return dto;
    }

    public UserDto toDto(BaseUserDto userDto) {
        if (userDto == null) {
            return null;
        }

        UserDto dto = new UserDto();
        dto.setId(userDto.getId());
        dto.setKeycloakId(userDto.getKeycloakId());
        dto.setUsername(userDto.getUsername());
        dto.setEmail(userDto.getEmail());
        dto.setFirstName(userDto.getFirstName());
        dto.setLastName(userDto.getLastName());
        dto.setPhone(userDto.getPhone());
        dto.setAddress(userDto.getAddress());
        dto.setIdentityNumber(userDto.getIdentityNumber());
        dto.setRoles(userDto.getRoles());
        dto.setStatus(userDto.getStatus());
        dto.setCreatedAt(userDto.getCreatedAt());
        dto.setUpdatedAt(userDto.getUpdatedAt());
        return dto;
    }

    public BaseUserDto toBaseDto(UserDto dto) {
        if (dto == null) {
            return null;
        }

        return new BaseUserDto(
                dto.getId() != null ? dto.getId() : "",
                dto.getKeycloakId() != null ? dto.getKeycloakId() : "",
                dto.getUsername() != null ? dto.getUsername() : "",
                dto.getEmail() != null ? dto.getEmail() : "",
                dto.getFirstName() != null ? dto.getFirstName() : "",
                dto.getLastName() != null ? dto.getLastName() : "",
                dto.getPhone() != null ? dto.getPhone() : "",
                dto.getAddress() != null ? dto.getAddress() : "",
                dto.getIdentityNumber() != null ? dto.getIdentityNumber() : "",
                dto.getRoles() != null ? dto.getRoles() : List.of(),
                dto.getStatus() != null ? dto.getStatus() : 0,
                dto.getCreatedAt() != null ? dto.getCreatedAt() : null,
                dto.getUpdatedAt() != null ? dto.getUpdatedAt() : null);
    }

    private LocalDateTime parseDateTime(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.isEmpty()) {
            return null;
        }
        try {
            return LocalDateTime.parse(dateTimeStr, FORMATTER);
        } catch (Exception e) {
            return null;
        }
    }
}
