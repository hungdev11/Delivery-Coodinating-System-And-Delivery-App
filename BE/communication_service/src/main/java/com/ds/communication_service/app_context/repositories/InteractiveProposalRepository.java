package com.ds.communication_service.app_context.repositories;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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
    
    /**
     * Tìm proposal với conversation được load (fetch join) để tránh LazyInitializationException.
     * Sử dụng khi cần truy cập conversation sau khi transaction đóng.
     */
    @Query("SELECT p FROM InteractiveProposal p JOIN FETCH p.conversation WHERE p.id = :id")
    Optional<InteractiveProposal> findByIdWithConversation(@Param("id") UUID id);
    
    // Bạn có thể thêm các hàm tìm kiếm khác ở đây, ví dụ:
    // Page<InteractiveProposal> findByConversationId(UUID conversationId, Pageable pageable);
}
