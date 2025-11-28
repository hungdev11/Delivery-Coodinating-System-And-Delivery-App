package com.ds.gateway.application.controllers.v1;

import com.ds.gateway.annotations.PublicRoute;
import com.ds.gateway.common.entities.dto.common.BaseResponse;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Public configuration controller
 * Exposes non-sensitive configuration and API keys for frontend applications
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/config")
@RequiredArgsConstructor
public class ConfigController {

    private final RestTemplate restTemplate;

    @Value("${services.settings.base-url}")
    private String settingsServiceUrl;

    // List of secret keys that are safe to expose to frontend
    private static final List<String> PUBLIC_SECRET_KEYS = List.of(
        "MAPTILER_API_KEY",
        "GOOGLE_MAPS_API_KEY"
    );

    /**
     * Get public configuration for frontend applications
     * This endpoint is public (no authentication required)
     */
    @PublicRoute
    @GetMapping("/public")
    public ResponseEntity<BaseResponse<Map<String, Object>>> getPublicConfig() {
        log.debug("Fetching public configuration");
        
        Map<String, Object> config = new HashMap<>();
        
        try {
            // Fetch SECRETS group from Settings Service
            String url = settingsServiceUrl + "/api/v1/settings/SECRETS";
            JsonNode response = restTemplate.getForObject(url, JsonNode.class);
            
            if (response != null) {
                // BaseResponse uses "result" field, not "data"
                JsonNode settingsNode = response.has("result") ? response.get("result") : 
                                       response.has("data") ? response.get("data") : null;
                
                if (settingsNode != null && settingsNode.isArray()) {
                    Map<String, String> secrets = new HashMap<>();
                    
                    for (JsonNode setting : settingsNode) {
                        String key = setting.has("key") ? setting.get("key").asText() : null;
                        // Get value - handle null properly
                        String value = null;
                        if (setting.has("value") && !setting.get("value").isNull()) {
                            value = setting.get("value").asText();
                        }
                        
                        // Only expose whitelisted keys and ensure value is not null/empty
                        if (key != null && PUBLIC_SECRET_KEYS.contains(key) && value != null && !value.trim().isEmpty()) {
                            secrets.put(key, value);
                            log.debug("Loaded secret key: {} (value length: {})", key, value.length());
                        } else if (key != null && PUBLIC_SECRET_KEYS.contains(key)) {
                            log.warn("Secret key {} found but value is null or empty", key);
                        }
                    }
                    
                    config.put("secrets", secrets);
                    log.debug("Loaded {} public secrets", secrets.size());
                } else {
                    log.warn("Settings response format is invalid or empty");
                }
            }
        } catch (Exception e) {
            log.error("Failed to fetch secrets from Settings Service", e);
            config.put("secrets", Map.of());
        }
        
        // Add other public config if needed
        config.put("version", "1.0.0");
        
        return ResponseEntity.ok(BaseResponse.success(config, "Public configuration"));
    }
}
