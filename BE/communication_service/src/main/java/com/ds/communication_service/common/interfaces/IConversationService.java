package com.ds.communication_service.common.interfaces;

import java.util.List;

import com.ds.communication_service.app_context.models.Conversation;

public interface IConversationService {
    Conversation findOrCreateConversation(String userA, String userB);
    List<Conversation> getConversationsForUser(String userId);
}
