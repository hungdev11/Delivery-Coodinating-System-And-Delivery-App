package com.ds.communication_service.business.v1.services;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import com.ds.communication_service.app_context.models.Conversation;
import com.ds.communication_service.app_context.models.Message;
import com.ds.communication_service.app_context.repositories.ConversationRepository;
import com.ds.communication_service.app_context.repositories.MessageRepository;
import com.ds.communication_service.common.dto.ChatMessagePayload;
import com.ds.communication_service.common.dto.InteractiveProposalResponseDTO;
import com.ds.communication_service.common.dto.MessageResponse;
import com.ds.communication_service.common.enums.ContentType;
import com.ds.communication_service.common.enums.MessageStatus;
import com.ds.communication_service.common.interfaces.IMessageService;
import com.ds.communication_service.infrastructure.kafka.MessageProducer;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class MessageService implements IMessageService{
    private final MessageRepository messageRepository;
    private final ConversationRepository conversationRepository;
    private final ConversationService conversationService;
    private final MessageProducer messageProducer; 

    @Override
    public Page<MessageResponse> getMessagesForConversation(UUID conversationId, String userId, Pageable pageable) {
        Conversation conversation = conversationRepository.findById(conversationId)
                    .orElseThrow(() -> {
                        log.debug("[communication-service] [MessageService.getMessagesForConversation] Conversation not found with ID: {}", conversationId);
                        return new EntityNotFoundException("Conversation not found with ID: " + conversationId);
                    });

        if (!conversation.getUser1Id().equals(userId) && !conversation.getUser2Id().equals(userId)) {
            log.debug("[communication-service] [MessageService.getMessagesForConversation] Security violation: User {} attempted to access conversation {} without permission.", userId, conversationId);
            throw new AccessDeniedException("User does not have access to this conversation."); 
        }        
        
        Page<Message> messagePage = messageRepository.findByConversation_Id(conversationId, pageable);
        return messagePage.map(this::toDto);
    }

    @Override
    @Transactional
    public MessageResponse processAndSaveMessage(ChatMessagePayload payload, String senderId) {
        // 1. Find or create conversation
        Conversation conversation = conversationService
                    .findOrCreateConversation(senderId, payload.getRecipientId());
        log.debug("[communication-service] [MessageService.processAndSaveMessage] Send message: {}", payload.getContent());
        
        // 2. Create and save message with SENT status
        Message message = new Message();
        message.setConversation(conversation);
        message.setSenderId(senderId); 
        message.setContent(payload.getContent());
        message.setType(ContentType.TEXT);
        message.setStatus(MessageStatus.SENT); // Set initial status
        message.setSentAt(java.time.LocalDateTime.now()); // Explicitly set sentAt timestamp

        Message savedMessage = messageRepository.save(message);
        log.debug("[communication-service] [MessageService.processAndSaveMessage] Message saved to database with status SENT. MessageId: {}", savedMessage.getId());

        // 3. Publish message to Kafka for guaranteed delivery
        // Use conversationId as partition key for ordering
        try {
            messageProducer.publishMessage(
                conversation.getId().toString(), 
                payload
            );
            log.debug("[communication-service] [MessageService.processAndSaveMessage] Message published to Kafka for guaranteed delivery");
        } catch (Exception e) {
            log.error("[communication-service] [MessageService.processAndSaveMessage] Failed to publish message to Kafka", e);
            // Message is still saved in DB, can be reprocessed later
        }

        return toDto(savedMessage);
    }
    
    /**
     * Update message status
     * Used by MessageStatusService to track message lifecycle
     */
    @Transactional
    public void updateMessageStatus(UUID messageId, MessageStatus status) {
        Message message = messageRepository.findById(messageId)
            .orElseThrow(() -> new EntityNotFoundException("Message not found: " + messageId));
        
        message.setStatus(status);
        
        if (status == MessageStatus.DELIVERED) {
            message.setDeliveredAt(java.time.LocalDateTime.now());
        } else if (status == MessageStatus.READ) {
            message.setReadAt(java.time.LocalDateTime.now());
        }
        
        messageRepository.save(message);
        log.debug("[communication-service] [MessageService.updateMessageStatus] Message status updated: messageId={}, status={}", messageId, status);
    }

    private MessageResponse toDto(Message message) {        
        InteractiveProposalResponseDTO res = message.getProposal() != null ? InteractiveProposalResponseDTO.from(message.getProposal()) : null;
        return MessageResponse.builder()
            .id(message.getId().toString()) 
            .conversationId(message.getConversation().getId().toString())
            .content(message.getContent())
            .type(message.getType())
            .senderId(message.getSenderId())
            .sentAt(message.getSentAt())
            .status(message.getStatus()) // Include status
            .deliveredAt(message.getDeliveredAt()) // Include deliveredAt
            .readAt(message.getReadAt()) // Include readAt
            .proposal(res) 
            .build();
    }
}
