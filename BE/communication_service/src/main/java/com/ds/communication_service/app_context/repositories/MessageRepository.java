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
}
