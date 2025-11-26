package com.ds.gateway.application.configs;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

import io.netty.channel.ChannelOption;
import reactor.netty.http.client.HttpClient;

/**
 * WebClient and RestTemplate configuration for calling microservices
 */
@Configuration
public class WebClientConfig {

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        // Configure timeouts to prevent hanging requests
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10000); // 10 seconds connect timeout
        factory.setReadTimeout(30000); // 30 seconds read timeout
        
        return builder
            .requestFactory(() -> factory)
            .build();
    }

    @Bean("userServiceWebClient")
    public WebClient userServiceWebClient(
            @Value("${services.user.base-url}") String baseUrl) {
        return WebClient.builder()
                .baseUrl(baseUrl)
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(50 * 1024 * 1024)) // 50MB
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
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024)) // 10MB
                .build();
    }

    @Bean("parcelServiceWebClient")
    public WebClient parcelServiceWebClient(
            @Value("${services.parcel.base-url}") String baseUrl) {
        return WebClient.builder()
                .baseUrl(baseUrl)
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024)) // 10MB
                .build();
    }

    @Bean("sessionServiceWebClient")
    public WebClient sessionServiceWebClient(
            @Value("${services.session.base-url}") String baseUrl) {
        // Configure HttpClient with extended timeouts for session operations
        // Session operations (like accept-parcel) may take longer due to routing validation
        HttpClient httpClient = HttpClient.create()
                .responseTimeout(Duration.ofSeconds(60)) // 60 seconds for response
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000); // 10 seconds connect timeout
        
        return WebClient.builder()
                .baseUrl(baseUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024)) // 10MB
                .build();
    }

    @Bean("communicationServiceWebClient")
    public WebClient communicationServiceWebClient(
            @Value("${services.communication.base-url}") String baseUrl) {
        return WebClient.builder()
                .baseUrl(baseUrl)
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024)) // 10MB
                .build();
    }
}
