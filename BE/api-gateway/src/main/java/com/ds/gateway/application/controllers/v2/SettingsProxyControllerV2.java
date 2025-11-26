package com.ds.gateway.application.controllers.v2;

import com.ds.gateway.application.controllers.support.ProxyControllerSupport;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Proxy controller for Settings Service V2 endpoints (enhanced filtering)
 */
@Slf4j
@RestController
@RequestMapping("/api/v2/settings")
@RequiredArgsConstructor
public class SettingsProxyControllerV2 {

    private static final String SETTINGS_SERVICE = "settings-service";

    private final ProxyControllerSupport proxyControllerSupport;

    @Value("${services.settings.base-url}")
    private String settingsServiceUrl;

    private String settingsV2Url;

    @PostConstruct
    private void init() {
        this.settingsV2Url = settingsServiceUrl + "/api/v2/settings";
    }

    @PostMapping
    public ResponseEntity<?> listSettings(@RequestBody Object requestBody) {
        log.debug("[api-gateway] [SettingsProxyControllerV2.listSettings] POST /api/v2/settings - proxy to Settings Service");
        return proxyControllerSupport.forward(SETTINGS_SERVICE, HttpMethod.POST, settingsV2Url, requestBody);
    }
}
