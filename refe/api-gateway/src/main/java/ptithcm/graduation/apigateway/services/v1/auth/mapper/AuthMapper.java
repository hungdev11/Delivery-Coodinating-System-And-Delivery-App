package ptithcm.graduation.apigateway.services.v1.auth.mapper;

import knp.ptithcm.datn.user_module.modules.base.grpc.UserGrpc;
import ptithcm.graduation.apigateway.services.v1.auth.dto.AuthDto;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class AuthMapper {
    
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    public AuthDto toDto(UserGrpc proto) {
        if (proto == null) {
            return null;
        }
        
        return AuthDto.builder()
                .id(proto.getId())
                .keycloakId(proto.getKeycloakId())
                .username(proto.getUsername())
                .email(proto.getEmail())
                .firstName(proto.getFirstName())
                .lastName(proto.getLastName())
                .phone(proto.getPhone())
                .address(proto.getAddress())
                .identityNumber(proto.getIdentityNumber())
                .roles(proto.getRolesList())
                .status(proto.getStatus())
                .createdAt(parseDateTime(proto.getCreatedAt()))
                .updatedAt(parseDateTime(proto.getUpdatedAt()))
                .build();
    }
    
    public UserGrpc toProto(AuthDto dto) {
        if (dto == null) {
            return null;
        }
        
        return UserGrpc.newBuilder()
                .setId(dto.getId() != null ? dto.getId() : "")
                .setKeycloakId(dto.getKeycloakId() != null ? dto.getKeycloakId() : "")
                .setUsername(dto.getUsername() != null ? dto.getUsername() : "")
                .setEmail(dto.getEmail() != null ? dto.getEmail() : "")
                .setFirstName(dto.getFirstName() != null ? dto.getFirstName() : "")
                .setLastName(dto.getLastName() != null ? dto.getLastName() : "")
                .setPhone(dto.getPhone() != null ? dto.getPhone() : "")
                .setAddress(dto.getAddress() != null ? dto.getAddress() : "")
                .setIdentityNumber(dto.getIdentityNumber() != null ? dto.getIdentityNumber() : "")
                .addAllRoles(dto.getRoles() != null ? dto.getRoles() : List.of())
                .setStatus(dto.getStatus() != null ? dto.getStatus() : 0)
                .setCreatedAt(dto.getCreatedAt() != null ? dto.getCreatedAt().format(FORMATTER) : "")
                .setUpdatedAt(dto.getUpdatedAt() != null ? dto.getUpdatedAt().format(FORMATTER) : "")
                .build();
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
