package com.ds.communication_service.application.controller;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
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
import com.ds.communication_service.common.interfaces.IConversationService;
import com.ds.communication_service.common.interfaces.IMessageService;
import com.ds.communication_service.app_context.repositories.MessageRepository;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/conversations")
public class ConversationApiController {

    private final IMessageService messageService;
    private final IConversationService conversationService;
    private final MessageRepository messageRepository;

    @GetMapping("/{conversationId}/messages")
    public ResponseEntity<PageResponse<MessageResponse>> getMessages(
            @PathVariable String conversationId,
            @RequestParam String userId,
            @PageableDefault(size = 30, sort = "sentAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {        
        Page<MessageResponse> messages =
                messageService.getMessagesForConversation(UUID.fromString(conversationId), userId, pageable);
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
        List<Conversation> conversations = conversationService.getConversationsForUser(currentUserId);

        List<ConversationResponse> responseDtos = conversations.stream()
                .map(conv -> mapToConversationResponse(conv, currentUserId)) 
                .collect(Collectors.toList());

        return ResponseEntity.ok(responseDtos);
    }

    private ConversationResponse mapToConversationResponse(Conversation conversation, String currentUserId) {
        // Determine partner ID
        String partnerId = conversation.getUser1Id().equals(currentUserId) 
            ? conversation.getUser2Id() 
            : conversation.getUser1Id();
        
        // Get last message time
        LocalDateTime lastMessageTime = messageRepository
            .findLastMessageTimeByConversationId(conversation.getId())
            .orElse(conversation.getCreatedAt());
        
        ConversationResponse dto = ConversationResponse.builder()
            .conversationId(conversation.getId().toString())
            .partnerId(partnerId)
            .partnerName("User " + partnerId.substring(0, Math.min(4, partnerId.length())))
            .partnerAvatar(null)
            .lastMessageTime(lastMessageTime)
            .build();

        return dto;
    }
}
