package com.ds.communication_service.business.v1.services;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;

import com.ds.communication_service.app_context.models.Conversation;
import com.ds.communication_service.app_context.models.Message;
import com.ds.communication_service.app_context.repositories.MessageRepository;
import com.ds.communication_service.common.dto.MessageResponse;
import com.ds.communication_service.common.dto.UpdateNotificationDTO;
import com.ds.communication_service.common.enums.ContentType;
import com.ds.communication_service.common.enums.MessageStatus;
import com.ds.communication_service.infrastructure.kafka.dto.ParcelPostponedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service to handle parcel postponed events and send messages to users
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PostponeNotificationService {
    
    private final ConversationService conversationService;
    private final MessageRepository messageRepository;
    private final SimpMessageSendingOperations messagingTemplate;
    private final WebSocketSessionManager sessionManager;
    private final ObjectMapper objectMapper;
    
    /**
     * Handle parcel postponed event
     * Sends message to user when parcel is postponed (out of session)
     */
    @Transactional
    public void handleParcelPostponed(ParcelPostponedEvent event) {
        log.debug("[communication-service] [PostponeNotificationService.handleParcelPostponed] Handling parcel postponed event: parcelId={}, assignmentId={}, receiverId={}, postponeDateTime={}", 
                event.getParcelId(), event.getAssignmentId(), event.getReceiverId(), event.getPostponeDateTime());
        
        try {
            String receiverId = event.getReceiverId();
            if (receiverId == null) {
                log.debug("[communication-service] [PostponeNotificationService.handleParcelPostponed] Cannot send postpone message: receiverId is null for event {}", event.getEventId());
                return;
            }
            
            // Find or create conversation between shipper and client
            Conversation conversation = conversationService.findOrCreateConversation(event.getDeliveryManId(), receiverId);
            
            // Create message data
            Map<String, Object> messageData = new HashMap<>();
            messageData.put("parcelId", event.getParcelId());
            messageData.put("parcelCode", event.getParcelCode());
            messageData.put("postponeDateTime", event.getPostponeDateTime() != null ? 
                event.getPostponeDateTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null);
            messageData.put("reason", event.getReason());
            messageData.put("deliveryManId", event.getDeliveryManId());
            messageData.put("receiverId", event.getReceiverId());
            messageData.put("receiverName", event.getReceiverName());
            
            String jsonContent;
            try {
                jsonContent = objectMapper.writeValueAsString(messageData);
            } catch (Exception e) {
                log.error("[communication-service] [PostponeNotificationService.handleParcelPostponed] Error converting message data to JSON for parcel {}", event.getParcelId(), e);
                jsonContent = "{}";
            }
            
            // Create message (sender is delivery man, recipient is receiver)
            Message message = new Message();
            message.setConversation(conversation);
            message.setSenderId(event.getDeliveryManId()); // Delivery man sends the notification
            message.setContent(jsonContent);
            message.setType(ContentType.TEXT); // Use TEXT for postpone message
            message.setStatus(MessageStatus.SENT);
            message.setSentAt(LocalDateTime.now());
            
            Message savedMessage = messageRepository.save(message);
            log.debug("[communication-service] [PostponeNotificationService.handleParcelPostponed] Postpone message created: messageId={}, conversationId={}", 
                    savedMessage.getId(), conversation.getId());
            
            // Convert to DTO and send via WebSocket
            MessageResponse messageResponse = MessageResponse.builder()
                    .id(savedMessage.getId().toString())
                    .conversationId(conversation.getId().toString())
                    .senderId(savedMessage.getSenderId())
                    .content(savedMessage.getContent())
                    .type(savedMessage.getType())
                    .sentAt(savedMessage.getSentAt())
                    .status(savedMessage.getStatus())
                    .build();
            
            // Send to receiver (client)
            if (receiverId != null) {
                messagingTemplate.convertAndSendToUser(
                        receiverId,
                        "/queue/messages",
                        messageResponse
                );
                log.debug("[communication-service] [PostponeNotificationService.handleParcelPostponed] Postpone message sent via WebSocket to receiver: {}", receiverId);
            }
            
            // Also send update notification to client to refresh parcel list
            UpdateNotificationDTO clientNotification = UpdateNotificationDTO.builder()
                    .id(UUID.randomUUID().toString())
                    .userId(receiverId)
                    .updateType(UpdateNotificationDTO.UpdateType.PARCEL_UPDATE)
                    .entityType(UpdateNotificationDTO.EntityType.PARCEL)
                    .entityId(event.getParcelId())
                    .action(UpdateNotificationDTO.ActionType.STATUS_CHANGED)
                    .message(String.format("Đơn hàng %s đã được hoãn đến %s", 
                            event.getParcelCode() != null ? event.getParcelCode() : event.getParcelId(),
                            event.getPostponeDateTime() != null ? 
                                event.getPostponeDateTime().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "thời gian sau"))
                    .clientType(UpdateNotificationDTO.ClientType.WEB)
                    .timestamp(LocalDateTime.now())
                    .build();
            
            sessionManager.sendToUserIfClientType(
                receiverId,
                "/queue/updates",
                clientNotification,
                UpdateNotificationDTO.ClientType.WEB
            );
            log.debug("[communication-service] [PostponeNotificationService.handleParcelPostponed] Postpone update notification sent to client: {}", receiverId);
            
        } catch (Exception e) {
            log.error("[communication-service] [PostponeNotificationService.handleParcelPostponed] Error handling parcel postponed event: parcelId={}", 
                    event.getParcelId(), e);
            throw e;
        }
    }
}
