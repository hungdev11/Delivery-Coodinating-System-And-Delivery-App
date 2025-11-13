package com.ds.communication_service.common.enums;

/**
 * Message status for tracking message delivery
 * Flow: SENT → DELIVERED → READ
 */
public enum MessageStatus {
    /**
     * Message has been saved to database and sent to Kafka queue
     */
    SENT,
    
    /**
     * Message has been delivered to recipient via WebSocket
     */
    DELIVERED,
    
    /**
     * Message has been read/viewed by recipient
     */
    READ
}
