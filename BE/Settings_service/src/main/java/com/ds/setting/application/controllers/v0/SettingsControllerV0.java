package com.ds.setting.application.controllers.v0;

import com.ds.setting.business.v1.services.SettingsService;
import com.ds.setting.common.entities.dto.SystemSettingDto;
import com.ds.setting.common.entities.dto.common.BaseResponse;
import com.ds.setting.common.entities.dto.common.PagedData;
import com.ds.setting.common.entities.dto.request.PagingRequestV0;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * V0 API Controller for Settings
 * V0: Simple paging and sorting without dynamic filters
 */
@Slf4j
@RestController
@RequestMapping("/api/v0/settings")
@RequiredArgsConstructor
public class SettingsControllerV0 {

    private final SettingsService settingsService;

    @PostMapping
    public ResponseEntity<BaseResponse<PagedData<SystemSettingDto>>> getSettings(
        @Valid @RequestBody PagingRequestV0 query
    ) {
        log.debug("[settings-service] [SettingsControllerV0.getSettings] POST /api/v0/settings - Get settings with simple paging (V0)");
        PagedData<SystemSettingDto> result = settingsService.getSettingsV0(query);
        return ResponseEntity.ok(BaseResponse.success(result));
    }
}
