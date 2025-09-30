package com.ds.setting.application.configs;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI/Swagger Configuration
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI settingsServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Settings Service API")
                        .description("System-wide settings and configuration management")
                        .version("1.0.0"));
    }
}

