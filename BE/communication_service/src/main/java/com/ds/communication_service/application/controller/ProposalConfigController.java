package com.ds.communication_service.application.controller;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ds.communication_service.app_context.models.ProposalTypeConfig;
import com.ds.communication_service.common.dto.BaseResponse;
import com.ds.communication_service.common.dto.ProposalConfigDTO;
import com.ds.communication_service.common.interfaces.IProposalConfigService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/proposals/configs")
public class ProposalConfigController {

    // Inject Interface
    private final IProposalConfigService configService;

    /**
     * Lấy tất cả các cấu hình proposal đang có.
     */
    @GetMapping
    public ResponseEntity<BaseResponse<List<ProposalTypeConfig>>> getAllConfigs() {
        return ResponseEntity.ok(BaseResponse.success(configService.getAllConfigs()));
    }

    /**
     * Tạo một loại proposal config mới.
     */
    @PostMapping
    public ResponseEntity<BaseResponse<ProposalTypeConfig>> createConfig(
            @RequestBody ProposalConfigDTO dto) {
        ProposalTypeConfig createdConfig = configService.createConfig(dto);
        return ResponseEntity
                .created(URI.create("/api/v1/admin/proposals/configs/" + createdConfig.getId()))
                .body(BaseResponse.success(createdConfig));
    }

    /**
     * Cập nhật một proposal config đã có.
     */
    @PutMapping("/{configId}")
    public ResponseEntity<BaseResponse<ProposalTypeConfig>> updateConfig(
            @PathVariable UUID configId,
            @RequestBody ProposalConfigDTO dto) {
        ProposalTypeConfig updatedConfig = configService.updateConfig(configId, dto);
        return ResponseEntity.ok(BaseResponse.success(updatedConfig));
    }

    /**
     * Xóa một proposal config.
     */
    @DeleteMapping("/{configId}")
    public ResponseEntity<Void> deleteConfig(@PathVariable UUID configId) {
        configService.deleteConfig(configId);
        return ResponseEntity.noContent().build(); // Trả về 204 No Content
    }
}
