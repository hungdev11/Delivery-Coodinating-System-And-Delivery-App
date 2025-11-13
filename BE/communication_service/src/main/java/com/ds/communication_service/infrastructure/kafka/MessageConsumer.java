package com.ds.communication_service.infrastructure.kafka;

import com.ds.communication_service.common.dto.ChatMessagePayload;
import com.ds.communication_service.common.dto.MessageStatusUpdate;
import com.ds.communication_service.common.dto.NotificationMessage;
import com.ds.communication_service.common.dto.TypingIndicator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;

/**
 * Kafka consumer for processing messages and events
 * Consumes from all topics and handles WebSocket distribution
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MessageConsumer {

    private final SimpMessageSendingOperations messagingTemplate;

    /**
     * Consume chat messages from Kafka queue
     * Process and send via WebSocket to recipient
     */
    @KafkaListener(
        topics = KafkaConfig.TOPIC_CHAT_MESSAGES,
        groupId = "${spring.kafka.consumer.group-id:communication-service-group}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeChatMessage(
            @Payload ChatMessagePayload message,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {
        
        try {
            log.info("📥 Received message from Kafka. Topic: {}, Partition: {}, Offset: {}", 
                topic, partition, offset);
            
            // Process message and send via WebSocket
            // (This is handled by the existing MessageService/ChatController flow)
            log.info("✅ Chat message consumed from Kafka: conversationId={}", 
                message.getConversationId());
            
            // Acknowledge message processing
            if (acknowledgment != null) {
                acknowledgment.acknowledge();
            }
            
        } catch (Exception e) {
            log.error("❌ Error consuming chat message from Kafka: {}", e.getMessage(), e);
            // Don't acknowledge - message will be reprocessed
        }
    }

    /**
     * Consume message status updates from Kafka
     * Broadcast status changes via WebSocket to relevant users
     */
    @KafkaListener(
        topics = KafkaConfig.TOPIC_MESSAGE_STATUS,
        groupId = "${spring.kafka.consumer.group-id:communication-service-group}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeStatusUpdate(
            @Payload MessageStatusUpdate statusUpdate,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {
        
        try {
            log.debug("📥 Received status update from Kafka. Topic: {}, Partition: {}, Offset: {}", 
                topic, partition, offset);
            
            // Send status update to message sender via WebSocket
            messagingTemplate.convertAndSendToUser(
                statusUpdate.getUserId(),
                "/queue/status-updates",
                statusUpdate
            );
            
            log.debug("✅ Status update sent via WebSocket: status={}, messageId={}", 
                statusUpdate.getStatus(), statusUpdate.getMessageId());
            
            // Acknowledge message processing
            if (acknowledgment != null) {
                acknowledgment.acknowledge();
            }
            
        } catch (Exception e) {
            log.error("❌ Error consuming status update from Kafka: {}", e.getMessage(), e);
            // Don't acknowledge - message will be reprocessed
        }
    }

    /**
     * Consume typing events from Kafka
     * Broadcast typing indicators via WebSocket to conversation participants
     */
    @KafkaListener(
        topics = KafkaConfig.TOPIC_TYPING_EVENTS,
        groupId = "${spring.kafka.consumer.group-id:communication-service-group}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeTypingEvent(
            @Payload TypingIndicator typingIndicator,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {
        
        try {
            log.debug("📥 Received typing event from Kafka. Topic: {}, Partition: {}, Offset: {}", 
                topic, partition, offset);
            
            // Broadcast typing indicator to conversation topic
            String destination = "/topic/conversation/" + typingIndicator.getConversationId() + "/typing";
            messagingTemplate.convertAndSend(destination, typingIndicator);
            
            log.debug("✅ Typing indicator sent via WebSocket: conversationId={}, isTyping={}", 
                typingIndicator.getConversationId(), typingIndicator.isTyping());
            
            // Acknowledge message processing
            if (acknowledgment != null) {
                acknowledgment.acknowledge();
            }
            
        } catch (Exception e) {
            log.error("❌ Error consuming typing event from Kafka: {}", e.getMessage(), e);
            // Don't acknowledge - message will be reprocessed
        }
    }

    /**
     * Consume notifications from Kafka
     * Send notifications via WebSocket to target users
     */
    @KafkaListener(
        topics = KafkaConfig.TOPIC_NOTIFICATIONS,
        groupId = "${spring.kafka.consumer.group-id:communication-service-group}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeNotification(
            @Payload NotificationMessage notification,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {
        
        try {
            log.info("📥 Received notification from Kafka. Topic: {}, Partition: {}, Offset: {}", 
                topic, partition, offset);
            
            // Send notification to user via WebSocket
            messagingTemplate.convertAndSendToUser(
                notification.getUserId(),
                "/queue/notifications",
                notification
            );
            
            log.info("✅ Notification sent via WebSocket: type={}, userId={}", 
                notification.getType(), notification.getUserId());
            
            // Acknowledge message processing
            if (acknowledgment != null) {
                acknowledgment.acknowledge();
            }
            
        } catch (Exception e) {
            log.error("❌ Error consuming notification from Kafka: {}", e.getMessage(), e);
            // Don't acknowledge - message will be reprocessed
        }
    }
}
