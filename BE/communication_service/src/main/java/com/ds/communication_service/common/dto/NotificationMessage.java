package com.ds.communication_service.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for notification messages
 * Sent via WebSocket to notify users of events
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationMessage {
    /**
     * Unique notification ID
     */
    private String id;
    
    /**
     * ID of the user who should receive the notification
     */
    private String userId;
    
    /**
     * Notification type
     */
    private NotificationType type;
    
    /**
     * Notification title
     */
    private String title;
    
    /**
     * Notification message/body
     */
    private String message;
    
    /**
     * Optional data payload (JSON string)
     */
    private String data;
    
    /**
     * Whether the notification has been read
     */
    private boolean read;
    
    /**
     * Timestamp when notification was created
     */
    private LocalDateTime createdAt;
    
    /**
     * Timestamp when notification was read (if applicable)
     */
    private LocalDateTime readAt;
    
    /**
     * Optional link/action for the notification
     */
    private String actionUrl;
    
    public enum NotificationType {
        /**
         * New message notification
         */
        NEW_MESSAGE,
        
        /**
         * New proposal notification
         */
        NEW_PROPOSAL,
        
        /**
         * Proposal status update
         */
        PROPOSAL_UPDATE,
        
        /**
         * Delivery status update
         */
        DELIVERY_UPDATE,
        
        /**
         * System notification
         */
        SYSTEM,
        
        /**
         * General information
         */
        INFO,
        
        /**
         * Warning notification
         */
        WARNING,
        
        /**
         * Error notification
         */
        ERROR
    }
}
