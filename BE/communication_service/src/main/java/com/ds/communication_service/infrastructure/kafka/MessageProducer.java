package com.ds.communication_service.infrastructure.kafka;

import com.ds.communication_service.common.dto.ChatMessagePayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * Kafka producer for chat messages
 * Publishes messages to chat-messages topic for guaranteed delivery
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MessageProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Publish chat message to Kafka for processing
     * 
     * @param conversationId Conversation ID (used as partition key for ordering)
     * @param message        Chat message payload
     * @return CompletableFuture for async handling
     */
    public CompletableFuture<SendResult<String, Object>> publishMessage(
            String conversationId, 
            ChatMessagePayload message) {
        
        log.info("📤 Publishing message to Kafka topic: {}, conversationId: {}", 
            KafkaConfig.TOPIC_CHAT_MESSAGES, conversationId);
        
        CompletableFuture<SendResult<String, Object>> future = 
            kafkaTemplate.send(KafkaConfig.TOPIC_CHAT_MESSAGES, conversationId, message);
        
        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("✅ Message published successfully to Kafka. Topic: {}, Partition: {}, Offset: {}",
                    result.getRecordMetadata().topic(),
                    result.getRecordMetadata().partition(),
                    result.getRecordMetadata().offset());
            } else {
                log.error("❌ Failed to publish message to Kafka: {}", ex.getMessage(), ex);
            }
        });
        
        return future;
    }

    /**
     * Publish message synchronously (blocks until complete)
     * Use for critical messages that must be confirmed
     * 
     * @param conversationId Conversation ID
     * @param message        Chat message payload
     * @return SendResult
     * @throws Exception if publishing fails
     */
    public SendResult<String, Object> publishMessageSync(
            String conversationId, 
            ChatMessagePayload message) throws Exception {
        
        log.info("📤 Publishing message to Kafka (sync) topic: {}, conversationId: {}", 
            KafkaConfig.TOPIC_CHAT_MESSAGES, conversationId);
        
        SendResult<String, Object> result = kafkaTemplate.send(
            KafkaConfig.TOPIC_CHAT_MESSAGES, conversationId, message).get();
        
        log.info("✅ Message published successfully (sync) to Kafka. Topic: {}, Partition: {}, Offset: {}",
            result.getRecordMetadata().topic(),
            result.getRecordMetadata().partition(),
            result.getRecordMetadata().offset());
        
        return result;
    }
}
