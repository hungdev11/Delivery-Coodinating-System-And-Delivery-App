package com.ds.user.application.startup.data;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * CommandLineRunner to initialize Keycloak data on application startup
 */
@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class KeycloakDataInitializer implements CommandLineRunner {

    private final KeycloakInitializationService keycloakInitializationService;

    @Override
    public void run(String... args) {
        log.info("=".repeat(80));
        log.info("Starting Keycloak Data Initialization");
        log.info("=".repeat(80));

        try {
            keycloakInitializationService.initializeKeycloakData();
            log.info("=".repeat(80));
            log.info("Keycloak Data Initialization Completed Successfully");
            log.info("=".repeat(80));
        } catch (Exception e) {
            log.error("=".repeat(80));
            log.error("FAILED to initialize Keycloak data: {}", e.getMessage(), e);
            log.error("=".repeat(80));
            // Don't throw - allow application to continue even if seed fails
        }
    }
}
