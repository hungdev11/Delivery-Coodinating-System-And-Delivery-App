package com.ds.communication_service.infrastructure.kafka;

import com.ds.communication_service.common.dto.MessageStatusUpdate;
import com.ds.communication_service.common.dto.NotificationMessage;
import com.ds.communication_service.common.dto.TypingIndicator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * Kafka producer for events (status updates, typing, notifications)
 * Publishes events to various topics for event streaming
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Publish message status update event
     * 
     * @param userId User ID (used as partition key)
     * @param statusUpdate Status update payload
     * @return CompletableFuture for async handling
     */
    public CompletableFuture<SendResult<String, Object>> publishStatusUpdate(
            String userId, 
            MessageStatusUpdate statusUpdate) {
        
        log.debug("📤 Publishing status update to Kafka topic: {}, userId: {}", 
            KafkaConfig.TOPIC_MESSAGE_STATUS, userId);
        
        CompletableFuture<SendResult<String, Object>> future = 
            kafkaTemplate.send(KafkaConfig.TOPIC_MESSAGE_STATUS, userId, statusUpdate);
        
        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.debug("✅ Status update published successfully to Kafka. Status: {}", 
                    statusUpdate.getStatus());
            } else {
                log.error("❌ Failed to publish status update to Kafka: {}", ex.getMessage());
            }
        });
        
        return future;
    }

    /**
     * Publish typing indicator event
     * 
     * @param conversationId Conversation ID (used as partition key)
     * @param typingIndicator Typing indicator payload
     * @return CompletableFuture for async handling
     */
    public CompletableFuture<SendResult<String, Object>> publishTypingEvent(
            String conversationId, 
            TypingIndicator typingIndicator) {
        
        log.debug("📤 Publishing typing event to Kafka topic: {}, conversationId: {}", 
            KafkaConfig.TOPIC_TYPING_EVENTS, conversationId);
        
        CompletableFuture<SendResult<String, Object>> future = 
            kafkaTemplate.send(KafkaConfig.TOPIC_TYPING_EVENTS, conversationId, typingIndicator);
        
        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.debug("✅ Typing event published successfully to Kafka");
            } else {
                log.error("❌ Failed to publish typing event to Kafka: {}", ex.getMessage());
            }
        });
        
        return future;
    }

    /**
     * Publish notification event
     * 
     * @param userId User ID (used as partition key)
     * @param notification Notification payload
     * @return CompletableFuture for async handling
     */
    public CompletableFuture<SendResult<String, Object>> publishNotification(
            String userId, 
            NotificationMessage notification) {
        
        log.info("📤 Publishing notification to Kafka topic: {}, userId: {}", 
            KafkaConfig.TOPIC_NOTIFICATIONS, userId);
        
        CompletableFuture<SendResult<String, Object>> future = 
            kafkaTemplate.send(KafkaConfig.TOPIC_NOTIFICATIONS, userId, notification);
        
        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("✅ Notification published successfully to Kafka. Type: {}", 
                    notification.getType());
            } else {
                log.error("❌ Failed to publish notification to Kafka: {}", ex.getMessage(), ex);
            }
        });
        
        return future;
    }

    /**
     * Publish notification synchronously (blocks until complete)
     * Use for critical notifications that must be confirmed
     * 
     * @param userId User ID
     * @param notification Notification payload
     * @return SendResult
     * @throws Exception if publishing fails
     */
    public SendResult<String, Object> publishNotificationSync(
            String userId, 
            NotificationMessage notification) throws Exception {
        
        log.info("📤 Publishing notification to Kafka (sync) topic: {}, userId: {}", 
            KafkaConfig.TOPIC_NOTIFICATIONS, userId);
        
        SendResult<String, Object> result = kafkaTemplate.send(
            KafkaConfig.TOPIC_NOTIFICATIONS, userId, notification).get();
        
        log.info("✅ Notification published successfully (sync) to Kafka. Type: {}",
            notification.getType());
        
        return result;
    }
}
