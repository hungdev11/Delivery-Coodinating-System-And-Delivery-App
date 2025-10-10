package com.ds.project.common.mapper;

import com.ds.project.app_context.models.Role;
import com.ds.project.common.entities.dto.request.RoleRequest;
import com.ds.project.common.entities.dto.response.RoleResponse;
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
     * Maps Role entity to RoleResponse
     */
    public RoleResponse mapToResponse(Role role) {
        return RoleResponse.builder()
            .id(role.getId())
            .name(role.getName())
            .description(role.getDescription())
            .deleted(role.getDeleted())
            .createdAt(role.getCreatedAt())
            .updatedAt(role.getUpdatedAt())
            .build();
    }
}
