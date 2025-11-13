package com.ds.communication_service.business.v1.services;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.ds.communication_service.app_context.models.Conversation;
import com.ds.communication_service.app_context.repositories.ConversationRepository;
import com.ds.communication_service.app_context.repositories.MessageRepository;
import com.ds.communication_service.common.interfaces.IConversationService;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConversationService implements IConversationService{

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;

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
}
