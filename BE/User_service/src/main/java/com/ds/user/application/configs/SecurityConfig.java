package com.ds.user.application.configs;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security Configuration
 * 
 * NOTE: Authentication & Authorization are handled by API Gateway.
 * This service is intended to be called internally from other services via gRPC
 * or through the API Gateway which already validates JWT tokens.
 * 
 * Therefore, we permit all requests at this service level.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(authorize -> authorize
                .anyRequest().permitAll()
            );
        
        return http.build();
    }
}
