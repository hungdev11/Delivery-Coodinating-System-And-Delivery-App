package com.ds.project.application.startup.data;

import com.ds.project.common.interfaces.IRoleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Role startup data initialization service
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RoleStartup {
    
    private final IRoleService roleService;
    
    public void initializeRoles() {
        log.info("Initializing role startup data...");
        
        try {
            // Use the business service to initialize default roles
            roleService.initializeDefaultRoles();
            log.info("Role startup data initialization completed");
        } catch (Exception e) {
            log.error("Failed to initialize role startup data: {}", e.getMessage());
            throw e;
        }
    }
}
