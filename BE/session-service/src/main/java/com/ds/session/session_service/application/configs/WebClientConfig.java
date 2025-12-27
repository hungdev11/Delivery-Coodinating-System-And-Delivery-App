package com.ds.session.session_service.application.configs;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * WebClient configuration for calling external microservices
 */
@Configuration
public class WebClientConfig {

    @Bean("userServiceWebClient")
    public WebClient userServiceWebClient(
            @Value("${services.user.base-url:http://localhost:21501}") String baseUrl) {
        return WebClient.builder()
                .baseUrl(baseUrl)
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024)) // 10MB
                .build();
    }

    @Bean("parcelServiceWebClient")
    public WebClient parcelServiceWebClient(
            @Value("${PARCEL_SERVICE_URL:http://localhost:21506}") String baseUrl) {
        return WebClient.builder()
                .baseUrl(baseUrl)
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024)) // 10MB
                .build();
    }
}
