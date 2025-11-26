package com.ds.communication_service.application.controller;

import com.ds.communication_service.business.v1.services.NotificationService;
import com.ds.communication_service.common.dto.BaseResponse;
import com.ds.communication_service.common.dto.NotificationMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST API controller for managing notifications
 * Provides endpoints for fetching, marking as read, and deleting notifications
 */
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Slf4j
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * Get notifications for current user (paginated)
     * Frontend should provide userId in header (from JWT)
     */
    @GetMapping
    public ResponseEntity<BaseResponse<Page<NotificationMessage>>> getNotifications(
            @RequestHeader("X-User-Id") String userId,
            Pageable pageable) {

        log.debug("Get notifications for userId={}", userId);
        Page<NotificationMessage> notifications = notificationService.getNotificationsForUser(userId, pageable);

        return ResponseEntity.ok(BaseResponse.success(notifications));
    }

    /**
     * Get unread notifications for current user
     */
    @GetMapping("/unread")
    public ResponseEntity<BaseResponse<List<NotificationMessage>>> getUnreadNotifications(
            @RequestHeader("X-User-Id") String userId) {

        log.debug("Get unread notifications for userId={}", userId);
        List<NotificationMessage> notifications = notificationService.getUnreadNotifications(userId);

        return ResponseEntity.ok(BaseResponse.success(notifications));
    }

    /**
     * Get unread notification count
     */
    @GetMapping("/unread/count")
    public ResponseEntity<BaseResponse<Long>> getUnreadCount(
            @RequestHeader("X-User-Id") String userId) {

        long count = notificationService.getUnreadCount(userId);

        return ResponseEntity.ok(BaseResponse.success(count));
    }

    /**
     * Mark notification as read
     */
    @PutMapping("/{notificationId}/read")
    public ResponseEntity<BaseResponse<NotificationMessage>> markAsRead(
            @PathVariable String notificationId,
            @RequestHeader("X-User-Id") String userId) {

        log.debug("Mark notification as read: notificationId={}, userId={}", notificationId, userId);
        NotificationMessage notification = notificationService.markAsRead(notificationId, userId);

        return ResponseEntity.ok(BaseResponse.success(notification));
    }

    /**
     * Mark all notifications as read
     */
    @PutMapping("/read-all")
    public ResponseEntity<BaseResponse<Integer>> markAllAsRead(
            @RequestHeader("X-User-Id") String userId) {

        log.debug("Mark all notifications as read for userId={}", userId);
        int count = notificationService.markAllAsRead(userId);

        return ResponseEntity.ok(BaseResponse.success(count));
    }

    /**
     * Delete notification
     */
    @DeleteMapping("/{notificationId}")
    public ResponseEntity<BaseResponse<Void>> deleteNotification(
            @PathVariable String notificationId,
            @RequestHeader("X-User-Id") String userId) {

        log.debug("Delete notification: notificationId={}, userId={}", notificationId, userId);
        notificationService.deleteNotification(notificationId, userId);

        return ResponseEntity.ok(BaseResponse.success(null));
    }
}
