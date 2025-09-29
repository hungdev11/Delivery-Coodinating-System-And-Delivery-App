package com.ds.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

import java.util.Arrays;

@SpringBootApplication
public class GatewayApplication {

    private static final Logger log = LoggerFactory.getLogger(GatewayApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }

    @Bean
    public ApplicationRunner logStartup(Environment environment) {
        return args -> {
            String applicationName = environment.getProperty("spring.application.name", "api-gateway");
            String httpPort = environment.getProperty("server.port", "unknown");
            String keycloakServerUrl = environment.getProperty("spring.security.oauth2.resourceserver.jwt.issuer-uri", "not-set");
            String[] activeProfiles = environment.getActiveProfiles();

            log.info("=".repeat(80));
            log.info("Application '{}' started successfully!", applicationName);
            log.info("Active profiles: {}", Arrays.toString(activeProfiles));
            log.info("HTTP REST Gateway port: {}", httpPort);
            log.info("Keycloak OAuth2 issuer: {}", keycloakServerUrl);
            log.info("=".repeat(80));
        };
    }
}
