package com.ds.gateway.application.controllers.v1;

import com.ds.gateway.annotations.AuthRequired;
import com.ds.gateway.application.controllers.support.ProxyControllerSupport;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

    private static final String SETTINGS_SERVICE = "settings-service";

    private final ProxyControllerSupport proxyControllerSupport;

    @Value("${services.settings.base-url}")
    private String settingsServiceUrl;

    private String settingsBaseUrl;

    @PostConstruct
    private void init() {
        this.settingsBaseUrl = settingsServiceUrl + "/api/v1/settings";
    }

    @PostMapping
    public ResponseEntity<?> listSettings(@RequestBody Object query) {
        log.info("POST /api/v1/settings - Proxying to Settings Service (list)");
        return proxyControllerSupport.forward(SETTINGS_SERVICE, HttpMethod.POST, settingsBaseUrl, query);
    }

    /**
     * Get all settings by group (service identifier)
     */
    @GetMapping("/{group}")
    public ResponseEntity<?> getSettingsByGroup(@PathVariable String group) {
        log.info("GET /api/v1/settings/{} - Proxying to Settings Service", group);
        return proxyControllerSupport.forward(SETTINGS_SERVICE, HttpMethod.GET, settingsBaseUrl + "/" + group, null);
    }

    /**
     * Get setting by group and key pair
     */
    @GetMapping("/{group}/{key}")
    public ResponseEntity<?> getSetting(@PathVariable String group, @PathVariable String key) {
        log.info("GET /api/v1/settings/{}/{} - Proxying to Settings Service", group, key);
        return proxyControllerSupport.forward(SETTINGS_SERVICE, HttpMethod.GET, settingsBaseUrl + "/" + group + "/" + key, null);
    }

    /**
     * Get setting value only by group and key pair
     */
    @GetMapping("/{group}/{key}/value")
    public ResponseEntity<String> getSettingValue(@PathVariable String group, @PathVariable String key) {
        log.info("GET /api/v1/settings/{}/{}/value - Proxying to Settings Service", group, key);
        return proxyControllerSupport.forwardForString(SETTINGS_SERVICE, HttpMethod.GET, settingsBaseUrl + "/" + group + "/" + key + "/value", null);
    }

    /**
     * Upsert (create or update) a setting by group/key pair
     * Requires admin authentication - implement role check
     */
    @PutMapping("/{group}/{key}")
    @AuthRequired
    public ResponseEntity<?> upsertSetting(
            @PathVariable String group,
            @PathVariable String key,
            @RequestBody Object request) {
        log.info("PUT /api/v1/settings/{}/{} - Proxying to Settings Service", group, key);
        return proxyControllerSupport.forward(SETTINGS_SERVICE, HttpMethod.PUT, settingsBaseUrl + "/" + group + "/" + key, request);
    }

    /**
     * Delete a setting by group/key pair
     * Requires admin authentication - implement role check
     */
    @DeleteMapping("/{group}/{key}")
    @AuthRequired
    public ResponseEntity<?> deleteSetting(@PathVariable String group, @PathVariable String key) {
        log.info("DELETE /api/v1/settings/{}/{} - Proxying to Settings Service", group, key);
        return proxyControllerSupport.forward(SETTINGS_SERVICE, HttpMethod.DELETE, settingsBaseUrl + "/" + group + "/" + key, null);
    }
}
