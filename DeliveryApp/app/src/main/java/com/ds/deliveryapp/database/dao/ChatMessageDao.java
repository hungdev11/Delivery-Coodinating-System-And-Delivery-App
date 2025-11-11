package com.ds.deliveryapp.database.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.ds.deliveryapp.database.entities.ChatMessageEntity;

import java.util.List;

/**
 * Data Access Object for chat messages
 */
@Dao
public interface ChatMessageDao {
    
    /**
     * Insert a message (replace if exists)
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertMessage(ChatMessageEntity message);
    
    /**
     * Insert multiple messages
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertMessages(List<ChatMessageEntity> messages);
    
    /**
     * Update a message
     */
    @Update
    void updateMessage(ChatMessageEntity message);
    
    /**
     * Get all messages for a conversation
     */
    @Query("SELECT * FROM chat_messages WHERE conversationId = :conversationId ORDER BY sentAt ASC")
    List<ChatMessageEntity> getMessagesForConversation(String conversationId);
    
    /**
     * Get a single message by ID
     */
    @Query("SELECT * FROM chat_messages WHERE id = :messageId")
    ChatMessageEntity getMessageById(String messageId);
    
    /**
     * Update message status
     */
    @Query("UPDATE chat_messages SET status = :status, deliveredAt = :deliveredAt, readAt = :readAt WHERE id = :messageId")
    void updateMessageStatus(String messageId, String status, String deliveredAt, String readAt);
    
    /**
     * Delete messages for a conversation
     */
    @Query("DELETE FROM chat_messages WHERE conversationId = :conversationId")
    void deleteConversationMessages(String conversationId);
    
    /**
     * Delete all messages
     */
    @Query("DELETE FROM chat_messages")
    void deleteAllMessages();
    
    /**
     * Get latest message timestamp for sync
     */
    @Query("SELECT MAX(sentAt) FROM chat_messages WHERE conversationId = :conversationId")
    String getLatestMessageTimestamp(String conversationId);
}
