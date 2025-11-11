package com.ds.communication_service.business.v1.services;

import com.ds.communication_service.common.dto.TypingIndicator;
import com.ds.communication_service.infrastructure.kafka.EventProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Service for handling typing indicators
 * Broadcasts typing events to conversation participants
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TypingService {

    private final EventProducer eventProducer;

    /**
     * Handle typing indicator event
     * Publishes to Kafka for broadcasting to other users in conversation
     * 
     * @param conversationId Conversation ID
     * @param userId User ID who is typing
     * @param isTyping Whether user is currently typing (true) or stopped (false)
     */
    public void handleTypingEvent(String conversationId, String userId, boolean isTyping) {
        try {
            TypingIndicator typingIndicator = TypingIndicator.builder()
                .conversationId(conversationId)
                .userId(userId)
                .isTyping(isTyping)
                .timestamp(System.currentTimeMillis())
                .build();
            
            // Publish typing event to Kafka
            eventProducer.publishTypingEvent(conversationId, typingIndicator);
            
            log.debug("✅ Typing indicator published: conversationId={}, userId={}, isTyping={}", 
                conversationId, userId, isTyping);
            
        } catch (Exception e) {
            log.error("❌ Failed to handle typing event: conversationId={}, userId={}, error={}", 
                conversationId, userId, e.getMessage());
        }
    }

    /**
     * User started typing
     * 
     * @param conversationId Conversation ID
     * @param userId User ID who started typing
     */
    public void userStartedTyping(String conversationId, String userId) {
        handleTypingEvent(conversationId, userId, true);
    }

    /**
     * User stopped typing
     * 
     * @param conversationId Conversation ID
     * @param userId User ID who stopped typing
     */
    public void userStoppedTyping(String conversationId, String userId) {
        handleTypingEvent(conversationId, userId, false);
    }
}
