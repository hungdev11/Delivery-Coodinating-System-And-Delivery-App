package com.ds.communication_service.app_context.repositories;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ds.communication_service.app_context.models.Message;

@Repository
public interface MessageRepository extends JpaRepository<Message, UUID> {
    Page<Message> findByConversation_Id(UUID conversationId, Pageable pageable);
    
    /**
     * Get the last message time for a conversation
     */
    @Query("SELECT MAX(m.sentAt) FROM Message m WHERE m.conversation.id = :conversationId")
    Optional<LocalDateTime> findLastMessageTimeByConversationId(@Param("conversationId") UUID conversationId);
    
    /**
     * Count unread messages for a user in a conversation
     * Unread = messages where senderId != userId AND status != READ
     */
    @Query("SELECT COUNT(m) FROM Message m WHERE m.conversation.id = :conversationId " +
           "AND m.senderId != :userId AND m.status != 'READ'")
    long countUnreadMessagesByConversationIdAndUserId(
        @Param("conversationId") UUID conversationId,
        @Param("userId") String userId
    );
    
    /**
     * Get last message content for a conversation
     * Uses native query with LIMIT 1 to ensure only one result is returned
     * Returns content from message, or empty string if null
     */
    @Query(value = "SELECT COALESCE(m.content, '') " +
           "FROM messages m " +
           "WHERE m.conversation_id = :conversationId " +
           "ORDER BY m.sent_at DESC LIMIT 1", nativeQuery = true)
    Optional<String> findLastMessageContentByConversationId(@Param("conversationId") UUID conversationId);
}
