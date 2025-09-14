package com.ds.project.common.interfaces;

import com.ds.project.common.entities.base.BaseResponse;
import com.ds.project.common.entities.base.Page;
import com.ds.project.common.entities.base.PagedData;
import com.ds.project.common.entities.dto.RoleDto;
import com.ds.project.common.entities.dto.request.RoleRequest;

import java.util.List;
import java.util.Optional;

/**
 * Role service interface
 */
public interface IRoleService {
    
    /**
     * Create a new role
     */
    BaseResponse<RoleDto> createRole(RoleRequest roleRequest);
    
    /**
     * Get role by ID
     */
    Optional<BaseResponse<RoleDto>> getRoleById(String id);
    
    /**
     * Get role by name
     */
    Optional<BaseResponse<RoleDto>> getRoleByName(String name);
    
    /**
     * Get all roles
     */
    BaseResponse<PagedData<Page, RoleDto>> getAllRoles();
    
    /**
     * Update role
     */
    BaseResponse<RoleDto> updateRole(String id, RoleRequest roleRequest);
    
    /**
     * Delete role (soft delete)
     */
    void deleteRole(String id);
    
    /**
     * Initialize default roles (admin, manager)
     */
    void initializeDefaultRoles();
}
