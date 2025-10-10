package com.ds.project.common.interfaces;

import com.ds.project.common.entities.dto.request.RoleRequest;
import com.ds.project.common.entities.dto.response.RoleResponse;

import java.util.List;
import java.util.Optional;

/**
 * Role service interface
 */
public interface IRoleService {
    
    /**
     * Create a new role
     */
    RoleResponse createRole(RoleRequest roleRequest);
    
    /**
     * Get role by ID
     */
    Optional<RoleResponse> getRoleById(String id);
    
    /**
     * Get role by name
     */
    Optional<RoleResponse> getRoleByName(String name);
    
    /**
     * Get all roles
     */
    List<RoleResponse> getAllRoles();
    
    /**
     * Update role
     */
    RoleResponse updateRole(String id, RoleRequest roleRequest);
    
    /**
     * Delete role (soft delete)
     */
    void deleteRole(String id);
    
    /**
     * Initialize default roles (admin, manager)
     */
    void initializeDefaultRoles();
}
