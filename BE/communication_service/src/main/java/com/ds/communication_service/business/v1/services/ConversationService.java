package com.ds.communication_service.business.v1.services;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.ds.communication_service.app_context.models.Conversation;
import com.ds.communication_service.app_context.repositories.ConversationRepository;
import com.ds.communication_service.app_context.repositories.MessageRepository;
import com.ds.communication_service.common.dto.ConversationResponse;
import com.ds.communication_service.common.dto.MessageResponse;
import com.ds.communication_service.common.dto.UserInfoDto;
import com.ds.communication_service.common.interfaces.IConversationService;
import com.ds.communication_service.common.interfaces.IMessageService;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;

@Service
@Slf4j
public class ConversationService implements IConversationService{

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final IMessageService messageService;
    private final UserServiceClient userServiceClient;
    private final WebSocketSessionManager webSocketSessionManager;
    
    // Use constructor injection with @Lazy to break circular dependency
    public ConversationService(
            ConversationRepository conversationRepository,
            MessageRepository messageRepository,
            @Lazy IMessageService messageService,
            UserServiceClient userServiceClient,
            WebSocketSessionManager webSocketSessionManager) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.messageService = messageService;
        this.userServiceClient = userServiceClient;
        this.webSocketSessionManager = webSocketSessionManager;
    }

    @Transactional
    @Override
    public Conversation findOrCreateConversation(String userA, String userB) {
        String user1;
        String user2;
        
        if (userA.compareTo(userB) < 0) {
            user1 = userA;
            user2 = userB;
        } else if (userA.compareTo(userB) > 0) {
            user1 = userB;
            user2 = userA;
        } else {
            throw new IllegalArgumentException("User IDs cannot be the same.");
        }

        return conversationRepository.findByUser1IdAndUser2Id(user1, user2)
                .orElseGet(() -> {
                    Conversation newConversation = new Conversation();
                    newConversation.setUser1Id(user1);
                    newConversation.setUser2Id(user2);
                    return conversationRepository.save(newConversation);
                });
    }
    
    @Override
    public List<Conversation> getConversationsForUser(String userId) {
        List<Conversation> conversations = conversationRepository.findAllByUserId(userId);
        
        // Sort by last message time (most recent first), then by creation time
        return conversations.stream()
                .sorted(Comparator
                        .comparing((Conversation c) -> 
                            messageRepository.findLastMessageTimeByConversationId(c.getId())
                                .orElse(c.getCreatedAt()), 
                            Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(Conversation::getCreatedAt, Comparator.reverseOrder()))
                .collect(Collectors.toList());
    }
    
    @Override
    public List<ConversationResponse> getConversationsForUserWithMessages(String userId, boolean includeMessages, int messageLimit) {
        List<Conversation> conversations = getConversationsForUser(userId);
        
        return conversations.stream()
                .map(conv -> mapToConversationResponse(conv, userId, includeMessages, messageLimit))
                .collect(Collectors.toList());
    }
    
    /**
     * Map Conversation entity to ConversationResponse DTO
     * Business logic for building conversation response with optional messages
     */
    private ConversationResponse mapToConversationResponse(Conversation conversation, String currentUserId, 
            boolean includeMessages, int messageLimit) {
        try {
            // Determine partner ID
            String partnerId = conversation.getUser1Id().equals(currentUserId)
                    ? conversation.getUser2Id()
                    : conversation.getUser1Id();

            // Get last message time (with error handling)
            LocalDateTime lastMessageTime;
            try {
                lastMessageTime = messageRepository
                        .findLastMessageTimeByConversationId(conversation.getId())
                        .orElse(conversation.getCreatedAt());
            } catch (Exception e) {
                log.debug("[communication-service] [ConversationService.mapToConversationResponse] Error getting last message time for conversation {}: {}",
                        conversation.getId(), e.getMessage());
                lastMessageTime = conversation.getCreatedAt();
            }

            // Get last message content (with error handling)
            String lastMessageContent = null;
            try {
                lastMessageContent = messageRepository
                        .findLastMessageContentByConversationId(conversation.getId())
                        .orElse(null);

                // If empty string, convert to null for cleaner response
                if (lastMessageContent != null && lastMessageContent.isEmpty()) {
                    lastMessageContent = null;
                }
            } catch (Exception e) {
                log.debug("[communication-service] [ConversationService.mapToConversationResponse] Error getting last message content for conversation {}: {}",
                        conversation.getId(), e.getMessage());
                lastMessageContent = null;
            }

            // Calculate unread count (messages where sender != currentUserId and status != READ)
            long unreadCount = 0;
            try {
                unreadCount = messageRepository.countUnreadMessagesByConversationIdAndUserId(
                        conversation.getId(), currentUserId);
            } catch (Exception e) {
                log.debug("[communication-service] [ConversationService.mapToConversationResponse] Error counting unread messages for conversation {}: {}",
                        conversation.getId(), e.getMessage());
                unreadCount = 0;
            }

            // Fetch user info from User Service (with error handling)
            UserInfoDto userInfo = null;
            try {
                userInfo = userServiceClient.getUserById(partnerId);
            } catch (Exception e) {
                log.debug("[communication-service] [ConversationService.mapToConversationResponse] Error fetching user info for partnerId {}: {}", 
                        partnerId, e.getMessage());
            }

            String partnerName;
            String partnerUsername = null;
            if (userInfo != null) {
                partnerName = userInfo.getFullName();
                partnerUsername = userInfo.getUsername();
            } else {
                // Fallback if user service is unavailable
                partnerName = "User " + partnerId.substring(0, Math.min(4, partnerId.length()));
                log.debug("Could not fetch user info for partnerId: {}, using fallback name", partnerId);
            }

            // Check online status from WebSocket session manager (with error handling)
            Boolean isOnline = null;
            try {
                if (webSocketSessionManager != null) {
                    isOnline = webSocketSessionManager.isUserOnline(partnerId);
                }
            } catch (Exception e) {
                log.debug("Could not check online status for user {}: {}", partnerId, e.getMessage());
            }

            ConversationResponse.ConversationResponseBuilder builder = ConversationResponse.builder()
                    .conversationId(conversation.getId().toString())
                    .partnerId(partnerId)
                    .partnerName(partnerName)
                    .partnerUsername(partnerUsername)
                    .partnerAvatar(null) // TODO: Add avatar support when available
                    .isOnline(isOnline)
                    .lastMessageTime(lastMessageTime)
                    .lastMessageContent(lastMessageContent)
                    .unreadCount((int) unreadCount);

            // Include messages if requested
            if (includeMessages) {
                try {
                    log.debug("[communication-service] [ConversationService.mapToConversationResponse] Loading messages for conversation {} (limit: {})", 
                            conversation.getId(), messageLimit);
                    Pageable messagePageable = PageRequest.of(0, messageLimit, Sort.by(Sort.Direction.DESC, "sentAt"));
                    Page<MessageResponse> messagesPage = messageService.getMessagesForConversation(
                            conversation.getId(), currentUserId, messagePageable);
                    
                    // Convert to list (messages are already sorted DESC - newest first)
                    List<MessageResponse> messages = messagesPage.getContent();
                    builder.messages(messages);
                    
                    log.info("[communication-service] [ConversationService.mapToConversationResponse] ✅ Included {} messages for conversation {} (total available: {})", 
                            messages.size(), conversation.getId(), messagesPage.getTotalElements());
                } catch (Exception e) {
                    log.error("[communication-service] [ConversationService.mapToConversationResponse] ❌ Error loading messages for conversation {}: {}", 
                            conversation.getId(), e.getMessage(), e);
                    builder.messages(java.util.Collections.emptyList());
                }
            } else {
                log.debug("[communication-service] [ConversationService.mapToConversationResponse] Messages not requested for conversation {}", 
                        conversation.getId());
            }

            return builder.build();
        } catch (Exception e) {
            log.error("[communication-service] [ConversationService.mapToConversationResponse] Error mapping conversation {} for user {}", 
                    conversation.getId(), currentUserId, e);
            // Return minimal response instead of failing completely
            String partnerId = conversation.getUser1Id().equals(currentUserId)
                    ? conversation.getUser2Id()
                    : conversation.getUser1Id();
            return ConversationResponse.builder()
                    .conversationId(conversation.getId().toString())
                    .partnerId(partnerId)
                    .partnerName("Unknown User")
                    .unreadCount(0)
                    .messages(includeMessages ? java.util.Collections.emptyList() : null)
                    .build();
        }
    }
}
