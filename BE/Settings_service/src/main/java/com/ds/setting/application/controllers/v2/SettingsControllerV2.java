package com.ds.setting.application.controllers.v2;

import com.ds.setting.business.v1.services.SettingsService;
import com.ds.setting.common.entities.dto.SystemSettingDto;
import com.ds.setting.common.entities.dto.common.BaseResponse;
import com.ds.setting.common.entities.dto.common.PagedData;
import com.ds.setting.common.entities.dto.request.PagingRequestV2;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * V2 API Controller for Settings
 * V2: Enhanced dynamic filtering with operations between each pair of conditions
 */
@Slf4j
@RestController
@RequestMapping("/api/v2/settings")
@RequiredArgsConstructor
public class SettingsControllerV2 {

    private final SettingsService settingsService;

    @PostMapping
    public ResponseEntity<BaseResponse<PagedData<SystemSettingDto>>> getSettings(
        @Valid @RequestBody PagingRequestV2 query
    ) {
        log.info("POST /api/v2/settings - Get settings with enhanced filtering (V2)");
        PagedData<SystemSettingDto> result = settingsService.getSettingsV2(query);
        return ResponseEntity.ok(BaseResponse.success(result));
    }
}
