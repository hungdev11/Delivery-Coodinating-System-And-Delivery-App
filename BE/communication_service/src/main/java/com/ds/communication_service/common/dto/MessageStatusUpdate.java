package com.ds.communication_service.common.dto;

import com.ds.communication_service.common.enums.MessageStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for message status updates
 * Sent via WebSocket to notify sender when recipient receives/reads message
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageStatusUpdate {
    /**
     * ID of the message being updated
     */
    private String messageId;
    
    /**
     * ID of the conversation
     */
    private String conversationId;
    
    /**
     * New status of the message
     */
    private MessageStatus status;
    
    /**
     * ID of the user who performed the action (read/received)
     */
    private String userId;
    
    /**
     * Timestamp when the status changed
     */
    private LocalDateTime timestamp;
}
