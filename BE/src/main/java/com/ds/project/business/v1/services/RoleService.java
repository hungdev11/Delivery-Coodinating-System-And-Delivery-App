package com.ds.project.business.v1.services;

import com.ds.project.app_context.models.Role;
import com.ds.project.app_context.repositories.RoleRepository;
import com.ds.project.common.entities.dto.request.RoleRequest;
import com.ds.project.common.entities.dto.response.RoleResponse;
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
    public RoleResponse createRole(RoleRequest roleRequest) {
        log.info("Creating role: {}", roleRequest.getName());
        
        // Check if role already exists
        if (roleRepository.existsByName(roleRequest.getName())) {
            throw new IllegalArgumentException("Role with name " + roleRequest.getName() + " already exists");
        }
        
        Role role = roleMapper.mapToEntity(roleRequest);
        
        Role savedRole = roleRepository.save(role);
        log.info("Successfully created role: {}", savedRole.getName());
        
        return roleMapper.mapToResponse(savedRole);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Optional<RoleResponse> getRoleById(String id) {
        log.info("Getting role by ID: {}", id);
        return roleRepository.findById(id)
            .filter(role -> !role.getDeleted())
            .map(roleMapper::mapToResponse);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Optional<RoleResponse> getRoleByName(String name) {
        log.info("Getting role by name: {}", name);
        return roleRepository.findByName(name)
            .filter(role -> !role.getDeleted())
            .map(roleMapper::mapToResponse);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<RoleResponse> getAllRoles() {
        log.info("Getting all roles");
        return roleRepository.findAll().stream()
            .filter(role -> !role.getDeleted())
            .map(roleMapper::mapToResponse)
            .collect(Collectors.toList());
    }
    
    @Override
    public RoleResponse updateRole(String id, RoleRequest roleRequest) {
        log.info("Updating role: {}", id);
        
        Role existingRole = roleRepository.findById(id)
            .filter(role -> !role.getDeleted())
            .orElseThrow(() -> new IllegalArgumentException("Role not found with id: " + id));
        
        // Check if new name conflicts with existing role
        if (!existingRole.getName().equals(roleRequest.getName()) && 
            roleRepository.existsByName(roleRequest.getName())) {
            throw new IllegalArgumentException("Role with name " + roleRequest.getName() + " already exists");
        }
        
        existingRole.setName(roleRequest.getName());
        existingRole.setDescription(roleRequest.getDescription());
        existingRole.setUpdatedAt(LocalDateTime.now());
        
        Role updatedRole = roleRepository.save(existingRole);
        log.info("Successfully updated role: {}", updatedRole.getName());
        
        return roleMapper.mapToResponse(updatedRole);
    }
    
    @Override
    public void deleteRole(String id) {
        log.info("Soft deleting role: {}", id);
        
        Role role = roleRepository.findById(id)
            .filter(r -> !r.getDeleted())
            .orElseThrow(() -> new IllegalArgumentException("Role not found with id: " + id));
        
        role.setDeleted(true);
        role.setUpdatedAt(LocalDateTime.now());
        roleRepository.save(role);
        
        log.info("Successfully soft deleted role: {}", role.getName());
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
