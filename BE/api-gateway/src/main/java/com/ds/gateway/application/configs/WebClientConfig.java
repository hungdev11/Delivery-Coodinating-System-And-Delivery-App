package com.ds.gateway.application.configs;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * WebClient and RestTemplate configuration for calling microservices
 */
@Configuration
public class WebClientConfig {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean("userServiceWebClient")
    public WebClient userServiceWebClient(
            @Value("${services.user.base-url}") String baseUrl) {
        return WebClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    @Bean("keycloakWebClient")
    public WebClient keycloakWebClient(
            @Value("${keycloak.auth-server-url}") String keycloakUrl) {
        return WebClient.builder()
                .baseUrl(keycloakUrl)
                .build();
    }

    @Bean("settingsServiceWebClient")
    public WebClient settingsServiceWebClient(
            @Value("${services.settings.base-url}") String baseUrl) {
        return WebClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    @Bean("zoneServiceWebClient")
    public WebClient zoneServiceWebClient(
            @Value("${services.zone.base-url}") String baseUrl) {
        return WebClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    @Bean("parcelServiceWebClient")
    public WebClient parcelServiceWebClient(
            @Value("${services.parcel.base-url}") String baseUrl) {
        return WebClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    @Bean("sessionServiceWebClient")
    public WebClient sessionServiceWebClient(
            @Value("${services.session.base-url}") String baseUrl) {
        return WebClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    @Bean("communicationServiceWebClient")
    public WebClient communicationServiceWebClient(
            @Value("${services.communication.base-url}") String baseUrl) {
        return WebClient.builder()
                .baseUrl(baseUrl)
                .build();
    }
}
