package com.ds.communication_service.app_context.repositories;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ds.communication_service.app_context.models.InteractiveProposal;
import com.ds.communication_service.common.enums.ProposalStatus;

@Repository
public interface InteractiveProposalRepository extends JpaRepository<InteractiveProposal, UUID> {

    /**
     * Tìm tất cả các proposal có trạng thái (status) cụ thể
     * VÀ đã hết hạn (expires_at) trước hoặc bằng thời điểm hiện tại.
     *
     * Đây là truy vấn cốt lõi cho Cron Job xử lý timeout.
     */
    List<InteractiveProposal> findByStatusAndExpiresAtLessThanEqual(
        ProposalStatus status, 
        LocalDateTime now
    );
    
    // Bạn có thể thêm các hàm tìm kiếm khác ở đây, ví dụ:
    // Page<InteractiveProposal> findByConversationId(UUID conversationId, Pageable pageable);
}
