package com.ds.communication_service.app_context.repositories;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ds.communication_service.app_context.models.ProposalTypeConfig;
import com.ds.communication_service.common.enums.ProposalType;

@Repository
public interface ProposalTypeConfigRepository extends JpaRepository<ProposalTypeConfig, UUID> {

    /**
     * Tìm cấu hình theo 'type' (đã được cache).
     * 'proposal-configs-by-type' là tên vùng cache.
     * key = "#type.name()" dùng tên của Enum làm key (ví dụ: "CONFIRM_REFUSAL")
     */
    @Cacheable(value = "proposal-configs-by-type", key = "#type.name()")
    Optional<ProposalTypeConfig> findByType(ProposalType type);

    /**
     * Kiểm tra sự tồn tại (dùng cho Startup Validator) (đã được cache).
     */
    @Cacheable(value = "proposal-configs-exist-by-type", key = "#type.name()")
    boolean existsByType(ProposalType type);

    List<ProposalTypeConfig> findByRequiredRoleIn(Collection<String> roles);
}
