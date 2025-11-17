package com.ds.setting.application.controllers.v1;

import com.ds.setting.business.v1.services.SettingsService;
import com.ds.setting.common.entities.dto.CreateSettingRequest;
import com.ds.setting.common.entities.dto.SystemSettingDto;
import com.ds.setting.common.entities.dto.common.BaseResponse;
import com.ds.setting.common.entities.dto.common.PagedData;
import com.ds.setting.common.entities.dto.common.PagingRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST API Controller for Settings Management
 * Settings are always identified by group/key pair
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/settings")
@RequiredArgsConstructor
@Tag(name = "Settings", description = "System Settings Management API")
public class SettingsController {

    private final SettingsService settingsService;

    @PostMapping
    @Operation(summary = "List settings with filtering/sorting/paging (POST)")
    public ResponseEntity<BaseResponse<PagedData<SystemSettingDto>>> getSettings(@RequestBody PagingRequest query) {
        log.info("POST /api/v1/settings - List settings with query: {}", query);
        PagedData<SystemSettingDto> page = settingsService.getSettings(query);
        return ResponseEntity.ok(BaseResponse.success(page));
    }

    @GetMapping("/{group}")
    @Operation(summary = "Get all settings by group (service identifier)")
    public ResponseEntity<BaseResponse<List<SystemSettingDto>>> getSettingsByGroup(@PathVariable String group) {
        log.info("GET /api/v1/settings/{} - Get settings by group", group);
        List<SystemSettingDto> settings = settingsService.getByGroup(group);
        return ResponseEntity.ok(BaseResponse.success(settings));
    }

    @GetMapping("/{group}/{key}")
    @Operation(summary = "Get setting by group and key pair")
    public ResponseEntity<BaseResponse<SystemSettingDto>> getSetting(
            @PathVariable String group,
            @PathVariable String key) {
        log.info("GET /api/v1/settings/{}/{} - Get setting by group/key", group, key);
        SystemSettingDto setting = settingsService.getByKeyAndGroup(key, group);
        return ResponseEntity.ok(BaseResponse.success(setting));
    }

    @GetMapping("/{group}/{key}/value")
    @Operation(summary = "Get setting value only by group and key pair")
    public ResponseEntity<BaseResponse<String>> getSettingValue(
            @PathVariable String group,
            @PathVariable String key) {
        log.info("GET /api/v1/settings/{}/{}/value - Get setting value", group, key);
        String value = settingsService.getValueByKeyAndGroup(key, group);
        return ResponseEntity.ok(BaseResponse.success(value));
    }

    @PutMapping("/{group}/{key}")
    @Operation(summary = "Upsert (create or update) a setting by group/key pair")
    public ResponseEntity<BaseResponse<SystemSettingDto>> upsertSetting(
            @PathVariable String group,
            @PathVariable String key,
            @Valid @RequestBody CreateSettingRequest request,
            @RequestHeader(value = "X-User-Id", required = false, defaultValue = "system") String userId) {
        log.info("PUT /api/v1/settings/{}/{} - Upsert setting by: {}", group, key, userId);
        SystemSettingDto result = settingsService.upsertByKeyAndGroup(key, group, request, userId);
        return ResponseEntity.ok(BaseResponse.success(result, "Setting saved successfully"));
    }

    @DeleteMapping("/{group}/{key}")
    @Operation(summary = "Delete a setting by group/key pair")
    public ResponseEntity<BaseResponse<Void>> deleteSetting(
            @PathVariable String group,
            @PathVariable String key) {
        log.info("DELETE /api/v1/settings/{}/{} - Delete setting", group, key);
        settingsService.deleteByKeyAndGroup(key, group);
        return ResponseEntity.ok(BaseResponse.success(null, "Setting deleted successfully"));
    }

    @PutMapping("/{group}/bulk")
    @Operation(summary = "Bulk upsert (create or update) multiple settings in a group")
    public ResponseEntity<BaseResponse<List<SystemSettingDto>>> bulkUpsertSettings(
            @PathVariable String group,
            @Valid @RequestBody com.ds.setting.common.entities.dto.BulkUpsertSettingsRequest request,
            @RequestHeader(value = "X-User-Id", required = false, defaultValue = "system") String userId) {
        log.info("PUT /api/v1/settings/{}/bulk - Bulk upsert {} settings by: {}", group, request.getSettings().size(), userId);
        List<SystemSettingDto> results = settingsService.bulkUpsertByGroup(group, request.getSettings(), userId);
        return ResponseEntity.ok(BaseResponse.success(results, "Settings saved successfully"));
    }
}
