package com.ds.communication_service.app_context.repositories;

import com.ds.communication_service.app_context.models.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for Notification entity
 */
@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    /**
     * Find notifications for a specific user (paginated)
     */
    Page<Notification> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);

    /**
     * Find unread notifications for a specific user
     */
    List<Notification> findByUserIdAndReadFalseOrderByCreatedAtDesc(String userId);

    /**
     * Count unread notifications for a specific user
     */
    long countByUserIdAndReadFalse(String userId);

    /**
     * Delete old notifications (older than specified date)
     * Used for cleanup/maintenance
     */
    void deleteByCreatedAtBefore(java.time.LocalDateTime date);
}
