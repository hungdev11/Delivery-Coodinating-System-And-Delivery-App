package com.ds.project.application.startup.data;

import com.ds.project.common.entities.dto.request.UserRequest;
import com.ds.project.common.interfaces.IRoleService;
import com.ds.project.common.interfaces.IUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * User startup data initialization service
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserStartup {
    
    private final IUserService userService;
    private final IRoleService roleService;
    
    @Value("${app.admin.email:admin@mail.mail}")
    private String adminEmail;
    
    @Value("${app.admin.username:admin}")
    private String adminUsername;
    
    @Value("${app.admin.password:admin}")
    private String adminPassword;
    
    @Value("${app.startup.create-dev-datas:false}")
    private boolean createDevUsers;
    
    public void initializeUsers() {
        log.info("Initializing user startup data...");
        
        try {
            // Create admin user
            createAdminUser();
            
            // Create development users if configured
            if (createDevUsers) {
                createDevelopmentUsers();
            }   
            
            log.info("User startup data initialization completed");
        } catch (Exception e) {
            log.error("Failed to initialize user startup data: {}", e.getMessage());
            throw e;
        }
    }
    
    private void createAdminUser() {
        try {
            // Check if admin user already exists
            if (userService.getUserByEmail(adminEmail).isPresent()) {
                log.info("Admin user already exists: {}", adminEmail);
                return;
            }
            
            // Create admin user using business service
            UserRequest adminRequest = UserRequest.builder()
                .email(adminEmail)
                .username(adminUsername)
                .firstName("")
                .lastName("")
                .password(adminPassword)
                .roles(Set.of("ADMIN"))
                .build();
            
            userService.createUser(adminRequest);
        } catch (Exception e) {
            log.error("Failed to create admin user: {}", e.getMessage());
        }
    }
    
    private void createDevelopmentUsers() {
        log.info("Creating development users...");
        
        try {
            // Check if MANAGER role exists
            if (roleService.getRoleByName("MANAGER").isEmpty()) {
                log.error("MANAGER role not found. Please ensure roles are initialized first.");
                return;
            }
            
            for (int i = 0; i <= 20; i++) {
                String email = String.format("manager%d@mail.mail", i);
                String username = String.format("manager%d", i);
                
                try {
                    // Check if user already exists
                    if (userService.getUserByEmail(email).isPresent()) {
                        log.info("Manager user already exists: {}", email);
                        continue;
                    }

                    // Set roles for the user, first user is admin, other users are manager
                    Set<String> roles = i == 0 ? Set.of("ADMIN") : Set.of("MANAGER");
                    
                    // Create manager user using business service
                    UserRequest managerRequest = UserRequest.builder()  
                        .email(email)
                        .username(username)
                        .firstName("Manager")
                        .lastName(String.format("User %d", i))
                        .password("password")
                        .roles(roles)
                        .build();
                    
                    userService.createUser(managerRequest);
                    log.info("Successfully created manager user: {}", email);
                    
                } catch (Exception e) {
                    log.error("Failed to create manager user {}: {}", email, e.getMessage(), e);
                }
            }
        } catch (Exception e) {
            log.error("Failed to create development users: {}", e.getMessage());
        }
    }
}
