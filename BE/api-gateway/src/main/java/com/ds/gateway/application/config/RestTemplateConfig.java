package com.ds.gateway.application.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * RestTemplate configuration for API Gateway
 * Configured to avoid double chunked encoding issues with Cloudflare
 */
@Configuration
public class RestTemplateConfig {
    
    @Bean
    public RestTemplate restTemplate() {
        // Use SimpleClientHttpRequestFactory to avoid chunked encoding issues
        // This ensures responses are fully buffered before being sent
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10000); // 10 seconds connect timeout
        factory.setReadTimeout(180000); // 180 seconds (3 minutes) read timeout - matches nginx config
        
        RestTemplate restTemplate = new RestTemplate(factory);
        return restTemplate;
    }
}
