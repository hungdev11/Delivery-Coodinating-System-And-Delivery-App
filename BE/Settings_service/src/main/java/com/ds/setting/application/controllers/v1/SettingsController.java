package com.ds.setting.application.controllers.v1;

import com.ds.setting.app_context.models.SystemSetting.SettingLevel;
import com.ds.setting.business.v1.services.SettingsService;
import com.ds.setting.common.entities.dto.CreateSettingRequest;
import com.ds.setting.common.entities.dto.SystemSettingDto;
import com.ds.setting.common.entities.dto.UpdateSettingRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST API Controller for Settings Management
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/settings")
@RequiredArgsConstructor
@Tag(name = "Settings", description = "System Settings Management API")
public class SettingsController {

    private final SettingsService settingsService;

    @GetMapping
    @Operation(summary = "Get all settings")
    public ResponseEntity<List<SystemSettingDto>> getAllSettings() {
        log.info("GET /api/v1/settings - Get all settings");
        return ResponseEntity.ok(settingsService.getAllSettings());
    }

    @GetMapping("/{key}")
    @Operation(summary = "Get setting by key")
    public ResponseEntity<SystemSettingDto> getSettingByKey(@PathVariable String key) {
        log.info("GET /api/v1/settings/{} - Get setting by key", key);
        return ResponseEntity.ok(settingsService.getByKey(key));
    }

    @GetMapping("/{key}/value")
    @Operation(summary = "Get setting value by key")
    public ResponseEntity<String> getSettingValue(@PathVariable String key) {
        log.info("GET /api/v1/settings/{}/value - Get setting value", key);
        return ResponseEntity.ok(settingsService.getValue(key));
    }

    @GetMapping("/group/{group}")
    @Operation(summary = "Get settings by group")
    public ResponseEntity<List<SystemSettingDto>> getSettingsByGroup(@PathVariable String group) {
        log.info("GET /api/v1/settings/group/{} - Get settings by group", group);
        return ResponseEntity.ok(settingsService.getByGroup(group));
    }

    @GetMapping("/level/{level}")
    @Operation(summary = "Get settings by level")
    public ResponseEntity<List<SystemSettingDto>> getSettingsByLevel(@PathVariable SettingLevel level) {
        log.info("GET /api/v1/settings/level/{} - Get settings by level", level);
        return ResponseEntity.ok(settingsService.getByLevel(level));
    }


    @GetMapping("/search")
    @Operation(summary = "Search settings")
    public ResponseEntity<List<SystemSettingDto>> searchSettings(@RequestParam String q) {
        log.info("GET /api/v1/settings/search?q={} - Search settings", q);
        return ResponseEntity.ok(settingsService.searchSettings(q));
    }

    @PostMapping
    @Operation(summary = "Create a new setting")
    public ResponseEntity<SystemSettingDto> createSetting(@Valid @RequestBody CreateSettingRequest request) {
        log.info("POST /api/v1/settings - Create setting: key={}", request.getKey());
        SystemSettingDto created = settingsService.createSetting(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{key}")
    @Operation(summary = "Update a setting")
    public ResponseEntity<SystemSettingDto> updateSetting(
            @PathVariable String key,
            @Valid @RequestBody UpdateSettingRequest request,
            @RequestHeader(value = "X-User-Id", required = false, defaultValue = "system") String userId) {
        log.info("PUT /api/v1/settings/{} - Update setting by: {}", key, userId);
        SystemSettingDto updated = settingsService.updateSetting(key, request, userId);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{key}")
    @Operation(summary = "Delete a setting")
    public ResponseEntity<Void> deleteSetting(@PathVariable String key) {
        log.info("DELETE /api/v1/settings/{} - Delete setting", key);
        settingsService.deleteSetting(key);
        return ResponseEntity.noContent().build();
    }
}
