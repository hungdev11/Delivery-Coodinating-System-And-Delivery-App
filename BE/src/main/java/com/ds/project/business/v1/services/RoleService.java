package com.ds.project.business.v1.services;

import com.ds.project.app_context.models.Role;
import com.ds.project.app_context.repositories.RoleRepository;
import com.ds.project.common.entities.base.BaseResponse;
import com.ds.project.common.entities.base.Page;
import com.ds.project.common.entities.base.PagedData;
import com.ds.project.common.entities.dto.RoleDto;
import com.ds.project.common.entities.dto.request.RoleRequest;
import com.ds.project.common.interfaces.IRoleService;
import com.ds.project.common.mapper.RoleMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Role service implementation
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class RoleService implements IRoleService {
    
    private final RoleRepository roleRepository;
    private final RoleMapper roleMapper;
    
    @Override
    public BaseResponse<RoleDto> createRole(RoleRequest roleRequest) {
        try {
            log.info("Creating role: {}", roleRequest.getName());
            
            // Check if role already exists
            if (roleRepository.existsByName(roleRequest.getName())) {
                return BaseResponse.<RoleDto>builder()
                    .message(Optional.of("Role with name " + roleRequest.getName() + " already exists"))
                    .build();
            }
            
            Role role = roleMapper.mapToEntity(roleRequest);
            
            Role savedRole = roleRepository.save(role);
            log.info("Successfully created role: {}", savedRole.getName());
            
            RoleDto roleDto = roleMapper.mapToDto(savedRole);
            return BaseResponse.<RoleDto>builder()
                .result(Optional.of(roleDto))
                .build();
        } catch (Exception e) {
            log.error("Error creating role: {}", e.getMessage(), e);
            return BaseResponse.<RoleDto>builder()
                .message(Optional.of("Failed to create role: " + e.getMessage()))
                .build();
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public Optional<BaseResponse<RoleDto>> getRoleById(String id) {
        try {
            log.info("Getting role by ID: {}", id);
            return roleRepository.findById(id)
                .filter(role -> !role.getDeleted())
                .map(role -> {
                    RoleDto roleDto = roleMapper.mapToDto(role);
                    return BaseResponse.<RoleDto>builder()
                        .result(Optional.of(roleDto))
                        .build();
                });
        } catch (Exception e) {
            log.error("Error getting role by id {}: {}", id, e.getMessage(), e);
            return Optional.of(BaseResponse.<RoleDto>builder()
                .message(Optional.of("Failed to get role: " + e.getMessage()))
                .build());
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public Optional<BaseResponse<RoleDto>> getRoleByName(String name) {
        try {
            log.info("Getting role by name: {}", name);
            return roleRepository.findByName(name)
                .filter(role -> !role.getDeleted())
                .map(role -> {
                    RoleDto roleDto = roleMapper.mapToDto(role);
                    return BaseResponse.<RoleDto>builder()
                        .result(Optional.of(roleDto))
                        .build();
                });
        } catch (Exception e) {
            log.error("Error getting role by name {}: {}", name, e.getMessage(), e);
            return Optional.of(BaseResponse.<RoleDto>builder()
                .message(Optional.of("Failed to get role: " + e.getMessage()))
                .build());
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public BaseResponse<PagedData<Page, RoleDto>> getAllRoles() {
        try {
            log.info("Getting all roles");
            List<Role> roles = roleRepository.findAll().stream()
                .filter(role -> !role.getDeleted())
                .collect(Collectors.toList());
            
            List<RoleDto> roleDtos = roles.stream()
                .map(roleMapper::mapToDto)
                .collect(Collectors.toList());
            
            Page page = Page.builder()
                .page(0)
                .size(roleDtos.size())
                .totalElements((long) roleDtos.size())
                .totalPages(1)
                .build();
            
            PagedData<Page, RoleDto> pagedData = PagedData.<Page, RoleDto>builder()
                .data(roleDtos)
                .page(page)
                .build();
            
            return BaseResponse.<PagedData<Page, RoleDto>>builder()
                .result(Optional.of(pagedData))
                .build();
        } catch (Exception e) {
            log.error("Error getting all roles: {}", e.getMessage(), e);
            return BaseResponse.<PagedData<Page, RoleDto>>builder()
                .message(Optional.of("Failed to get roles: " + e.getMessage()))
                .build();
        }
    }
    
    @Override
    public BaseResponse<RoleDto> updateRole(String id, RoleRequest roleRequest) {
        try {
            log.info("Updating role: {}", id);
            
            Role existingRole = roleRepository.findById(id)
                .filter(role -> !role.getDeleted())
                .orElse(null);
            
            if (existingRole == null) {
                return BaseResponse.<RoleDto>builder()
                    .message(Optional.of("Role not found with id: " + id))
                    .build();
            }
            
            // Check if new name conflicts with existing role
            if (!existingRole.getName().equals(roleRequest.getName()) && 
                roleRepository.existsByName(roleRequest.getName())) {
                return BaseResponse.<RoleDto>builder()
                    .message(Optional.of("Role with name " + roleRequest.getName() + " already exists"))
                    .build();
            }
            
            existingRole.setName(roleRequest.getName());
            existingRole.setDescription(roleRequest.getDescription());
            existingRole.setUpdatedAt(LocalDateTime.now());
            
            Role updatedRole = roleRepository.save(existingRole);
            log.info("Successfully updated role: {}", updatedRole.getName());
            
            RoleDto roleDto = roleMapper.mapToDto(updatedRole);
            return BaseResponse.<RoleDto>builder()
                .result(Optional.of(roleDto))
                .build();
        } catch (Exception e) {
            log.error("Error updating role {}: {}", id, e.getMessage(), e);
            return BaseResponse.<RoleDto>builder()
                .message(Optional.of("Failed to update role: " + e.getMessage()))
                .build();
        }
    }
    
    @Override
    public void deleteRole(String id) {
        try {
            log.info("Soft deleting role: {}", id);
            
            Role role = roleRepository.findById(id)
                .filter(r -> !r.getDeleted())
                .orElse(null);
            
            if (role == null) {
                log.warn("Role not found with id: {}", id);
                return;
            }
            
            role.setDeleted(true);
            role.setUpdatedAt(LocalDateTime.now());
            roleRepository.save(role);
            
            log.info("Successfully soft deleted role: {}", role.getName());
        } catch (Exception e) {
            log.error("Error deleting role {}: {}", id, e.getMessage(), e);
        }
    }
    
    @Override
    public void initializeDefaultRoles() {
        log.info("Initializing default roles");
        
        List<Role> defaultRoles = List.of(
            Role.builder()
                .name("ADMIN")
                .description("Administrator role with full system access")
                .deleted(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build(),
            Role.builder()
                .name("MANAGER")
                .description("Manager role with limited administrative access")
                .deleted(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build()
        );
        
        for (Role role : defaultRoles) {
            if (!roleRepository.existsByName(role.getName())) {
                roleRepository.save(role);
                log.info("Created default role: {}", role.getName());
            } else {
                log.info("Default role already exists: {}", role.getName());
            }
        }
        
        log.info("Default roles initialization completed");
    }
    
}
