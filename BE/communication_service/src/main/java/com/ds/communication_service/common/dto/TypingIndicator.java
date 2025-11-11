package com.ds.communication_service.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for typing indicator events
 * Sent via WebSocket to notify when user is typing
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TypingIndicator {
    /**
     * ID of the conversation
     */
    private String conversationId;
    
    /**
     * ID of the user who is typing
     */
    private String userId;
    
    /**
     * Whether the user is currently typing
     */
    private boolean isTyping;
    
    /**
     * Timestamp of the typing event
     */
    private long timestamp;
}
