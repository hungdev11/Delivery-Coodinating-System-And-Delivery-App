package com.ds.project.application.controllers.v1;

import com.ds.project.common.entities.base.BaseResponse;
import com.ds.project.common.entities.base.PagedData;
import com.ds.project.common.entities.base.Page;
import com.ds.project.common.entities.dto.RoleDto;
import com.ds.project.common.entities.dto.request.RoleRequest;
import com.ds.project.common.interfaces.IRoleService;
import com.ds.project.common.utils.ResponseUtils;
import com.ds.project.application.annotations.AuthRequired;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.Map;

/**
 * REST Controller for Role operations
 */
@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
public class RoleController {
    
    private final IRoleService roleService;
    
    @PostMapping
    @AuthRequired
    public ResponseEntity<Map<String, Object>> createRole(@Valid @RequestBody RoleRequest roleRequest) {
        BaseResponse<RoleDto> response = roleService.createRole(roleRequest);
        
        if (response.getResult().isPresent()) {
            return ResponseUtils.success(response.getResult().get());
        } else {
            return ResponseUtils.error(response.getMessage().orElse("Failed to create role"), 
                org.springframework.http.HttpStatus.BAD_REQUEST);
        }
    }
    
    @GetMapping("/{id}")
    @AuthRequired
    public ResponseEntity<Map<String, Object>> getRoleById(@PathVariable String id) {
        return roleService.getRoleById(id)
            .map(response -> {
                if (response.getResult().isPresent()) {
                    return ResponseUtils.success(response.getResult().get());
                } else {
                    return ResponseUtils.error(response.getMessage().orElse("Role not found"), 
                        org.springframework.http.HttpStatus.NOT_FOUND);
                }
            })
            .orElse(ResponseUtils.error("Role not found", org.springframework.http.HttpStatus.NOT_FOUND));
    }
    
    @GetMapping
    @AuthRequired
    public ResponseEntity<Map<String, Object>> getAllRoles() {
        BaseResponse<PagedData<Page, RoleDto>> response = roleService.getAllRoles();
        
        if (response.getResult().isPresent()) {
            return ResponseUtils.success(response.getResult().get());
        } else {
            return ResponseUtils.error(response.getMessage().orElse("Failed to get roles"), 
                org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    @PutMapping("/{id}")
    @AuthRequired
    public ResponseEntity<Map<String, Object>> updateRole(@PathVariable String id, @Valid @RequestBody RoleRequest roleRequest) {
        BaseResponse<RoleDto> response = roleService.updateRole(id, roleRequest);
        
        if (response.getResult().isPresent()) {
            return ResponseUtils.success(response.getResult().get());
        } else {
            return ResponseUtils.error(response.getMessage().orElse("Failed to update role"), 
                org.springframework.http.HttpStatus.BAD_REQUEST);
        }
    }
    
    @DeleteMapping("/{id}")
    @AuthRequired
    public ResponseEntity<Map<String, Object>> deleteRole(@PathVariable String id) {
        try {
            roleService.deleteRole(id);
            return ResponseUtils.success(null, "Role deleted successfully");
        } catch (Exception e) {
            return ResponseUtils.error("Failed to delete role: " + e.getMessage(), 
                org.springframework.http.HttpStatus.BAD_REQUEST);
        }
    }
    
    @PostMapping("/initialize")
    @AuthRequired
    public ResponseEntity<Map<String, Object>> initializeDefaultRoles() {
        try {
            roleService.initializeDefaultRoles();
            return ResponseUtils.success(null, "Default roles initialized successfully");
        } catch (Exception e) {
            return ResponseUtils.error("Failed to initialize roles: " + e.getMessage(), 
                org.springframework.http.HttpStatus.BAD_REQUEST);
        }
    }
}
