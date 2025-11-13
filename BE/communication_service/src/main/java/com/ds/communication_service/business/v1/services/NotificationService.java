package com.ds.communication_service.business.v1.services;

import com.ds.communication_service.app_context.models.Notification;
import com.ds.communication_service.app_context.repositories.NotificationRepository;
import com.ds.communication_service.common.dto.NotificationMessage;
import com.ds.communication_service.infrastructure.kafka.EventProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing notifications
 * Handles creation, persistence, and delivery of notifications
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final EventProducer eventProducer;

    /**
     * Create and send notification to user
     * Saves notification to database and publishes to Kafka for real-time delivery
     * 
     * @param notificationDto Notification data
     * @return Saved notification
     */
    @Transactional
    public NotificationMessage createAndSendNotification(NotificationMessage notificationDto) {
        // Save notification to database
        Notification notification = Notification.fromDto(notificationDto);
        Notification savedNotification = notificationRepository.save(notification);
        
        log.info("✅ Notification saved to database: id={}, userId={}, type={}", 
            savedNotification.getId(), savedNotification.getUserId(), savedNotification.getType());
        
        // Convert back to DTO
        NotificationMessage savedDto = savedNotification.toDto();
        
        // Publish to Kafka for real-time delivery via WebSocket
        try {
            eventProducer.publishNotification(savedNotification.getUserId(), savedDto);
            log.info("📤 Notification published to Kafka for real-time delivery");
        } catch (Exception e) {
            log.error("❌ Failed to publish notification to Kafka: {}", e.getMessage(), e);
            // Notification is still saved in DB, can be retrieved via API
        }
        
        return savedDto;
    }

    /**
     * Get notifications for a user (paginated)
     * 
     * @param userId User ID
     * @param pageable Pagination info
     * @return Page of notifications
     */
    public Page<NotificationMessage> getNotificationsForUser(String userId, Pageable pageable) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
            .map(Notification::toDto);
    }

    /**
     * Get unread notifications for a user
     * 
     * @param userId User ID
     * @return List of unread notifications
     */
    public List<NotificationMessage> getUnreadNotifications(String userId) {
        return notificationRepository.findByUserIdAndReadFalseOrderByCreatedAtDesc(userId)
            .stream()
            .map(Notification::toDto)
            .collect(Collectors.toList());
    }

    /**
     * Get unread notification count for a user
     * 
     * @param userId User ID
     * @return Count of unread notifications
     */
    public long getUnreadCount(String userId) {
        return notificationRepository.countByUserIdAndReadFalse(userId);
    }

    /**
     * Mark notification as read
     * 
     * @param notificationId Notification ID
     * @param userId User ID (for security check)
     * @return Updated notification
     */
    @Transactional
    public NotificationMessage markAsRead(String notificationId, String userId) {
        UUID id = UUID.fromString(notificationId);
        Notification notification = notificationRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Notification not found: " + notificationId));
        
        // Security check: ensure user owns this notification
        if (!notification.getUserId().equals(userId)) {
            throw new SecurityException("User does not have access to this notification");
        }
        
        if (!notification.isRead()) {
            notification.setRead(true);
            notification.setReadAt(LocalDateTime.now());
            notification = notificationRepository.save(notification);
            log.info("✅ Notification marked as read: id={}, userId={}", notificationId, userId);
        }
        
        return notification.toDto();
    }

    /**
     * Mark all notifications as read for a user
     * 
     * @param userId User ID
     * @return Count of notifications marked as read
     */
    @Transactional
    public int markAllAsRead(String userId) {
        List<Notification> unreadNotifications = 
            notificationRepository.findByUserIdAndReadFalseOrderByCreatedAtDesc(userId);
        
        LocalDateTime now = LocalDateTime.now();
        unreadNotifications.forEach(notification -> {
            notification.setRead(true);
            notification.setReadAt(now);
        });
        
        notificationRepository.saveAll(unreadNotifications);
        
        int count = unreadNotifications.size();
        log.info("✅ Marked {} notifications as read for userId={}", count, userId);
        return count;
    }

    /**
     * Delete notification
     * 
     * @param notificationId Notification ID
     * @param userId User ID (for security check)
     */
    @Transactional
    public void deleteNotification(String notificationId, String userId) {
        UUID id = UUID.fromString(notificationId);
        Notification notification = notificationRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Notification not found: " + notificationId));
        
        // Security check: ensure user owns this notification
        if (!notification.getUserId().equals(userId)) {
            throw new SecurityException("User does not have access to this notification");
        }
        
        notificationRepository.delete(notification);
        log.info("✅ Notification deleted: id={}, userId={}", notificationId, userId);
    }

    /**
     * Clean up old notifications (maintenance task)
     * Should be called periodically (e.g., daily cron job)
     * 
     * @param daysOld Delete notifications older than this many days
     */
    @Transactional
    public void deleteOldNotifications(int daysOld) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(daysOld);
        notificationRepository.deleteByCreatedAtBefore(cutoffDate);
        log.info("✅ Deleted notifications older than {} days (before {})", daysOld, cutoffDate);
    }
}
