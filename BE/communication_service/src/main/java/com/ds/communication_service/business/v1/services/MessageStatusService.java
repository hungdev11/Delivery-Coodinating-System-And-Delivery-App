package com.ds.communication_service.business.v1.services;

import com.ds.communication_service.common.dto.MessageStatusUpdate;
import com.ds.communication_service.common.enums.MessageStatus;
import com.ds.communication_service.infrastructure.kafka.EventProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Service for handling message status updates
 * Manages status lifecycle: SENT → DELIVERED → READ
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MessageStatusService {

    private final MessageService messageService;
    private final EventProducer eventProducer;

    /**
     * Mark message as delivered
     * Called when recipient receives the message via WebSocket
     * 
     * @param messageId Message ID
     * @param conversationId Conversation ID
     * @param userId User ID who received the message
     */
    public void markAsDelivered(String messageId, String conversationId, String userId) {
        try {
            UUID msgId = UUID.fromString(messageId);
            
            // Update message status in database
            messageService.updateMessageStatus(msgId, MessageStatus.DELIVERED);
            
            // Create status update event
            MessageStatusUpdate statusUpdate = MessageStatusUpdate.builder()
                .messageId(messageId)
                .conversationId(conversationId)
                .status(MessageStatus.DELIVERED)
                .userId(userId)
                .timestamp(LocalDateTime.now())
                .build();
            
            // Publish status update event to Kafka
            eventProducer.publishStatusUpdate(userId, statusUpdate);
            
            log.info("✅ Message marked as DELIVERED: messageId={}, userId={}", messageId, userId);
            
        } catch (Exception e) {
            log.error("❌ Failed to mark message as delivered: messageId={}, error={}", 
                messageId, e.getMessage(), e);
        }
    }

    /**
     * Mark message as read
     * Called when recipient views/opens the message
     * 
     * @param messageId Message ID
     * @param conversationId Conversation ID
     * @param userId User ID who read the message
     */
    public void markAsRead(String messageId, String conversationId, String userId) {
        try {
            UUID msgId = UUID.fromString(messageId);
            
            // Update message status in database
            messageService.updateMessageStatus(msgId, MessageStatus.READ);
            
            // Create status update event
            MessageStatusUpdate statusUpdate = MessageStatusUpdate.builder()
                .messageId(messageId)
                .conversationId(conversationId)
                .status(MessageStatus.READ)
                .userId(userId)
                .timestamp(LocalDateTime.now())
                .build();
            
            // Publish status update event to Kafka
            eventProducer.publishStatusUpdate(userId, statusUpdate);
            
            log.info("✅ Message marked as READ: messageId={}, userId={}", messageId, userId);
            
        } catch (Exception e) {
            log.error("❌ Failed to mark message as read: messageId={}, error={}", 
                messageId, e.getMessage(), e);
        }
    }

    /**
     * Mark multiple messages as read
     * Useful when user opens a conversation and views multiple unread messages
     * 
     * @param messageIds Array of message IDs
     * @param conversationId Conversation ID
     * @param userId User ID who read the messages
     */
    public void markMultipleAsRead(String[] messageIds, String conversationId, String userId) {
        log.info("Marking {} messages as read for userId={}", messageIds.length, userId);
        
        for (String messageId : messageIds) {
            markAsRead(messageId, conversationId, userId);
        }
    }
}
