package com.ds.communication_service.business.v1.services;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;

import com.ds.communication_service.app_context.models.Conversation;
import com.ds.communication_service.app_context.models.Message;
import com.ds.communication_service.app_context.repositories.MessageRepository;
import com.ds.communication_service.common.dto.MessageResponse;
import com.ds.communication_service.common.enums.ContentType;
import com.ds.communication_service.common.enums.MessageStatus;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service to handle parcel status changed to SUCCEEDED events
 * Creates messages in chat to notify users about parcel completion
 * Distinguishes between user confirmation and automatic timeout
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ParcelSucceededNotificationService {
    
    private final ConversationService conversationService;
    private final MessageRepository messageRepository;
    private final SimpMessageSendingOperations messagingTemplate;
    private final ObjectMapper objectMapper;
    
    /**
     * Handle parcel status changed to SUCCEEDED
     * Creates message in chat for both receiver and sender
     * 
     * @param parcelId Parcel ID
     * @param parcelCode Parcel code
     * @param receiverId Receiver user ID
     * @param senderId Sender user ID (optional)
     * @param deliveryManId Delivery man ID (optional)
     * @param confirmedBy User ID who confirmed (null if automatic timeout)
     * @param confirmedAt Confirmation timestamp (null if automatic)
     * @param succeededAt Timestamp when parcel status changed to SUCCEEDED
     */
    @Transactional
    public void handleParcelSucceeded(
            String parcelId,
            String parcelCode,
            String receiverId,
            String senderId,
            String deliveryManId,
            String confirmedBy,
            LocalDateTime confirmedAt,
            LocalDateTime succeededAt) {
        
        log.debug("[communication-service] [ParcelSucceededNotificationService.handleParcelSucceeded] Handling parcel succeeded event: parcelId={}, confirmedBy={}", 
                parcelId, confirmedBy);
        
        try {
            // Determine if this was user confirmation or automatic timeout
            boolean isUserConfirmed = confirmedBy != null && !confirmedBy.isBlank();
            String source = isUserConfirmed ? "USER_CONFIRM" : "AUTO_TIMEOUT";
            
            // Determine conversation participants
            // If deliveryManId is available, use receiver-deliveryMan conversation
            // Otherwise, use receiver-sender conversation
            String partnerId = deliveryManId != null && !deliveryManId.isBlank() 
                    ? deliveryManId 
                    : (senderId != null && !senderId.isBlank() ? senderId : null);
            
            if (partnerId == null || receiverId == null) {
                log.debug("[communication-service] [ParcelSucceededNotificationService.handleParcelSucceeded] Cannot create message: missing partnerId or receiverId");
                return;
            }
            
            // Find or create conversation
            Conversation conversation = conversationService.findOrCreateConversation(receiverId, partnerId);
            
            // Create message content with parcel info and source
            Map<String, Object> messageData = new HashMap<>();
            messageData.put("type", "DELIVERY_SUCCEEDED");
            messageData.put("parcelId", parcelId);
            messageData.put("parcelCode", parcelCode != null ? parcelCode : "");
            messageData.put("succeededAt", succeededAt != null ? succeededAt.toString() : LocalDateTime.now().toString());
            messageData.put("source", source); // "USER_CONFIRM" or "AUTO_TIMEOUT"
            messageData.put("isUserConfirmed", isUserConfirmed);
            
            if (isUserConfirmed) {
                messageData.put("confirmedBy", confirmedBy);
                messageData.put("confirmedAt", confirmedAt != null ? confirmedAt.toString() : LocalDateTime.now().toString());
            }
            
            if (deliveryManId != null) {
                messageData.put("deliveryManId", deliveryManId);
            }
            if (senderId != null) {
                messageData.put("senderId", senderId);
            }
            messageData.put("receiverId", receiverId);
            
            String messageContent;
            try {
                messageContent = objectMapper.writeValueAsString(messageData);
            } catch (Exception e) {
                log.error("[communication-service] [ParcelSucceededNotificationService.handleParcelSucceeded] Failed to serialize message data", e);
                messageContent = String.format("{\"type\":\"DELIVERY_SUCCEEDED\",\"parcelId\":\"%s\",\"parcelCode\":\"%s\",\"source\":\"%s\"}", 
                        parcelId, parcelCode != null ? parcelCode : "", source);
            }
            
            // Create message (sender is system/partner, recipient is receiver)
            Message message = new Message();
            message.setConversation(conversation);
            message.setSenderId(partnerId); // Partner (delivery man or sender) sends the notification
            message.setContent(messageContent);
            message.setType(ContentType.DELIVERY_SUCCEEDED);
            message.setStatus(MessageStatus.SENT);
            message.setSentAt(LocalDateTime.now());
            
            Message savedMessage = messageRepository.save(message);
            log.debug("[communication-service] [ParcelSucceededNotificationService.handleParcelSucceeded] Message created: messageId={}, conversationId={}, source={}", 
                    savedMessage.getId(), conversation.getId(), source);
            
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
            
            // Send to receiver
            if (receiverId != null && messageResponse != null) {
                messagingTemplate.convertAndSendToUser(
                        receiverId,
                        "/queue/messages",
                        (Object) messageResponse
                );
            }
            
            // Also send to partner (delivery man or sender)
            if (partnerId != null && messageResponse != null) {
                messagingTemplate.convertAndSendToUser(
                        partnerId,
                        "/queue/messages",
                        (Object) messageResponse
                );
            }
            
            log.debug("[communication-service] [ParcelSucceededNotificationService.handleParcelSucceeded] Message sent via WebSocket: receiverId={}, partnerId={}, source={}", 
                    receiverId, partnerId, source);
            
        } catch (Exception e) {
            log.error("[communication-service] [ParcelSucceededNotificationService.handleParcelSucceeded] Failed to handle parcel succeeded event", e);
            throw e; // Re-throw to prevent acknowledgment
        }
    }
}
