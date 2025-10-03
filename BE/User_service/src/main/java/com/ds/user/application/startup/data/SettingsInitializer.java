package com.ds.user.application.startup.data;

import com.ds.user.common.interfaces.ISettingsInitializationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Initialize Keycloak settings in Settings Service after Keycloak initialization
 */
@Slf4j
@Component
@Order(2) // Run after KeycloakDataInitializer (Order 1)
@RequiredArgsConstructor
public class SettingsInitializer implements CommandLineRunner {

    private final ISettingsInitializationService settingsInitializationService;

    @Override
    public void run(String... args) {
        log.info("=".repeat(80));
        log.info("INITIALIZING SETTINGS SERVICE INTEGRATION");
        log.info("=".repeat(80));
        
        long startTime = System.currentTimeMillis();
        settingsInitializationService.initialize();
        long endTime = System.currentTimeMillis();
        
        log.info("Settings Service initialization completed in {}ms", (endTime - startTime));
        log.info("=".repeat(80));
    }

    // Delegated to business service
}
