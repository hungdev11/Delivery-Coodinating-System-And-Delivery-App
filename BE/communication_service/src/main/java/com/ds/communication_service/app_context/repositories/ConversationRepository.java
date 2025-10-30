package com.ds.communication_service.app_context.repositories;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ds.communication_service.app_context.models.Conversation;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, UUID> {

    Optional<Conversation> findByUser1IdAndUser2Id(String user1, String user2);
    
    @Query("SELECT c FROM Conversation c WHERE c.user1Id = :userId OR c.user2Id = :userId")
    List<Conversation> findAllByUserId(@Param("userId") String userId);
}
