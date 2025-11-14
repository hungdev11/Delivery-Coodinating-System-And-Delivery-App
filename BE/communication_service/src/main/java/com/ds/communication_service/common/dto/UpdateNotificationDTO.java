package com.ds.communication_service.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * DTO for update notifications from other services
 * Sent via Kafka and forwarded to Android/Web clients via WebSocket
 * Used to notify clients when data needs to be refreshed (e.g., session ended, parcel status changed)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateNotificationDTO {
    
    /**
     * Unique notification ID
     */
    private String id;
    
    /**
     * ID of the user who should receive the update notification
     * Can be a single userId or multiple userIds (comma-separated) for broadcast
     */
    private String userId;
    
    /**
     * Type of update (SESSION_UPDATE, PARCEL_UPDATE, etc.)
     */
    private UpdateType updateType;
    
    /**
     * Type of entity being updated (SESSION, PARCEL, ASSIGNMENT, etc.)
     */
    private EntityType entityType;
    
    /**
     * ID of the entity being updated
     */
    private String entityId;
    
    /**
     * Action performed (CREATED, UPDATED, DELETED, STATUS_CHANGED, etc.)
     */
    private ActionType action;
    
    /**
     * Optional additional data (JSON string or Map)
     * Contains relevant information about the update (e.g., new status, changed fields)
     */
    private Map<String, Object> data;
    
    /**
     * Timestamp when the update occurred
     */
    private LocalDateTime timestamp;
    
    /**
     * Optional message/description
     */
    private String message;
    
    /**
     * Client type filter (ANDROID, WEB, ALL)
     * If specified, only send to clients of that type
     */
    private ClientType clientType;
    
    /**
     * Update type enum
     */
    public enum UpdateType {
        /**
         * Session-related updates (session created, started, completed, failed)
         */
        SESSION_UPDATE,
        
        /**
         * Parcel-related updates (parcel status changed, assigned, etc.)
         */
        PARCEL_UPDATE,
        
        /**
         * Assignment/task-related updates (task completed, failed, delayed)
         */
        ASSIGNMENT_UPDATE,
        
        /**
         * User-related updates (user profile changed, status changed)
         */
        USER_UPDATE,
        
        /**
         * Delivery-related updates (delivery started, completed, failed)
         */
        DELIVERY_UPDATE,
        
        /**
         * General data update
         */
        DATA_UPDATE
    }
    
    /**
     * Entity type enum
     */
    public enum EntityType {
        SESSION,
        PARCEL,
        ASSIGNMENT,
        USER,
        DELIVERY,
        CONVERSATION,
        MESSAGE
    }
    
    /**
     * Action type enum
     */
    public enum ActionType {
        CREATED,
        UPDATED,
        DELETED,
        STATUS_CHANGED,
        ASSIGNED,
        COMPLETED,
        FAILED,
        CANCELLED
    }
    
    /**
     * Client type enum
     */
    public enum ClientType {
        ANDROID,
        WEB,
        ALL
    }
}
