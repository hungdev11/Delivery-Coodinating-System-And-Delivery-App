package com.ds.communication_service.app_context.models;

import java.sql.Types;
import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;

import com.ds.communication_service.common.enums.ProposalActionType;
import com.ds.communication_service.common.enums.ProposalStatus;
import com.ds.communication_service.common.enums.ProposalType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "interactive_proposals")
@Getter
@Setter
public class InteractiveProposal {

    @Id
    @JdbcTypeCode(Types.VARCHAR)
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 36, nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    /**
     * ID của người gửi đề nghị (Shipper hoặc Customer).
     */
    @Column(name = "proposer_id", nullable = false)
    private String proposerId;

    /**
     * ID của người nhận đề nghị.
     */
    @Column(name = "recipient_id", nullable = false)
    private String recipientId;

    /**
     * Loại đề nghị, ví dụ: "CONFIRM_REFUSAL", "POSTPONE_REQUEST".
     * Client sẽ dựa vào đây để render UI.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 100)
    private ProposalType type;

    /**
     * Trạng thái hiện tại của đề nghị.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private ProposalStatus status;

    /**
     * Thời điểm đề nghị này hết hạn (nếu không được phản hồi).
     */
    @Column(name = "expires_at", nullable = true) // Đặt là true nếu có 1 số proposal không hết hạn
    private LocalDateTime expiresAt;

    /**
     * 'data' giờ là payload để render UI.
     * Ví dụ cho DATE_PICKER:
     * {"title": "Vui lòng chọn ngày giao mới", "min_date": "2025-10-31"}
     *
     * Ví dụ cho TEXT_INPUT:
     * {"title": "Vui lòng nhập lý do:", "placeholder": "Nhập lý do..."}
     */
    @Column(name = "data", columnDefinition = "TEXT")
    private String data; // JSON payload for UI rendering

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 50)
    private ProposalActionType actionType;
    
    /**
     * TRƯỜNG MỚI:
     * Lưu trữ kết quả của tương tác.
     * Ví dụ: "2025-11-01" (nếu là DATE_PICKER)
     * hoặc "Hàng bị ướt" (nếu là TEXT_INPUT)
     */
    @Column(name = "result_data", columnDefinition = "TEXT")
    private String resultData;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}