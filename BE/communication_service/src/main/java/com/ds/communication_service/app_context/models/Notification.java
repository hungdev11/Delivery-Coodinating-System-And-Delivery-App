package com.ds.communication_service.app_context.models;

import com.ds.communication_service.common.dto.NotificationMessage;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;

import java.sql.Types;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Notification entity for storing in-app notifications
 * Supports various notification types and persistence
 */
@Entity
@Table(name = "notifications", indexes = {
    @Index(name = "idx_notifications_user_id", columnList = "user_id"),
    @Index(name = "idx_notifications_read", columnList = "is_read"),
    @Index(name = "idx_notifications_created_at", columnList = "created_at")
})
@Getter
@Setter
public class Notification {

    @Id
    @JdbcTypeCode(Types.VARCHAR)
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 36, nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 50)
    private NotificationMessage.NotificationType type;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "message", columnDefinition = "TEXT", nullable = false)
    private String message;

    @Column(name = "data", columnDefinition = "TEXT")
    private String data; // JSON data payload

    @Column(name = "is_read", nullable = false)
    private boolean read = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @Column(name = "action_url")
    private String actionUrl;

    /**
     * Convert to DTO
     */
    public NotificationMessage toDto() {
        return NotificationMessage.builder()
            .id(this.id.toString())
            .userId(this.userId)
            .type(this.type)
            .title(this.title)
            .message(this.message)
            .data(this.data)
            .read(this.read)
            .createdAt(this.createdAt)
            .readAt(this.readAt)
            .actionUrl(this.actionUrl)
            .build();
    }

    /**
     * Create from DTO
     */
    public static Notification fromDto(NotificationMessage dto) {
        Notification notification = new Notification();
        if (dto.getId() != null) {
            notification.setId(UUID.fromString(dto.getId()));
        }
        notification.setUserId(dto.getUserId());
        notification.setType(dto.getType());
        notification.setTitle(dto.getTitle());
        notification.setMessage(dto.getMessage());
        notification.setData(dto.getData());
        notification.setRead(dto.isRead());
        notification.setReadAt(dto.getReadAt());
        notification.setActionUrl(dto.getActionUrl());
        return notification;
    }
}
