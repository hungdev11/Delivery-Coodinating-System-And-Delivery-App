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
import com.ds.communication_service.common.dto.BaseResponse;
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
    public ResponseEntity<BaseResponse<PageResponse<MessageResponse>>> getMessages(
            @PathVariable String conversationId,
            @RequestParam String userId,
            @PageableDefault(size = 30, sort = "sentAt", direction = Sort.Direction.DESC) Pageable pageable) {
        // Ensure DESC sort by sentAt (fix for incorrect sorting)
        // If pageable doesn't have sort or has wrong direction, force DESC
        Pageable sortedPageable = pageable;
        Sort.Order sentAtOrder = pageable.getSort().getOrderFor("sentAt");
        if (pageable.getSort().isUnsorted() ||
                sentAtOrder == null ||
                sentAtOrder.getDirection() != Sort.Direction.DESC) {
            log.debug("[communication-service] [ConversationApiController.getMessages] Fixing sort order: forcing DESC by sentAt. Original sort: {}", pageable.getSort());
            sortedPageable = PageRequest.of(
                    pageable.getPageNumber(),
                    pageable.getPageSize(),
                    Sort.by(Sort.Direction.DESC, "sentAt"));
        } else {
            log.debug("[communication-service] [ConversationApiController.getMessages] Sort order is correct: {}", pageable.getSort());
        }

        Page<MessageResponse> messages = messageService.getMessagesForConversation(UUID.fromString(conversationId),
                userId, sortedPageable);

        // Log first and last message timestamps for debugging
        if (messages.hasContent()) {
            MessageResponse first = messages.getContent().get(0);
            MessageResponse last = messages.getContent().get(messages.getContent().size() - 1);
            log.debug("[communication-service] [ConversationApiController.getMessages] Messages returned: total={}, first sentAt={}, last sentAt={}",
                    messages.getTotalElements(), first.getSentAt(), last.getSentAt());
        }

        return ResponseEntity.ok(BaseResponse.success(PageResponse.from(messages)));
    }

    @GetMapping("/find-by-users")
    public ResponseEntity<BaseResponse<ConversationResponse>> getConversationByTwoUsers(
            @RequestParam("user1") String userId1,
            @RequestParam("user2") String userId2) {
        Conversation conversation = conversationService.findOrCreateConversation(userId1, userId2);
        return ResponseEntity.ok(BaseResponse.success(mapToConversationResponse(conversation, userId2)));
    }

    @GetMapping("/user/{currentUserId}")
    public ResponseEntity<BaseResponse<List<ConversationResponse>>> getMyConversations(
            @PathVariable String currentUserId,
            @RequestParam(required = false, defaultValue = "false") Boolean includeMessages,
            @RequestParam(required = false, defaultValue = "50") int messageLimit) {
        try {
            // Convert Boolean to boolean, defaulting to false if null
            boolean includeMessagesFlag = includeMessages != null ? includeMessages : false;
            log.debug("Getting conversations for user: {}, includeMessages: {} (parsed as {}), messageLimit: {}", 
                    currentUserId, includeMessages, includeMessagesFlag, messageLimit);

            // Delegate to service layer for business logic
            List<ConversationResponse> responseDtos = conversationService.getConversationsForUserWithMessages(
                    currentUserId, includeMessagesFlag, messageLimit);

            if (responseDtos == null || responseDtos.isEmpty()) {
                log.debug("No conversations found for user: {}", currentUserId);
                return ResponseEntity.ok(BaseResponse.success(java.util.Collections.emptyList()));
            }

            log.debug("Successfully processed {} conversations for user: {}", responseDtos.size(), currentUserId);
            return ResponseEntity.ok(BaseResponse.success(responseDtos));
        } catch (Exception e) {
            log.error("[communication-service] [ConversationApiController.getMyConversations] Error getting conversations for user {}", currentUserId, e);
            return ResponseEntity.ok(BaseResponse.success(java.util.Collections.emptyList())); // Return empty list
        }
    }

    /**
     * Map Conversation entity to ConversationResponse DTO (for find-by-users endpoint)
     * This is a simplified version without messages
     */
    private ConversationResponse mapToConversationResponse(Conversation conversation, String currentUserId) {
        // Determine partner ID
        String partnerId = conversation.getUser1Id().equals(currentUserId)
                ? conversation.getUser2Id()
                : conversation.getUser1Id();

        // Get last message time
        LocalDateTime lastMessageTime = messageRepository
                .findLastMessageTimeByConversationId(conversation.getId())
                .orElse(conversation.getCreatedAt());

        // Get last message content
        String lastMessageContent = messageRepository
                .findLastMessageContentByConversationId(conversation.getId())
                .orElse(null);

        // Calculate unread count
        long unreadCount = messageRepository.countUnreadMessagesByConversationIdAndUserId(
                conversation.getId(), currentUserId);

        // Fetch user info
        UserInfoDto userInfo = null;
        try {
            userInfo = userServiceClient.getUserById(partnerId);
        } catch (Exception e) {
            log.debug("Error fetching user info for partnerId {}: {}", partnerId, e.getMessage());
        }

        String partnerName;
        String partnerUsername = null;
        if (userInfo != null) {
            partnerName = userInfo.getFullName();
            partnerUsername = userInfo.getUsername();
        } else {
            partnerName = "User " + partnerId.substring(0, Math.min(4, partnerId.length()));
        }

        // Check online status
        Boolean isOnline = null;
        try {
            if (webSocketSessionManager != null) {
                isOnline = webSocketSessionManager.isUserOnline(partnerId);
            }
        } catch (Exception e) {
            log.debug("Could not check online status for user {}: {}", partnerId, e.getMessage());
        }

        return ConversationResponse.builder()
                .conversationId(conversation.getId().toString())
                .partnerId(partnerId)
                .partnerName(partnerName)
                .partnerUsername(partnerUsername)
                .partnerAvatar(null)
                .isOnline(isOnline)
                .lastMessageTime(lastMessageTime)
                .lastMessageContent(lastMessageContent)
                .unreadCount((int) unreadCount)
                .build();
    }
}
