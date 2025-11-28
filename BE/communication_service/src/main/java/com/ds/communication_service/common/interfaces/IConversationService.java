package com.ds.communication_service.common.interfaces;

import java.util.List;

import com.ds.communication_service.app_context.models.Conversation;
import com.ds.communication_service.common.dto.ConversationResponse;

public interface IConversationService {
    Conversation findOrCreateConversation(String userA, String userB);
    List<Conversation> getConversationsForUser(String userId);
    
    /**
     * Get conversations for a user with optional message history
     * @param userId Current user ID
     * @param includeMessages Whether to include message history
     * @param messageLimit Maximum number of messages per conversation (if includeMessages is true)
     * @return List of ConversationResponse with optional messages
     */
    List<ConversationResponse> getConversationsForUserWithMessages(String userId, boolean includeMessages, int messageLimit);
}
