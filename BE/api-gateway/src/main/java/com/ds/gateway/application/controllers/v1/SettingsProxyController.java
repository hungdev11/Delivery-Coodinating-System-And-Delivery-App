package com.ds.gateway.application.controllers.v1;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

/**
 * API Gateway proxy for Settings Service
 * Allows external access to settings endpoints
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/settings")
@RequiredArgsConstructor
public class SettingsProxyController {

    private final RestTemplate restTemplate;

    @Value("${services.settings.base-url}")
    private String settingsServiceUrl;

    /**
     * Get setting by key (requires authentication)
     */
    @GetMapping("/{key}")
    public ResponseEntity<?> getSettingByKey(@PathVariable String key) {
        log.info("GET /api/v1/settings/{} - Proxying to Settings Service", key);
        String url = settingsServiceUrl + "/api/v1/settings/" + key;
        return ResponseEntity.ok(restTemplate.getForObject(url, Object.class));
    }

    /**
     * Get setting value by key (requires authentication)
     */
    @GetMapping("/{key}/value")
    public ResponseEntity<String> getSettingValue(@PathVariable String key) {
        log.info("GET /api/v1/settings/{}/value - Proxying to Settings Service", key);
        String url = settingsServiceUrl + "/api/v1/settings/" + key + "/value";
        return ResponseEntity.ok(restTemplate.getForObject(url, String.class));
    }

    /**
     * Get settings by group (requires authentication)
     */
    @GetMapping("/group/{group}")
    public ResponseEntity<?> getSettingsByGroup(@PathVariable String group) {
        log.info("GET /api/v1/settings/group/{} - Proxying to Settings Service", group);
        String url = settingsServiceUrl + "/api/v1/settings/group/" + group;
        return ResponseEntity.ok(restTemplate.getForObject(url, Object.class));
    }

    /**
     * Search settings (requires authentication)
     */
    @GetMapping("/search")
    public ResponseEntity<?> searchSettings(@RequestParam String q) {
        log.info("GET /api/v1/settings/search?q={} - Proxying to Settings Service", q);
        String url = settingsServiceUrl + "/api/v1/settings/search?q=" + q;
        return ResponseEntity.ok(restTemplate.getForObject(url, Object.class));
    }

    /**
     * Create setting (requires admin authentication - implement role check)
     */
    @PostMapping
    public ResponseEntity<?> createSetting(@RequestBody Object request) {
        log.info("POST /api/v1/settings - Proxying to Settings Service");
        String url = settingsServiceUrl + "/api/v1/settings";
        return ResponseEntity.ok(restTemplate.postForObject(url, request, Object.class));
    }

    /**
     * Update setting (requires admin authentication - implement role check)
     */
    @PutMapping("/{key}")
    public ResponseEntity<?> updateSetting(@PathVariable String key, @RequestBody Object request) {
        log.info("PUT /api/v1/settings/{} - Proxying to Settings Service", key);
        String url = settingsServiceUrl + "/api/v1/settings/" + key;
        restTemplate.put(url, request);
        return ResponseEntity.ok().build();
    }

    /**
     * Delete setting (requires admin authentication - implement role check)
     */
    @DeleteMapping("/{key}")
    public ResponseEntity<?> deleteSetting(@PathVariable String key) {
        log.info("DELETE /api/v1/settings/{} - Proxying to Settings Service", key);
        String url = settingsServiceUrl + "/api/v1/settings/" + key;
        restTemplate.delete(url);
        return ResponseEntity.noContent().build();
    }
}
