package com.ds.gateway.application.controllers.v1;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

/**
 * API Gateway proxy for Settings Service
 * Settings are always identified by group/key pair
 * Group represents the service/module identifier
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/settings")
@RequiredArgsConstructor
public class SettingsProxyController {

    private final RestTemplate restTemplate;

    @Value("${services.settings.base-url}")
    private String settingsServiceUrl;

    @PostMapping
    public ResponseEntity<?> listSettings(@RequestBody Object query) {
        log.info("POST /api/v1/settings - Proxying to Settings Service (list)");
        String url = settingsServiceUrl + "/api/v1/settings";
        return ResponseEntity.ok(restTemplate.postForObject(url, query, Object.class));
    }

    /**
     * Get all settings by group (service identifier)
     */
    @GetMapping("/{group}")
    public ResponseEntity<?> getSettingsByGroup(@PathVariable String group) {
        log.info("GET /api/v1/settings/{} - Proxying to Settings Service", group);
        String url = settingsServiceUrl + "/api/v1/settings/" + group;
        return ResponseEntity.ok(restTemplate.getForObject(url, Object.class));
    }

    /**
     * Get setting by group and key pair
     */
    @GetMapping("/{group}/{key}")
    public ResponseEntity<?> getSetting(@PathVariable String group, @PathVariable String key) {
        log.info("GET /api/v1/settings/{}/{} - Proxying to Settings Service", group, key);
        String url = settingsServiceUrl + "/api/v1/settings/" + group + "/" + key;
        return ResponseEntity.ok(restTemplate.getForObject(url, Object.class));
    }

    /**
     * Get setting value only by group and key pair
     */
    @GetMapping("/{group}/{key}/value")
    public ResponseEntity<String> getSettingValue(@PathVariable String group, @PathVariable String key) {
        log.info("GET /api/v1/settings/{}/{}/value - Proxying to Settings Service", group, key);
        String url = settingsServiceUrl + "/api/v1/settings/" + group + "/" + key + "/value";
        return ResponseEntity.ok(restTemplate.getForObject(url, String.class));
    }

    /**
     * Upsert (create or update) a setting by group/key pair
     * Requires admin authentication - implement role check
     */
    @PutMapping("/{group}/{key}")
    public ResponseEntity<?> upsertSetting(
            @PathVariable String group, 
            @PathVariable String key, 
            @RequestBody Object request,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        log.info("PUT /api/v1/settings/{}/{} - Proxying to Settings Service (user: {})", group, key, userId);
        String url = settingsServiceUrl + "/api/v1/settings/" + group + "/" + key;
        restTemplate.put(url, request);
        return ResponseEntity.ok().build();
    }

    /**
     * Delete a setting by group/key pair
     * Requires admin authentication - implement role check
     */
    @DeleteMapping("/{group}/{key}")
    public ResponseEntity<?> deleteSetting(@PathVariable String group, @PathVariable String key) {
        log.info("DELETE /api/v1/settings/{}/{} - Proxying to Settings Service", group, key);
        String url = settingsServiceUrl + "/api/v1/settings/" + group + "/" + key;
        restTemplate.delete(url);
        return ResponseEntity.noContent().build();
    }
}
