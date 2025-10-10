package com.ds.project.application.startup;

import com.ds.project.application.startup.data.RoleStartup;
import com.ds.project.application.startup.data.SettingStartup;
import com.ds.project.application.startup.data.UserStartup;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * Main application startup coordinator
 * Orchestrates all data startup components asynchronously
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Order(0)
public class AppStartup implements CommandLineRunner {
    
    private final RoleStartup roleStartup;
    private final UserStartup userStartup;
    private final SettingStartup settingStartup;
    
    @Override
    public void run(String... args) throws Exception {
        log.info("Starting application startup process...");
        
        try {
            // Execute startup components asynchronously in proper order
            CompletableFuture<Void> roleFuture = initializeRolesAsync();
            CompletableFuture<Void> userFuture = roleFuture.thenCompose(v -> initializeUsersAsync());
            CompletableFuture<Void> settingFuture = userFuture.thenCompose(v -> initializeSettingsAsync());
            
            // Wait for all startup processes to complete
            CompletableFuture.allOf(roleFuture, userFuture, settingFuture).join();
            
            log.info("Application startup process completed successfully");
        } catch (Exception e) {
            log.error("Application startup process failed: {}", e.getMessage(), e);
            throw e;
        }
    }
    
    @Async
    public CompletableFuture<Void> initializeRolesAsync() {
        return CompletableFuture.runAsync(() -> {
            try {
                roleStartup.initializeRoles();
            } catch (Exception e) {
                log.error("Failed to initialize roles asynchronously: {}", e.getMessage());
                throw new RuntimeException("Role initialization failed", e);
            }
        });
    }
    
    @Async
    public CompletableFuture<Void> initializeUsersAsync() {
        return CompletableFuture.runAsync(() -> {
            try {
                userStartup.initializeUsers();
            } catch (Exception e) {
                log.error("Failed to initialize users asynchronously: {}", e.getMessage());
                throw new RuntimeException("User initialization failed", e);
            }
        });
    }
    
    @Async
    public CompletableFuture<Void> initializeSettingsAsync() {
        return CompletableFuture.runAsync(() -> {
            try {
                settingStartup.initializeSettings();
            } catch (Exception e) {
                log.error("Failed to initialize settings asynchronously: {}", e.getMessage());
                throw new RuntimeException("Settings initialization failed", e);
            }
        });
    }
}
