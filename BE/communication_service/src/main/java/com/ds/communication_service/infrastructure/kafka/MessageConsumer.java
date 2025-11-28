package com.ds.communication_service.infrastructure.kafka;

import com.ds.communication_service.business.v1.services.WebSocketSessionManager;
import com.ds.communication_service.common.dto.ChatMessagePayload;
import com.ds.communication_service.common.dto.MessageStatusUpdate;
import com.ds.communication_service.common.dto.NotificationMessage;
import com.ds.communication_service.common.dto.TypingIndicator;
import com.ds.communication_service.common.dto.UpdateNotificationDTO;
import com.ds.communication_service.business.v1.services.AssignmentNotificationService;
import com.ds.communication_service.business.v1.services.PostponeNotificationService;
import com.ds.communication_service.business.v1.services.SessionCompletionNotificationService;
import com.ds.communication_service.business.v1.services.ParcelSucceededNotificationService;
import com.ds.communication_service.infrastructure.kafka.dto.AssignmentCompletedEvent;
import com.ds.communication_service.infrastructure.kafka.dto.ParcelPostponedEvent;
import com.ds.communication_service.infrastructure.kafka.dto.SessionCompletedEvent;
import com.ds.communication_service.app_context.repositories.ConversationRepository;
import com.ds.communication_service.app_context.models.Conversation;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;

import org.apache.kafka.clients.consumer.ConsumerRecord;
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
    private final WebSocketSessionManager sessionManager;
    private final AssignmentNotificationService assignmentNotificationService;
    private final PostponeNotificationService postponeNotificationService;
    private final SessionCompletionNotificationService sessionCompletionNotificationService;
    private final ParcelSucceededNotificationService parcelSucceededNotificationService;
    private final ConversationRepository conversationRepository;
    private final ObjectMapper objectMapper;

    /**
     * Consume chat messages from Kafka queue
     * Process and send via WebSocket to recipient
     * Note: This consumer may receive different message types (ChatMessagePayload, UserEventDto, etc.)
     * due to topic reuse or legacy messages. We handle this gracefully.
     */
    @KafkaListener(
        topics = KafkaConfig.TOPIC_CHAT_MESSAGES,
        groupId = "${spring.kafka.consumer.group-id:communication-service-group}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeChatMessage(
            @Payload Object payload,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {
        
        try {
            log.debug("[communication-service] [MessageConsumer.consumeStatusUpdate] Received message from Kafka. Topic: {}, Partition: {}, Offset: {}", 
                topic, partition, offset);
            
            // Convert payload to ChatMessagePayload
            ChatMessagePayload message;
            if (payload instanceof ChatMessagePayload) {
                message = (ChatMessagePayload) payload;
            } else if (payload instanceof java.util.Map) {
                // Deserializer returned a Map, convert to DTO
                try {
                    message = objectMapper.convertValue(payload, ChatMessagePayload.class);
                } catch (Exception e) {
                    log.debug("[communication-service] [MessageConsumer.consumeChatMessage] Received unexpected message type in chat-messages topic: {}. Expected ChatMessagePayload. Skipping... Error: {}", 
                        payload != null ? payload.getClass().getName() : "null", e.getMessage());
                    // Acknowledge to skip this message and avoid reprocessing
                    if (acknowledgment != null) {
                        acknowledgment.acknowledge();
                    }
                    return;
                }
            } else {
                log.debug("[communication-service] [MessageConsumer.consumeChatMessage] Received unexpected message type in chat-messages topic: {}. Expected ChatMessagePayload. Skipping...", 
                    payload != null ? payload.getClass().getName() : "null");
                // Acknowledge to skip this message and avoid reprocessing
                if (acknowledgment != null) {
                    acknowledgment.acknowledge();
                }
                return;
            }
            
            // Process message and send via WebSocket
            // (This is handled by the existing MessageService/ChatController flow)
            log.debug("[communication-service] [MessageConsumer.consumeChatMessage] Chat message consumed from Kafka: conversationId={}", 
                message.getConversationId());
            
            // Acknowledge message processing
            if (acknowledgment != null) {
                acknowledgment.acknowledge();
            }
            
        } catch (Exception e) {
            log.error("[communication-service] [MessageConsumer.consumeChatMessage] Error consuming chat message from Kafka", e);
            // Acknowledge even on error to prevent infinite retries for malformed messages
            if (acknowledgment != null) {
                acknowledgment.acknowledge();
            }
        }
    }

    /**
     * Consume message status updates from Kafka
     * Broadcast status changes via WebSocket to relevant users
     * Note: This consumer expects MessageStatusUpdate, but may receive other types due to topic misconfiguration
     */
    @KafkaListener(
        topics = KafkaConfig.TOPIC_MESSAGE_STATUS,
        groupId = "${spring.kafka.consumer.group-id:communication-service-group}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeStatusUpdate(
            @Payload Object payload,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {
        
        try {
            log.debug("[communication-service] [MessageConsumer.consumeStatusUpdate] Received message from Kafka topic: {}, Partition: {}, Offset: {}", 
                topic, partition, offset);
            
            // Check if payload is MessageStatusUpdate
            if (!(payload instanceof MessageStatusUpdate)) {
                log.debug("[communication-service] [MessageConsumer.consumeStatusUpdate] Received unexpected message type in message-status-events topic: {}. Expected MessageStatusUpdate. Skipping...", 
                    payload != null ? payload.getClass().getName() : "null");
                // Acknowledge to skip this message and avoid reprocessing
                if (acknowledgment != null) {
                    acknowledgment.acknowledge();
                }
                return;
            }
            
            MessageStatusUpdate statusUpdate = (MessageStatusUpdate) payload;
            
            // Validate userId before sending
            String userId = statusUpdate.getUserId();
            if (userId == null || userId.isBlank()) {
                log.debug("[communication-service] [MessageConsumer.consumeStatusUpdate] MessageStatusUpdate has no userId. Skipping.");
                if (acknowledgment != null) {
                    acknowledgment.acknowledge();
                }
                return;
            }
            
            // Send status update to message sender via WebSocket
            messagingTemplate.convertAndSendToUser(
                userId,
                "/queue/status-updates",
                statusUpdate
            );
            
            log.debug("[communication-service] [MessageConsumer.consumeStatusUpdate] Status update sent via WebSocket: status={}, messageId={}", 
                statusUpdate.getStatus(), statusUpdate.getMessageId());
            
            // Acknowledge message processing
            if (acknowledgment != null) {
                acknowledgment.acknowledge();
            }
            
        } catch (Exception e) {
            log.error("❌ Error consuming status update from Kafka: {}", e.getMessage(), e);
            // Acknowledge to avoid infinite retry loop for malformed messages
            if (acknowledgment != null) {
                acknowledgment.acknowledge();
            }
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
            @Payload Object payload,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {
        
        try {
            log.debug("[communication-service] [MessageConsumer.consumeTypingEvent] Received typing event from Kafka. Topic: {}, Partition: {}, Offset: {}", 
                topic, partition, offset);
            
            // Convert payload to TypingIndicator (handle both object and Map cases)
            TypingIndicator typingIndicator;
            if (payload instanceof TypingIndicator) {
                typingIndicator = (TypingIndicator) payload;
            } else if (payload instanceof java.util.Map) {
                try {
                    typingIndicator = objectMapper.convertValue(payload, TypingIndicator.class);
                } catch (Exception e) {
                    log.error("[communication-service] [MessageConsumer.consumeTypingEvent] Failed to convert payload to TypingIndicator", e);
                    if (acknowledgment != null) {
                        acknowledgment.acknowledge();
                    }
                    return;
                }
            } else {
                log.error("[communication-service] [MessageConsumer.consumeTypingEvent] Received unexpected message type in typing-events topic: {}. Expected TypingIndicator or Map.",
                    payload != null ? payload.getClass().getName() : "null");
                if (acknowledgment != null) {
                    acknowledgment.acknowledge();
                }
                return;
            }
            
            // Validate required fields
            if (typingIndicator.getConversationId() == null || typingIndicator.getUserId() == null) {
                log.warn("[communication-service] [MessageConsumer.consumeTypingEvent] TypingIndicator missing required fields. Skipping.");
                if (acknowledgment != null) {
                    acknowledgment.acknowledge();
                }
                return;
            }
            
            String senderUserId = typingIndicator.getUserId();
            String conversationId = typingIndicator.getConversationId();
            
            // Find conversation to get the other participant
            try {
                @SuppressWarnings("null")
                java.util.UUID convId = java.util.UUID.fromString(conversationId);
                Conversation conversation = conversationRepository.findById(convId).orElse(null);
                
                if (conversation == null) {
                    log.warn("[communication-service] [MessageConsumer.consumeTypingEvent] Conversation not found: {}. Skipping.", conversationId);
                    if (acknowledgment != null) {
                        acknowledgment.acknowledge();
                    }
                    return;
                }
                
                // Get the other user in the conversation (partner, not the sender)
                String partnerUserId = conversation.getUser1Id().equals(senderUserId) 
                    ? conversation.getUser2Id() 
                    : conversation.getUser1Id();
                
                if (partnerUserId == null || partnerUserId.isBlank()) {
                    log.warn("[communication-service] [MessageConsumer.consumeTypingEvent] Partner userId is null or blank. Skipping.");
                    if (acknowledgment != null) {
                        acknowledgment.acknowledge();
                    }
                    return;
                }
                
                // Create typing indicator message for WebSocket
                java.util.Map<String, Object> typingMessage = new java.util.HashMap<>();
                typingMessage.put("userId", senderUserId);
                typingMessage.put("conversationId", conversationId);
                typingMessage.put("isTyping", typingIndicator.isTyping());
                typingMessage.put("timestamp", typingIndicator.getTimestamp() != 0 ? typingIndicator.getTimestamp() : System.currentTimeMillis());
                
                // Send typing indicator to the partner via their personal queue
                // Frontend subscribes to /user/queue/typing
                messagingTemplate.convertAndSendToUser(partnerUserId, "/queue/typing", typingMessage);
                
                log.debug("[communication-service] [MessageConsumer.consumeTypingEvent] Typing indicator sent via WebSocket: conversationId={}, sender={}, partner={}, isTyping={}", 
                    conversationId, senderUserId, partnerUserId, typingIndicator.isTyping());
                
                // Acknowledge message processing
                if (acknowledgment != null) {
                    acknowledgment.acknowledge();
                }
                    
            } catch (java.lang.IllegalArgumentException e) {
                log.error("[communication-service] [MessageConsumer.consumeTypingEvent] Invalid conversationId format: {}", conversationId, e);
                if (acknowledgment != null) {
                    acknowledgment.acknowledge();
                }
                return;
            } catch (Exception e) {
                log.error("[communication-service] [MessageConsumer.consumeTypingEvent] Error finding conversation or sending typing indicator: conversationId={}", 
                    conversationId, e);
                // Acknowledge to avoid infinite retry loop
                if (acknowledgment != null) {
                    acknowledgment.acknowledge();
                }
                return;
            }
            if (acknowledgment != null) {
                acknowledgment.acknowledge();
            }
            
        } catch (Exception e) {
            log.error("[communication-service] [MessageConsumer.consumeTypingEvent] Error consuming typing event from Kafka", e);
            // Acknowledge to avoid infinite retry loop for malformed messages
            if (acknowledgment != null) {
                acknowledgment.acknowledge();
            }
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
            log.debug("[communication-service] [MessageConsumer.consumeNotification] Received notification from Kafka. Topic: {}, Partition: {}, Offset: {}", 
                topic, partition, offset);
            
            // Validate userId before sending
            String userId = notification.getUserId();
            if (userId == null || userId.isBlank()) {
                log.debug("[communication-service] [MessageConsumer.consumeNotification] NotificationMessage has no userId. Skipping.");
                if (acknowledgment != null) {
                    acknowledgment.acknowledge();
                }
                return;
            }
            
            // Send notification to user via WebSocket
            messagingTemplate.convertAndSendToUser(
                userId,
                "/queue/notifications",
                notification
            );
            
            log.debug("[communication-service] [MessageConsumer.consumeNotification] Notification sent via WebSocket: type={}, userId={}", 
                notification.getType(), userId);
            
            // Acknowledge message processing
            if (acknowledgment != null) {
                acknowledgment.acknowledge();
            }
            
        } catch (Exception e) {
            log.error("[communication-service] [MessageConsumer.consumeNotification] Error consuming notification from Kafka", e);
            // Don't acknowledge - message will be reprocessed
        }
    }

    /**
     * Consume update notifications from Kafka
     * Other services (session-service, parcel-service, etc.) publish updates to this topic
     * Communication service forwards to clients via WebSocket
     * Supports filtering by client type (ANDROID, WEB, ALL)
     */
    @KafkaListener(
        topics = KafkaConfig.TOPIC_UPDATE_NOTIFICATIONS,
        groupId = "${spring.kafka.consumer.group-id:communication-service-group}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeUpdateNotification(
            @Payload Object payload,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {
        
        try {
            log.debug("[communication-service] [MessageConsumer.consumeUpdateNotification] Received update notification from Kafka. Topic: {}, Partition: {}, Offset: {}", 
                topic, partition, offset);
            
            // Convert payload to UpdateNotificationDTO
            UpdateNotificationDTO updateNotification;
            if (payload instanceof UpdateNotificationDTO) {
                updateNotification = (UpdateNotificationDTO) payload;
            } else if (payload instanceof java.util.Map) {
                // Deserializer returned a Map, convert to DTO
                try {
                    updateNotification = objectMapper.convertValue(payload, UpdateNotificationDTO.class);
                } catch (Exception e) {
                    log.error("[communication-service] [MessageConsumer.consumeUpdateNotification] Failed to convert payload to UpdateNotificationDTO", e);
                    // Acknowledge to skip this message and avoid reprocessing
                    if (acknowledgment != null) {
                        acknowledgment.acknowledge();
                    }
                    return;
                }
            } else {
                log.error("[communication-service] [MessageConsumer.consumeUpdateNotification] Received unexpected message type in update-notifications topic: {}. Expected UpdateNotificationDTO.", 
                    payload != null ? payload.getClass().getName() : "null");
                // Acknowledge to skip this message and avoid reprocessing
                if (acknowledgment != null) {
                    acknowledgment.acknowledge();
                }
                return;
            }
            
            log.debug("[communication-service] [MessageConsumer.consumeUpdateNotification] Update notification: type={}, entityType={}, entityId={}, action={}, userId={}, clientType={}", 
                updateNotification.getUpdateType(), 
                updateNotification.getEntityType(), 
                updateNotification.getEntityId(), 
                updateNotification.getAction(), 
                updateNotification.getUserId(),
                updateNotification.getClientType());
            
            // Check if this is a parcel status changed to SUCCEEDED event
            if (updateNotification.getUpdateType() == UpdateNotificationDTO.UpdateType.PARCEL_UPDATE
                    && updateNotification.getEntityType() == UpdateNotificationDTO.EntityType.PARCEL
                    && updateNotification.getAction() == UpdateNotificationDTO.ActionType.STATUS_CHANGED
                    && updateNotification.getData() != null) {
                
                Object statusObj = updateNotification.getData().get("status");
                String status = statusObj != null ? statusObj.toString() : null;
                
                if ("SUCCEEDED".equals(status) || "SUCCEED".equals(status)) {
                    // Handle parcel succeeded event
                    try {
                        String parcelId = updateNotification.getEntityId();
                        String parcelCode = updateNotification.getData().get("parcelCode") != null 
                                ? updateNotification.getData().get("parcelCode").toString() 
                                : null;
                        String receiverId = updateNotification.getData().get("receiverId") != null 
                                ? updateNotification.getData().get("receiverId").toString() 
                                : null;
                        String senderId = updateNotification.getData().get("senderId") != null 
                                ? updateNotification.getData().get("senderId").toString() 
                                : null;
                        String deliveryManId = updateNotification.getData().get("deliveryManId") != null 
                                ? updateNotification.getData().get("deliveryManId").toString() 
                                : null;
                        String confirmedBy = updateNotification.getData().get("confirmedBy") != null 
                                ? updateNotification.getData().get("confirmedBy").toString() 
                                : null;
                        
                        LocalDateTime confirmedAt = null;
                        if (updateNotification.getData().get("confirmedAt") != null) {
                            try {
                                String confirmedAtStr = updateNotification.getData().get("confirmedAt").toString();
                                confirmedAt = LocalDateTime.parse(confirmedAtStr);
                            } catch (Exception e) {
                                log.debug("[communication-service] [MessageConsumer.consumeUpdateNotification] Failed to parse confirmedAt", e);
                            }
                        }
                        
                        LocalDateTime succeededAt = updateNotification.getTimestamp() != null 
                                ? updateNotification.getTimestamp() 
                                : LocalDateTime.now();
                        
                        // Create message in chat
                        parcelSucceededNotificationService.handleParcelSucceeded(
                                parcelId,
                                parcelCode,
                                receiverId,
                                senderId,
                                deliveryManId,
                                confirmedBy,
                                confirmedAt,
                                succeededAt
                        );
                        
                        log.debug("[communication-service] [MessageConsumer.consumeUpdateNotification] Handled parcel succeeded event: parcelId={}, confirmedBy={}", 
                                parcelId, confirmedBy);
                    } catch (Exception e) {
                        log.error("[communication-service] [MessageConsumer.consumeUpdateNotification] Error handling parcel succeeded event", e);
                        // Continue with normal update notification forwarding
                    }
                }
            }
            
            // Handle multiple userIds (comma-separated) for broadcast
            String userIds = updateNotification.getUserId();
            if (userIds == null || userIds.isBlank()) {
                log.debug("[communication-service] [MessageConsumer.consumeUpdateNotification] Update notification has no userId. Skipping.");
                if (acknowledgment != null) {
                    acknowledgment.acknowledge();
                }
                return;
            }
            
            // Split userIds if multiple (comma-separated)
            String[] userIdArray = userIds.split(",");
            
            // Get client type filter (default to ALL if not specified)
            UpdateNotificationDTO.ClientType clientTypeFilter = updateNotification.getClientType();
            if (clientTypeFilter == null) {
                clientTypeFilter = UpdateNotificationDTO.ClientType.ALL;
            }
            
            // Send update notification to each user via WebSocket
            for (String userId : userIdArray) {
                userId = userId.trim();
                if (userId.isBlank()) {
                    continue;
                }
                
                try {
                    // Check if user is connected and has the required client type
                    if (clientTypeFilter != UpdateNotificationDTO.ClientType.ALL) {
                        if (!sessionManager.hasClientType(userId, clientTypeFilter)) {
                            log.debug("[communication-service] [MessageConsumer.consumeUpdateNotification] Skipping update notification for user {}: required clientType={}, user has={}", 
                                userId, clientTypeFilter, sessionManager.getClientTypes(userId));
                            continue;
                        }
                    }
                    
                    // Destination: /user/{userId}/queue/updates
                    String destination = "/queue/updates";
                    messagingTemplate.convertAndSendToUser(userId, destination, updateNotification);
                    
                    log.debug("[communication-service] [MessageConsumer.consumeUpdateNotification] Update notification sent via WebSocket: userId={}, destination={}, type={}, entityId={}, clientType={}", 
                        userId, destination, updateNotification.getUpdateType(), updateNotification.getEntityId(), clientTypeFilter);
                    
                } catch (Exception e) {
                    log.error("[communication-service] [MessageConsumer.consumeUpdateNotification] Error sending update notification to user {}", userId, e);
                    // Continue with other users even if one fails
                }
            }
            
            // Acknowledge message processing
            if (acknowledgment != null) {
                acknowledgment.acknowledge();
            }
            
        } catch (Exception e) {
            log.error("[communication-service] [MessageConsumer.consumeUpdateNotification] Error consuming update notification from Kafka", e);
            // Don't acknowledge - message will be reprocessed
        }
    }
    
    /**
     * Consume assignment completed events from session-service
     * Creates notifications for shipper and messages for client
     */
    @KafkaListener(
        topics = KafkaConfig.TOPIC_ASSIGNMENT_COMPLETED,
        groupId = "${spring.kafka.consumer.group-id:communication-service-group}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeAssignmentCompleted(
            @Payload Object payload,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {
        
        try {
            log.debug("[communication-service] [MessageConsumer.consumeAssignmentCompleted] Received assignment completed event from Kafka. Topic: {}, Partition: {}, Offset: {}", 
                topic, partition, offset);
            
            // Convert payload to AssignmentCompletedEvent
            AssignmentCompletedEvent event;
            if (payload instanceof AssignmentCompletedEvent) {
                event = (AssignmentCompletedEvent) payload;
            } else if (payload instanceof java.util.Map) {
                // Deserializer returned a Map, convert to DTO
                try {
                    event = objectMapper.convertValue(payload, AssignmentCompletedEvent.class);
                } catch (Exception e) {
                    log.error("[communication-service] [MessageConsumer.consumeAssignmentCompleted] Failed to convert payload to AssignmentCompletedEvent", e);
                    // Acknowledge to skip this message and avoid reprocessing
                    if (acknowledgment != null) {
                        acknowledgment.acknowledge();
                    }
                    return;
                }
            } else if (payload instanceof ConsumerRecord) {
                // Deserializer failed, received raw ConsumerRecord - extract value
                @SuppressWarnings("unchecked")
                ConsumerRecord<String, Object> record = (ConsumerRecord<String, Object>) payload;
                Object recordValue = record.value();
                if (recordValue instanceof java.util.Map) {
                    try {
                        event = objectMapper.convertValue(recordValue, AssignmentCompletedEvent.class);
                        log.debug("[communication-service] [MessageConsumer.consumeAssignmentCompleted] Extracted AssignmentCompletedEvent from ConsumerRecord");
                    } catch (Exception e) {
                        log.error("[communication-service] [MessageConsumer.consumeAssignmentCompleted] Failed to convert ConsumerRecord value to AssignmentCompletedEvent", e);
                        if (acknowledgment != null) {
                            acknowledgment.acknowledge();
                        }
                        return;
                    }
                } else {
                    log.error("[communication-service] [MessageConsumer.consumeAssignmentCompleted] ConsumerRecord value is not a Map: {}", 
                        recordValue != null ? recordValue.getClass().getName() : "null");
                    if (acknowledgment != null) {
                        acknowledgment.acknowledge();
                    }
                    return;
                }
            } else {
                log.error("[communication-service] [MessageConsumer.consumeAssignmentCompleted] Received unexpected message type in assignment-completed topic: {}. Expected AssignmentCompletedEvent.", 
                    payload != null ? payload.getClass().getName() : "null");
                // Acknowledge to skip this message and avoid reprocessing
                if (acknowledgment != null) {
                    acknowledgment.acknowledge();
                }
                return;
            }
            
            log.debug("[communication-service] [MessageConsumer.consumeAssignmentCompleted] Event details: parcelId={}, sessionId={}, deliveryManId={}, receiverId={}", 
                event.getParcelId(), event.getSessionId(), event.getDeliveryManId(), event.getReceiverId());
            
            // Handle the event (creates notifications and messages)
            assignmentNotificationService.handleAssignmentCompleted(event);
            
            // Acknowledge message processing
            if (acknowledgment != null) {
                acknowledgment.acknowledge();
            }
            
        } catch (Exception e) {
            log.error("[communication-service] [MessageConsumer.consumeAssignmentCompleted] Error consuming assignment completed event", e);
            // Don't acknowledge - message will be reprocessed
        }
    }
    
    /**
     * Consume parcel postponed events from session-service
     * Sends message to user when parcel is postponed (out of session)
     */
    @KafkaListener(
        topics = KafkaConfig.TOPIC_PARCEL_POSTPONED,
        groupId = "${spring.kafka.consumer.group-id:communication-service-group}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeParcelPostponed(
            @Payload Object payload,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {
        
        try {
            log.debug("[communication-service] [MessageConsumer.consumeParcelPostponed] Received parcel postponed event from Kafka. Topic: {}, Partition: {}, Offset: {}", 
                topic, partition, offset);
            
            // Convert payload to ParcelPostponedEvent
            ParcelPostponedEvent event;
            if (payload instanceof ParcelPostponedEvent) {
                event = (ParcelPostponedEvent) payload;
            } else if (payload instanceof java.util.Map) {
                try {
                    event = objectMapper.convertValue(payload, ParcelPostponedEvent.class);
                } catch (Exception e) {
                    log.error("[communication-service] [MessageConsumer.consumeParcelPostponed] Failed to convert payload to ParcelPostponedEvent", e);
                    if (acknowledgment != null) acknowledgment.acknowledge();
                    return;
                }
            } else {
                log.error("[communication-service] [MessageConsumer.consumeParcelPostponed] Received unexpected message type in parcel-postponed topic: {}. Expected ParcelPostponedEvent.", 
                    payload != null ? payload.getClass().getName() : "null");
                if (acknowledgment != null) acknowledgment.acknowledge();
                return;
            }
            
            log.debug("[communication-service] [MessageConsumer.consumeParcelPostponed] Event details: parcelId={}, receiverId={}, postponeDateTime={}", 
                event.getParcelId(), event.getReceiverId(), event.getPostponeDateTime());
            
            // Handle the event (sends message to user)
            postponeNotificationService.handleParcelPostponed(event);
            
            // Acknowledge message processing
            if (acknowledgment != null) {
                acknowledgment.acknowledge();
            }
            
        } catch (Exception e) {
            log.error("[communication-service] [MessageConsumer.consumeParcelPostponed] Error consuming parcel postponed event", e);
            // Don't acknowledge - message will be reprocessed
        }
    }
    
    /**
     * Consume session completed events from session-service
     * Notifies all related clients and shipper to update their parcel lists
     */
    @KafkaListener(
        topics = KafkaConfig.TOPIC_SESSION_COMPLETED,
        groupId = "${spring.kafka.consumer.group-id:communication-service-group}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeSessionCompleted(
            @Payload Object payload,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {
        
        try {
            log.debug("[communication-service] [MessageConsumer.consumeSessionCompleted] Received session completed event from Kafka. Topic: {}, Partition: {}, Offset: {}", 
                topic, partition, offset);
            
            // Convert payload to SessionCompletedEvent
            SessionCompletedEvent event;
            Object actualPayload = payload;
            
            // Handle ConsumerRecord wrapper
            if (payload instanceof org.apache.kafka.clients.consumer.ConsumerRecord) {
                actualPayload = ((org.apache.kafka.clients.consumer.ConsumerRecord<?, ?>) payload).value();
                log.debug("[communication-service] [MessageConsumer.consumeSessionCompleted] Extracted value from ConsumerRecord: {}", 
                    actualPayload != null ? actualPayload.getClass().getName() : "null");
            }
            
            if (actualPayload instanceof SessionCompletedEvent) {
                event = (SessionCompletedEvent) actualPayload;
            } else if (actualPayload instanceof java.util.Map) {
                try {
                    event = objectMapper.convertValue(actualPayload, SessionCompletedEvent.class);
                } catch (Exception e) {
                    log.error("[communication-service] [MessageConsumer.consumeSessionCompleted] Failed to convert payload to SessionCompletedEvent", e);
                    if (acknowledgment != null) acknowledgment.acknowledge();
                    return;
                }
            } else if (actualPayload instanceof String) {
                try {
                    event = objectMapper.readValue((String) actualPayload, SessionCompletedEvent.class);
                } catch (Exception e) {
                    log.error("[communication-service] [MessageConsumer.consumeSessionCompleted] Failed to parse JSON string to SessionCompletedEvent", e);
                    if (acknowledgment != null) acknowledgment.acknowledge();
                    return;
                }
            } else {
                log.error("[communication-service] [MessageConsumer.consumeSessionCompleted] Received unexpected message type in session-completed topic: {}. Expected SessionCompletedEvent.", 
                    actualPayload != null ? actualPayload.getClass().getName() : "null");
                if (acknowledgment != null) acknowledgment.acknowledge();
                return;
            }
            
            log.debug("[communication-service] [MessageConsumer.consumeSessionCompleted] Event details: sessionId={}, deliveryManId={}, receiverIds={}", 
                event.getSessionId(), event.getDeliveryManId(), event.getReceiverIds());
            
            // Handle the event (notifies all related clients/shippers)
            sessionCompletionNotificationService.handleSessionCompleted(event);
            
            // Acknowledge message processing
            if (acknowledgment != null) {
                acknowledgment.acknowledge();
            }
            
        } catch (Exception e) {
            log.error("[communication-service] [MessageConsumer.consumeSessionCompleted] Error consuming session completed event", e);
            // Don't acknowledge - message will be reprocessed
        }
    }
}
