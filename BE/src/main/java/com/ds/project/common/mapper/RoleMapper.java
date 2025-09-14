package com.ds.project.common.mapper;

import com.ds.project.app_context.models.Role;
import com.ds.project.common.entities.dto.request.RoleRequest;
import com.ds.project.common.entities.dto.RoleDto;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Mapper for Role entity and DTOs
 */
@Component
public class RoleMapper {
    
    /**
     * Maps RoleRequest to Role entity
     */
    public Role mapToEntity(RoleRequest roleRequest) {
        return Role.builder()
            .name(roleRequest.getName())
            .description(roleRequest.getDescription())
            .deleted(false)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();
    }

    /**
     * Maps Role entity to RoleDto
     */
    public RoleDto mapToDto(Role role) {
        return RoleDto.builder()
            .id(role.getId())
            .name(role.getName())
            .description(role.getDescription())
            .createdAt(role.getCreatedAt().toString())
            .updatedAt(role.getUpdatedAt().toString())
            .build();
    }

}
