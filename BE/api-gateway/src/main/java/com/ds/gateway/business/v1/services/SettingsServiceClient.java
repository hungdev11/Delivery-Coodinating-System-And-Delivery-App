package com.ds.gateway.business.v1.services;

import com.ds.gateway.common.entities.dto.common.BaseResponse;
import com.ds.gateway.common.entities.dto.settings.SystemSettingDto;
import com.ds.gateway.common.exceptions.ServiceUnavailableException;
import com.ds.gateway.common.interfaces.ISettingsServiceClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * REST client implementation for Settings Service
 */
@Slf4j
@Service
public class SettingsServiceClient implements ISettingsServiceClient {
    
    @Autowired
    @Qualifier("settingsServiceWebClient")
    private WebClient settingsServiceWebClient;
    
    @Override
    public CompletableFuture<SystemSettingDto> getSettingByKey(String key) {
        log.debug("Getting setting by key via REST: {}", key);
        
        return settingsServiceWebClient.get()
            .uri("/api/v1/settings/{key}", key)
            .retrieve()
            .bodyToMono(new ParameterizedTypeReference<BaseResponse<SystemSettingDto>>() {})
            .map(BaseResponse::getResult)
            .onErrorMap(ex -> new ServiceUnavailableException("Settings service unavailable: " + ex.getMessage(), ex))
            .toFuture();
    }
    
    @Override
    public CompletableFuture<String> getSettingValue(String key) {
        log.debug("Getting setting value via REST: {}", key);
        
        return settingsServiceWebClient.get()
            .uri("/api/v1/settings/{key}/value", key)
            .retrieve()
            .bodyToMono(new ParameterizedTypeReference<BaseResponse<String>>() {})
            .map(BaseResponse::getResult)
            .onErrorMap(ex -> new ServiceUnavailableException("Settings service unavailable: " + ex.getMessage(), ex))
            .toFuture();
    }
    
    @Override
    public CompletableFuture<String> getSettingValue(String key, String defaultValue) {
        log.debug("Getting setting value with default via REST: {}", key);
        
        return settingsServiceWebClient.get()
            .uri("/api/v1/settings/{key}/value", key)
            .retrieve()
            .bodyToMono(new ParameterizedTypeReference<BaseResponse<String>>() {})
            .map(BaseResponse::getResult)
            .onErrorResume(ex -> {
                log.debug("[api-gateway] [SettingsServiceClient.getSettingValue] Setting {} not found, using default value", key);
                return Mono.just(defaultValue);
            })
            .toFuture();
    }
    
    @Override
    public CompletableFuture<List<SystemSettingDto>> getSettingsByGroup(String group) {
        log.debug("Getting settings by group via REST: {}", group);
        
        return settingsServiceWebClient.get()
            .uri("/api/v1/settings/group/{group}", group)
            .retrieve()
            .bodyToMono(new ParameterizedTypeReference<BaseResponse<List<SystemSettingDto>>>() {})
            .map(BaseResponse::getResult)
            .onErrorMap(ex -> new ServiceUnavailableException("Settings service unavailable: " + ex.getMessage(), ex))
            .toFuture();
    }
    
    @Override
    public CompletableFuture<List<SystemSettingDto>> getAllKeycloakSettings() {
        log.debug("Getting all Keycloak settings via REST");
        
        return settingsServiceWebClient.get()
            .uri("/api/v1/settings/group/keycloak")
            .retrieve()
            .bodyToMono(new ParameterizedTypeReference<BaseResponse<List<SystemSettingDto>>>() {})
            .map(BaseResponse::getResult)
            .onErrorMap(ex -> new ServiceUnavailableException("Settings service unavailable: " + ex.getMessage(), ex))
            .toFuture();
    }
}
