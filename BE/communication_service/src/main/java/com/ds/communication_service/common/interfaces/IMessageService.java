package com.ds.communication_service.common.interfaces;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.ds.communication_service.common.dto.ChatMessagePayload;
import com.ds.communication_service.common.dto.MessageResponse;

public interface IMessageService {
    Page<MessageResponse> getMessagesForConversation(UUID conversationId, String userId, Pageable pageable);
    MessageResponse processAndSaveMessage(ChatMessagePayload payload, String senderId);
}
