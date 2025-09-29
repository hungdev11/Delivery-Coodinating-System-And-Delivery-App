package com.ds.gateway.configs;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * WebClient configuration for calling microservices
 */
@Configuration
public class WebClientConfig {
    
    @Bean
    public WebClient userServiceWebClient(
            @Value("${services.user.base-url}") String baseUrl) {
        return WebClient.builder()
                .baseUrl(baseUrl)
                .build();
    }
    
    // Add more WebClient beans for other services as needed
    // @Bean
    // public WebClient productServiceWebClient(...) { ... }
    // @Bean
    // public WebClient orderServiceWebClient(...) { ... }
}
