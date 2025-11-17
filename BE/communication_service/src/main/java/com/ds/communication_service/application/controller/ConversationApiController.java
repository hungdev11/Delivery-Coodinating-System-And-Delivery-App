package com.ds.communication_service.application.controller;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ds.communication_service.app_context.models.Conversation;
import com.ds.communication_service.common.dto.ConversationResponse;
import com.ds.communication_service.common.dto.MessageResponse;
import com.ds.communication_service.common.dto.PageResponse;
import com.ds.communication_service.common.dto.UserInfoDto;
import com.ds.communication_service.common.interfaces.IConversationService;
import com.ds.communication_service.common.interfaces.IMessageService;
import com.ds.communication_service.app_context.repositories.MessageRepository;
import com.ds.communication_service.business.v1.services.UserServiceClient;
import com.ds.communication_service.business.v1.services.WebSocketSessionManager;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/conversations")
@Slf4j
public class ConversationApiController {

    private final IMessageService messageService;
    private final IConversationService conversationService;
    private final MessageRepository messageRepository;
    private final UserServiceClient userServiceClient;
    private final WebSocketSessionManager webSocketSessionManager;

    @GetMapping("/{conversationId}/messages")
    public ResponseEntity<PageResponse<MessageResponse>> getMessages(
            @PathVariable String conversationId,
            @RequestParam String userId,
            @PageableDefault(size = 30, sort = "sentAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        // Ensure DESC sort by sentAt (fix for incorrect sorting)
        // If pageable doesn't have sort or has wrong direction, force DESC
        Pageable sortedPageable = pageable;
        Sort.Order sentAtOrder = pageable.getSort().getOrderFor("sentAt");
        if (pageable.getSort().isUnsorted() || 
            sentAtOrder == null ||
            sentAtOrder.getDirection() != Sort.Direction.DESC) {
            log.info("🔧 Fixing sort order: forcing DESC by sentAt. Original sort: {}", pageable.getSort());
            sortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "sentAt")
            );
        } else {
            log.debug("✅ Sort order is correct: {}", pageable.getSort());
        }
        
        Page<MessageResponse> messages =
                messageService.getMessagesForConversation(UUID.fromString(conversationId), userId, sortedPageable);
        
        // Log first and last message timestamps for debugging
        if (messages.hasContent()) {
            MessageResponse first = messages.getContent().get(0);
            MessageResponse last = messages.getContent().get(messages.getContent().size() - 1);
            log.info("📋 Messages returned: total={}, first sentAt={}, last sentAt={}", 
                    messages.getTotalElements(), first.getSentAt(), last.getSentAt());
        }
        
        return ResponseEntity.ok(PageResponse.from(messages));
    }

    @GetMapping("/find-by-users")
    public ResponseEntity<ConversationResponse> getConversationByTwoUsers(
            @RequestParam("user1") String userId1,
            @RequestParam("user2") String userId2
    ) {
        Conversation conversation = conversationService.findOrCreateConversation(userId1, userId2);
        return ResponseEntity.ok(mapToConversationResponse(conversation, userId2)); 
    }

    @GetMapping("/user/{currentUserId}")
    public ResponseEntity<List<ConversationResponse>> getMyConversations(
            @PathVariable String currentUserId
    ) {
        try {
            List<Conversation> conversations = conversationService.getConversationsForUser(currentUserId);
            
            if (conversations == null || conversations.isEmpty()) {
                log.debug("No conversations found for user: {}", currentUserId);
                return ResponseEntity.ok(List.of());
            }

            log.debug("Processing {} conversations for user: {}", conversations.size(), currentUserId);
            
            // Process conversations with error handling to prevent one failure from blocking all
            List<ConversationResponse> responseDtos = conversations.stream()
                    .map(conv -> {
                        try {
                            return mapToConversationResponse(conv, currentUserId);
                        } catch (Exception e) {
                            log.error("Error mapping conversation {} for user {}: {}", 
                                    conv.getId(), currentUserId, e.getMessage(), e);
                            // Return a minimal response instead of failing completely
                            return ConversationResponse.builder()
                                    .conversationId(conv.getId().toString())
                                    .partnerId(conv.getUser1Id().equals(currentUserId) 
                                            ? conv.getUser2Id() 
                                            : conv.getUser1Id())
                                    .partnerName("Unknown User")
                                    .unreadCount(0)
                                    .build();
                        }
                    })
                    .collect(Collectors.toList());

            log.debug("Successfully processed {} conversations for user: {}", responseDtos.size(), currentUserId);
            return ResponseEntity.ok(responseDtos);
        } catch (Exception e) {
            log.error("Error getting conversations for user {}: {}", currentUserId, e.getMessage(), e);
            return ResponseEntity.ok(List.of()); // Return empty list instead of error
        }
    }

    private ConversationResponse mapToConversationResponse(Conversation conversation, String currentUserId) {
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
            log.warn("Error getting last message time for conversation {}: {}", 
                    conversation.getId(), e.getMessage());
            lastMessageTime = conversation.getCreatedAt();
        }
        
        // Get last message content (with error handling)
        // Simplified: just get from query, no expensive fallback queries
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
            log.warn("Error getting last message content for conversation {}: {}", 
                    conversation.getId(), e.getMessage());
            lastMessageContent = null;
        }
        
        // Calculate unread count (messages where sender != currentUserId and status != READ)
        long unreadCount = 0;
        try {
            unreadCount = messageRepository.countUnreadMessagesByConversationIdAndUserId(
                conversation.getId(), currentUserId
            );
        } catch (Exception e) {
            log.warn("Error counting unread messages for conversation {}: {}", 
                    conversation.getId(), e.getMessage());
            unreadCount = 0;
        }
        
        // Fetch user info from User Service (with error handling)
        UserInfoDto userInfo = null;
        try {
            userInfo = userServiceClient.getUserById(partnerId);
        } catch (Exception e) {
            log.warn("Error fetching user info for partnerId {}: {}", partnerId, e.getMessage());
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
        
        ConversationResponse dto = ConversationResponse.builder()
            .conversationId(conversation.getId().toString())
            .partnerId(partnerId)
            .partnerName(partnerName)
            .partnerUsername(partnerUsername)
            .partnerAvatar(null) // TODO: Add avatar support when available
            .isOnline(isOnline)
            .lastMessageTime(lastMessageTime)
            .lastMessageContent(lastMessageContent)
            .unreadCount((int) unreadCount)
            .build();

        return dto;
    }
}
