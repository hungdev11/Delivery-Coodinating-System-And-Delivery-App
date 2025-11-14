package com.ds.user.application.configs;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * WebClient configuration for calling external microservices
 */
@Configuration
public class WebClientConfig {

    @Bean("zoneServiceWebClient")
    public WebClient zoneServiceWebClient(
            @Value("${services.zone.base-url:http://localhost:21503}") String baseUrl) {
        return WebClient.builder()
                .baseUrl(baseUrl)
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024)) // 10MB
                .build();
    }

    @Bean("apiGatewayWebClient")
    public WebClient apiGatewayWebClient(
            @Value("${services.api-gateway.base-url:http://localhost:21500}") String baseUrl) {
        return WebClient.builder()
                .baseUrl(baseUrl)
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024)) // 10MB
                .build();
    }
}
