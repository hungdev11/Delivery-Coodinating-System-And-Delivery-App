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
import com.ds.communication_service.common.interfaces.IMessageService;

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

    @Override
    public Page<MessageResponse> getMessagesForConversation(UUID conversationId, String userId, Pageable pageable) {
        Conversation conversation = conversationRepository.findById(conversationId)
                    .orElseThrow(() -> {
                        log.warn("Conversation not found with ID: {}", conversationId);
                        return new EntityNotFoundException("Conversation not found with ID: " + conversationId);
                    });

        if (!conversation.getUser1Id().equals(userId) && !conversation.getUser2Id().equals(userId)) {
            log.warn("Security violation: User {} attempted to access conversation {} without permission.", userId, conversationId);
            throw new AccessDeniedException("User does not have access to this conversation."); 
        }        
        
        Page<Message> messagePage = messageRepository.findByConversation_Id(conversationId, pageable);
        return messagePage.map(this::toDto);
    }

    @Override
    @Transactional
    public MessageResponse processAndSaveMessage(ChatMessagePayload payload, String senderId) {
        // 1. Gọi ConversationService với String ID
        Conversation conversation = conversationService
                    .findOrCreateConversation(senderId, payload.getRecipientId());
        log.info("Send message: {}", payload.getContent());
        Message message = new Message();
        message.setConversation(conversation);
        message.setSenderId(senderId); 
        message.setContent(payload.getContent());
        message.setType(ContentType.TEXT); 

        Message savedMessage = messageRepository.save(message);

        return toDto(savedMessage);
    }

    private MessageResponse toDto(Message message) {        
        InteractiveProposalResponseDTO res = message.getProposal() != null ? InteractiveProposalResponseDTO.from(message.getProposal()) : null;
        return MessageResponse.builder()
            .id(message.getId().toString()) 
            .conversationId(message.getConversation().getId().toString()) // Include conversationId
            .content(message.getContent())
            .type(message.getType())
            .senderId(message.getSenderId())
            .sentAt(message.getSentAt())
            .proposal(res) 
            .build();
    }
}
