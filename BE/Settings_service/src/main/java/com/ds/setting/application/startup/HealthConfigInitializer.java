package com.ds.setting.application.startup;

import com.ds.setting.app_context.models.SystemSetting;
import com.ds.setting.app_context.models.SystemSetting.DisplayMode;
import com.ds.setting.app_context.models.SystemSetting.SettingLevel;
import com.ds.setting.app_context.models.SystemSetting.SettingType;
import com.ds.setting.app_context.repositories.SystemSettingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Initializes health monitoring configuration settings on startup
 */
@Slf4j
@Component
@Order(2) // Run after SecretsDataInitializer (Order 1)
@RequiredArgsConstructor
public class HealthConfigInitializer implements CommandLineRunner {

    private static final String GROUP_HEALTH = "HEALTH_MONITORING";
    
    private final SystemSettingRepository repository;

    @Override
    public void run(String... args) {
        log.info("Initializing HEALTH_MONITORING settings...");
        
        // Health ping interval in seconds (default: 10 seconds)
        String key = "HEALTH_PING_INTERVAL_SECONDS";
        if (!repository.existsById(key)) {
            SystemSetting setting = SystemSetting.builder()
                .key(key)
                .group(GROUP_HEALTH)
                .description("Interval in seconds for health status heartbeat. Services will publish health status to Kafka at this interval.")
                .type(SettingType.INTEGER)
                .value("10") // Default: 10 seconds
                .level(SettingLevel.SYSTEM)
                .isReadOnly(false)
                .displayMode(DisplayMode.NUMBER)
                .build();
            SystemSetting saved = repository.save(setting);
            if (saved != null) {
                log.info("Created health monitoring setting: {} with default value: 10 seconds", key);
            }
        } else {
            log.debug("Health monitoring setting {} already exists", key);
        }
        
        log.info("HEALTH_MONITORING initialization completed");
    }
}
