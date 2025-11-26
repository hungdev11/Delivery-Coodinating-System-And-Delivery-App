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

import java.util.List;

/**
 * Initializes default SECRETS settings on startup
 * These are API keys and tokens used by frontend applications
 */
@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class SecretsDataInitializer implements CommandLineRunner {

    private static final String GROUP_SECRETS = "SECRETS";
    
    private final SystemSettingRepository repository;

    @Override
    public void run(String... args) {
        log.info("Initializing SECRETS settings...");
        
        List<SecretDefinition> secrets = List.of(
            new SecretDefinition(
                "MAPTILER_API_KEY",
                "MapTiler API Key for map rendering",
                "",
                DisplayMode.PASSWORD
            ),
            new SecretDefinition(
                "GOOGLE_MAPS_API_KEY",
                "Google Maps API Key (optional)",
                "",
                DisplayMode.PASSWORD
            )
        );
        
        int created = 0;
        for (SecretDefinition secret : secrets) {
            if (!repository.existsById(secret.key)) {
                SystemSetting setting = SystemSetting.builder()
                    .key(secret.key)
                    .group(GROUP_SECRETS)
                    .description(secret.description)
                    .type(SettingType.STRING)
                    .value(secret.defaultValue)
                    .level(SettingLevel.SYSTEM)
                    .isReadOnly(false)
                    .displayMode(secret.displayMode)
                    .build();
                repository.save(setting);
                created++;
                log.debug("Created secret setting: {}", secret.key);
            }
        }
        
        log.info("SECRETS initialization completed: {} created, {} already exist", 
            created, secrets.size() - created);
    }
    
    private record SecretDefinition(
        String key,
        String description,
        String defaultValue,
        DisplayMode displayMode
    ) {}
}
