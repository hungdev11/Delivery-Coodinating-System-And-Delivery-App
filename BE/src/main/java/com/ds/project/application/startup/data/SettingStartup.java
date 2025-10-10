package com.ds.project.application.startup.data;

import com.ds.project.common.entities.dto.request.SettingRequest;
import com.ds.project.common.interfaces.ISettingService;
import com.ds.project.app_context.models.Setting;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Setting startup data initialization service
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SettingStartup {
    
    private final ISettingService settingService;
    
    public void initializeSettings() {
        log.info("Initializing setting startup data...");
        
        try {
            // Initialize JWT settings
            initializeJwtSettings();
            log.info("Setting startup data initialization completed");
        } catch (Exception e) {
            log.error("Failed to initialize setting startup data: {}", e.getMessage());
            throw e;
        }
    }
    
    private void initializeJwtSettings() {
        log.info("Initializing JWT settings...");
        
        try {
            // JWT Expire Time - 1 hour
            if (settingService.getSettingByKey("JWT_EXPIRE_TIME").isEmpty()) {
                SettingRequest jwtExpireSetting = SettingRequest.builder()
                    .key("JWT_EXPIRE_TIME")
                    .group("security")
                    .value("3600") // 1 hour in seconds
                    .type(Setting.SettingType.INTEGER)
                    .description("JWT token expiration time in seconds")
                    .level(Setting.SettingLevel.SYSTEM)
                    .build();
                
                settingService.createSetting(jwtExpireSetting);
                log.info("Created JWT expire time setting: 1 hour");
            } else {
                log.info("JWT expire time setting already exists");
            }
            
            // JWT Save Login - 1 day
            if (settingService.getSettingByKey("JWT_SAVE_LOGIN_DURATION").isEmpty()) {
                SettingRequest jwtSaveLoginSetting = SettingRequest.builder()
                    .key("JWT_SAVE_LOGIN_DURATION")
                    .group("security")
                    .value("86400") // 1 day in seconds
                    .type(Setting.SettingType.INTEGER)
                    .description("JWT save login duration in seconds")
                    .level(Setting.SettingLevel.SYSTEM)
                    .build();
                
                settingService.createSetting(jwtSaveLoginSetting);
                log.info("Created JWT save login duration setting: 1 day");
            } else {
                log.info("JWT save login duration setting already exists");
            }
            
        } catch (Exception e) {
            log.error("Failed to initialize JWT settings: {}", e.getMessage());
            // If it's a table doesn't exist error, log and continue
            if (e.getMessage() != null && e.getMessage().contains("doesn't exist")) {
                log.warn("Settings table not yet created. Skipping JWT settings initialization.");
            } else {
                log.error("Unexpected error during JWT settings initialization", e);
            }
        }
    }
}
