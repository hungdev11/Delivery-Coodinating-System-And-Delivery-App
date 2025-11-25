package com.ds.communication_service.business.v1.services;

import java.time.LocalDateTime;
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
import com.ds.communication_service.infrastructure.kafka.dto.AssignmentCompletedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service to handle assignment completed events and create notifications
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AssignmentNotificationService {
    
    private final ConversationService conversationService;
    private final MessageRepository messageRepository;
    private final SimpMessageSendingOperations messagingTemplate;
    private final ObjectMapper objectMapper;
    
    /**
     * Handle assignment completed event
     * Creates notifications for shipper and message for client
     */
    @Transactional
    public void handleAssignmentCompleted(AssignmentCompletedEvent event) {
        log.debug("[communication-service] [AssignmentNotificationService.handleAssignmentCompleted] Handling assignment completed event: parcelId={}, assignmentId={}, deliveryManId={}, receiverId={}", 
                event.getParcelId(), event.getAssignmentId(), event.getDeliveryManId(), event.getReceiverId());
        
        try {
            // 1. Create notification for shipper (light notification + pull-to-refresh hint)
            createShipperNotification(event);
            
            // 2. Create message in chat for client (like proposal) with parcel info
            createClientMessage(event);
            
            // 3. Create update notification for client (parcel list notification)
            createClientUpdateNotification(event);
            
            log.debug("[communication-service] [AssignmentNotificationService.handleAssignmentCompleted] Successfully processed assignment completed event: parcelId={}", event.getParcelId());
            
        } catch (Exception e) {
            log.error("[communication-service] [AssignmentNotificationService.handleAssignmentCompleted] Error handling assignment completed event: parcelId={}", 
                    event.getParcelId(), e);
            throw e; // Re-throw to prevent acknowledgment
        }
    }
    
    /**
     * Create light notification for shipper on Orders screen
     */
    private void createShipperNotification(AssignmentCompletedEvent event) {
        try {
            String deliveryManId = event.getDeliveryManId();
            
            // Create UpdateNotification for shipper (triggers pull-to-refresh)
            UpdateNotificationDTO updateNotification = UpdateNotificationDTO.builder()
                    .id(UUID.randomUUID().toString())
                    .userId(deliveryManId)
                    .updateType(UpdateNotificationDTO.UpdateType.ASSIGNMENT_UPDATE)
                    .entityType(UpdateNotificationDTO.EntityType.ASSIGNMENT)
                    .entityId(event.getAssignmentId())
                    .action(UpdateNotificationDTO.ActionType.COMPLETED)
                    .message(String.format("Đơn hàng %s đã hoàn thành", 
                            event.getParcelCode() != null ? event.getParcelCode() : event.getParcelId()))
                    .timestamp(LocalDateTime.now())
                    .clientType(UpdateNotificationDTO.ClientType.ANDROID) // For DeliveryApp
                    .data(Map.of(
                            "parcelId", event.getParcelId(),
                            "parcelCode", event.getParcelCode() != null ? event.getParcelCode() : "",
                            "assignmentId", event.getAssignmentId(),
                            "completedAt", event.getCompletedAt().toString()
                    ))
                    .build();
            
            // Send via WebSocket
            if (deliveryManId != null) {
                messagingTemplate.convertAndSendToUser(
                        deliveryManId,
                        "/queue/updates",
                        updateNotification
                );
            }
            
            log.debug("[communication-service] [AssignmentNotificationService.createShipperNotification] Shipper notification sent: deliveryManId={}, parcelCode={}", 
                    deliveryManId, event.getParcelCode());
            
        } catch (Exception e) {
            log.error("[communication-service] [AssignmentNotificationService.createShipperNotification] Failed to create shipper notification", e);
            // Don't throw - continue with other notifications
        }
    }
    
    /**
     * Create message in chat for client (similar to proposal)
     * Contains parcel info and completion time
     */
    private void createClientMessage(AssignmentCompletedEvent event) {
        try {
            String receiverId = event.getReceiverId();
            String deliveryManId = event.getDeliveryManId();
            
            if (receiverId == null || deliveryManId == null) {
                log.debug("[communication-service] [AssignmentNotificationService.createClientMessage] Cannot create client message: receiverId or deliveryManId is null");
                return;
            }
            
            // Find or create conversation between receiver and delivery man
            Conversation conversation = conversationService.findOrCreateConversation(receiverId, deliveryManId);
            
            // Create message content with parcel info
            Map<String, Object> messageData = new HashMap<>();
            messageData.put("type", "DELIVERY_COMPLETED");
            messageData.put("parcelId", event.getParcelId());
            messageData.put("parcelCode", event.getParcelCode());
            messageData.put("completedAt", event.getCompletedAt().toString());
            messageData.put("deliveryManId", event.getDeliveryManId());
            messageData.put("deliveryManName", event.getDeliveryManName());
            messageData.put("receiverId", event.getReceiverId());
            messageData.put("receiverName", event.getReceiverName());
            messageData.put("receiverPhone", event.getReceiverPhone());
            
            String messageContent;
            try {
                messageContent = objectMapper.writeValueAsString(messageData);
            } catch (Exception e) {
                log.error("[communication-service] [AssignmentNotificationService.createClientMessage] Failed to serialize message data", e);
                messageContent = String.format("Đơn hàng %s đã được giao thành công lúc %s", 
                        event.getParcelCode() != null ? event.getParcelCode() : event.getParcelId(),
                        event.getCompletedAt());
            }
            
            // Create message (sender is delivery man, recipient is receiver)
            Message message = new Message();
            message.setConversation(conversation);
            message.setSenderId(deliveryManId); // Delivery man sends the notification
            message.setContent(messageContent);
            message.setType(ContentType.DELIVERY_COMPLETED); // Use DELIVERY_COMPLETED type
            message.setStatus(MessageStatus.SENT);
            message.setSentAt(LocalDateTime.now());
            
            Message savedMessage = messageRepository.save(message);
            log.debug("[communication-service] [AssignmentNotificationService.createClientMessage] Client message created: messageId={}, conversationId={}", 
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
            }
            
            // Also send to sender (delivery man) for confirmation
            if (deliveryManId != null) {
                messagingTemplate.convertAndSendToUser(
                        deliveryManId,
                        "/queue/messages",
                        messageResponse
                );
            }
            
            log.debug("[communication-service] [AssignmentNotificationService.createClientMessage] Client message sent via WebSocket: receiverId={}, messageId={}", 
                    receiverId, savedMessage.getId());
            
        } catch (Exception e) {
            log.error("[communication-service] [AssignmentNotificationService.createClientMessage] Failed to create client message", e);
            // Don't throw - continue with other notifications
        }
    }
    
    /**
     * Create update notification for client (parcel list notification)
     */
    private void createClientUpdateNotification(AssignmentCompletedEvent event) {
        try {
            String receiverId = event.getReceiverId();
            
            if (receiverId == null) {
                log.debug("[communication-service] [AssignmentNotificationService.createClientUpdateNotification] Cannot create client update notification: receiverId is null");
                return;
            }
            
            // Create UpdateNotification for client (triggers parcel list refresh)
            UpdateNotificationDTO updateNotification = UpdateNotificationDTO.builder()
                    .id(UUID.randomUUID().toString())
                    .userId(receiverId)
                    .updateType(UpdateNotificationDTO.UpdateType.PARCEL_UPDATE)
                    .entityType(UpdateNotificationDTO.EntityType.PARCEL)
                    .entityId(event.getParcelId())
                    .action(UpdateNotificationDTO.ActionType.STATUS_CHANGED)
                    .message(String.format("Đơn hàng %s đã được giao thành công", 
                            event.getParcelCode() != null ? event.getParcelCode() : event.getParcelId()))
                    .timestamp(LocalDateTime.now())
                    .clientType(UpdateNotificationDTO.ClientType.ALL) // For both Android and Web
                    .data(Map.of(
                            "parcelId", event.getParcelId(),
                            "parcelCode", event.getParcelCode() != null ? event.getParcelCode() : "",
                            "status", "DELIVERED",
                            "completedAt", event.getCompletedAt().toString(),
                            "deliveryManId", event.getDeliveryManId(),
                            "deliveryManName", event.getDeliveryManName() != null ? event.getDeliveryManName() : ""
                    ))
                    .build();
            
            // Send via WebSocket
            if (receiverId != null) {
                messagingTemplate.convertAndSendToUser(
                        receiverId,
                        "/queue/updates",
                        updateNotification
                );
            }
            
            log.debug("[communication-service] [AssignmentNotificationService.createClientUpdateNotification] Client update notification sent: receiverId={}, parcelCode={}", 
                    receiverId, event.getParcelCode());
            
        } catch (Exception e) {
            log.error("[communication-service] [AssignmentNotificationService.createClientUpdateNotification] Failed to create client update notification", e);
            // Don't throw - other notifications may have succeeded
        }
    }
}
