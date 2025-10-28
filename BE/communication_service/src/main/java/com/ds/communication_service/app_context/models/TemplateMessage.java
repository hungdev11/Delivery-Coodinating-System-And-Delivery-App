package com.ds.communication_service.app_context.models;

import java.sql.Types;
import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;

import com.ds.communication_service.common.enums.TemplateActionType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "template_messages", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"shortcut"})
})
@Getter
@Setter
public class TemplateMessage {

    @Id
    @JdbcTypeCode(Types.VARCHAR)
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 36, nullable = false, updatable = false)
    private UUID id;

    /**
     * Mã/phím tắt để tìm kiếm nhanh template. 
     * Ví dụ: "welcome_msg", "faq_payment"
     */
    @Column(name = "shortcut", nullable = false, unique = true, length = 100)
    private String shortcut;

    /**
     * Nội dung của tin nhắn mẫu.
     */
    @Column(name = "content", columnDefinition = "TEXT", nullable = false)
    private String content;

    /**
     * Role (vai trò) được yêu cầu để xem/sử dụng template này.
     * Ví dụ: "ROLE_SUPPORT", "ROLE_ADMIN"
     */
    @Column(name = "required_role", nullable = false, length = 50)
    private String requiredRole;

    /**
     * Loại hành động đi kèm (để xử lý "chức năng kèm theo").
     * Ví dụ: SIMPLE_TEXT (chỉ gửi text), QUICK_REPLY (gửi text và gợi ý trả lời nhanh)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 50)
    private TemplateActionType actionType;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
