package com.ds.communication_service.business.v1.services;

import java.util.List;
import java.util.UUID;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.ds.communication_service.app_context.models.ProposalTypeConfig;
import com.ds.communication_service.app_context.repositories.ProposalTypeConfigRepository;
import com.ds.communication_service.common.dto.ProposalConfigDTO;
import com.ds.communication_service.common.enums.ProposalType;
import com.ds.communication_service.common.interfaces.IProposalConfigService;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProposalConfigService implements IProposalConfigService{

    private final ProposalTypeConfigRepository configRepo;

    /**
     * Lấy tất cả cấu hình (dùng cho trang Admin).
     * @Cacheable - Cache lại kết quả của toàn bộ danh sách.
     */
    @Cacheable(value = "proposal-configs-all")
    @Override
    public List<ProposalTypeConfig> getAllConfigs() {
        log.debug("[communication-service] [ProposalConfigService.getAllConfigs] Lấy tất cả proposal configs từ DB");
        return configRepo.findAll();
    }

    /**
     * Lấy 1 cấu hình theo Type (sẽ dùng cache từ Repository).
     */
    @Override
    public ProposalTypeConfig getConfigByType(ProposalType type) {
        return configRepo.findByType(type)
            .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy cấu hình cho: " + type));
    }

    /**
     * Tạo một cấu hình proposal mới.
     * Cần xóa tất cả cache liên quan.
     */
    @Transactional
    @Override
    // Xóa cache của 'findByType', 'existsByType', và 'getAll'
    @CacheEvict(value = {
        "proposal-configs-by-type", 
        "proposal-configs-exist-by-type",
        "proposal-configs-all"
    }, allEntries = true) // allEntries = true -> Xóa sạch cache, đơn giản nhất
    public ProposalTypeConfig createConfig(ProposalConfigDTO dto) {
        if (configRepo.existsByType(dto.getType())) {
            throw new IllegalArgumentException("Cấu hình cho " + dto.getType() + " đã tồn tại.");
        }
        
        ProposalTypeConfig config = new ProposalTypeConfig();
        config.setType(dto.getType());
        config.setRequiredRole(dto.getRequiredRole());
        config.setDescription(dto.getDescription());
        config.setDefaultTimeoutMinutes(dto.getDefaultTimeoutMinutes());
        
        log.debug("[communication-service] [ProposalConfigService.createConfig] Tạo mới proposal config cho: {}", dto.getType());
        return configRepo.save(config);
    }

    /**
     * Cập nhật một cấu hình proposal.
     * Cần xóa tất cả cache liên quan.
     */
    @Override
    @Transactional
    @CacheEvict(value = {
        "proposal-configs-by-type", 
        "proposal-configs-exist-by-type",
        "proposal-configs-all"
    }, allEntries = true)
    public ProposalTypeConfig updateConfig(UUID configId, ProposalConfigDTO dto) {
        ProposalTypeConfig config = configRepo.findById(configId)
            .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy Config ID: " + configId));

        // Kiểm tra nếu đổi Type và Type mới đã tồn tại
        if (!config.getType().equals(dto.getType()) && configRepo.existsByType(dto.getType())) {
             throw new IllegalArgumentException("Cấu hình cho " + dto.getType() + " đã tồn tại.");
        }

        config.setType(dto.getType());
        config.setRequiredRole(dto.getRequiredRole());
        config.setDescription(dto.getDescription());
        config.setDefaultTimeoutMinutes(dto.getDefaultTimeoutMinutes());
        
        log.debug("[communication-service] [ProposalConfigService.updateConfig] Cập nhật proposal config cho: {}", dto.getType());
        return configRepo.save(config);
    }

    /**
     * Xóa một cấu hình proposal.
     * Cần xóa tất cả cache liên quan.
     */
    @Override
    @Transactional
    @CacheEvict(value = {
        "proposal-configs-by-type", 
        "proposal-configs-exist-by-type",
        "proposal-configs-all"
    }, allEntries = true)
    public void deleteConfig(UUID configId) {
        if (!configRepo.existsById(configId)) {
             throw new EntityNotFoundException("Không tìm thấy Config ID: " + configId);
        }
        log.debug("[communication-service] [ProposalConfigService.deleteConfig] Xóa proposal config ID: {}", configId);
        configRepo.deleteById(configId);
    }
}
