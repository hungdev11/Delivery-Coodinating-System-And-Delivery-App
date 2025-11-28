package com.ds.communication_service.common.dto;

import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConversationResponse {
    private String conversationId;
    private String partnerId;
    private String partnerName;
    private String partnerAvatar;
    private String partnerUsername; // Add username for display
    private Boolean isOnline; // Online status (null if unavailable)
    private LocalDateTime lastMessageTime; // Time of last message in conversation
    private String lastMessageContent; // Content of last message
    private Integer unreadCount; // Number of unread messages for current user
    private List<MessageResponse> messages; // Optional: chat history included in response
}
