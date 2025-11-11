package com.ds.deliveryapp.repository;

import android.content.Context;
import android.util.Log;

import com.ds.deliveryapp.clients.res.InteractiveProposal;
import com.ds.deliveryapp.clients.res.Message;
import com.ds.deliveryapp.database.ChatDatabase;
import com.ds.deliveryapp.database.dao.ChatMessageDao;
import com.ds.deliveryapp.database.entities.ChatMessageEntity;
import com.ds.deliveryapp.enums.ContentType;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Repository for managing chat history with local database
 */
public class ChatHistoryRepository {
    
    private static final String TAG = "ChatHistoryRepository";
    private final ChatMessageDao chatMessageDao;
    private final ExecutorService executorService;
    private final Gson gson;
    
    public ChatHistoryRepository(Context context) {
        ChatDatabase database = ChatDatabase.getInstance(context);
        this.chatMessageDao = database.chatMessageDao();
        this.executorService = Executors.newSingleThreadExecutor();
        this.gson = new Gson();
    }
    
    /**
     * Save a message to local database
     */
    public void saveMessage(Message message) {
        executorService.execute(() -> {
            try {
                ChatMessageEntity entity = messageToEntity(message);
                chatMessageDao.insertMessage(entity);
                Log.d(TAG, "Message saved to local database: " + message.getId());
            } catch (Exception e) {
                Log.e(TAG, "Error saving message to database", e);
            }
        });
    }
    
    /**
     * Save multiple messages to local database
     */
    public void saveMessages(List<Message> messages) {
        executorService.execute(() -> {
            try {
                List<ChatMessageEntity> entities = new ArrayList<>();
                for (Message message : messages) {
                    entities.add(messageToEntity(message));
                }
                chatMessageDao.insertMessages(entities);
                Log.d(TAG, "Saved " + messages.size() + " messages to local database");
            } catch (Exception e) {
                Log.e(TAG, "Error saving messages to database", e);
            }
        });
    }
    
    /**
     * Get messages for a conversation from local database
     */
    public void getMessagesForConversation(String conversationId, OnMessagesLoadedListener listener) {
        executorService.execute(() -> {
            try {
                List<ChatMessageEntity> entities = chatMessageDao.getMessagesForConversation(conversationId);
                List<Message> messages = new ArrayList<>();
                for (ChatMessageEntity entity : entities) {
                    messages.add(entityToMessage(entity));
                }
                Log.d(TAG, "Loaded " + messages.size() + " messages from local database");
                listener.onMessagesLoaded(messages);
            } catch (Exception e) {
                Log.e(TAG, "Error loading messages from database", e);
                listener.onError(e);
            }
        });
    }
    
    /**
     * Update message status in local database
     */
    public void updateMessageStatus(String messageId, String status) {
        executorService.execute(() -> {
            try {
                String deliveredAt = status.equals("DELIVERED") ? String.valueOf(System.currentTimeMillis()) : null;
                String readAt = status.equals("READ") ? String.valueOf(System.currentTimeMillis()) : null;
                chatMessageDao.updateMessageStatus(messageId, status, deliveredAt, readAt);
                Log.d(TAG, "Updated message status in local database: " + messageId + " -> " + status);
            } catch (Exception e) {
                Log.e(TAG, "Error updating message status in database", e);
            }
        });
    }
    
    /**
     * Delete all messages for a conversation
     */
    public void deleteConversationMessages(String conversationId) {
        executorService.execute(() -> {
            try {
                chatMessageDao.deleteConversationMessages(conversationId);
                Log.d(TAG, "Deleted messages for conversation: " + conversationId);
            } catch (Exception e) {
                Log.e(TAG, "Error deleting conversation messages", e);
            }
        });
    }
    
    /**
     * Get latest message timestamp for sync
     */
    public void getLatestMessageTimestamp(String conversationId, OnTimestampLoadedListener listener) {
        executorService.execute(() -> {
            try {
                String timestamp = chatMessageDao.getLatestMessageTimestamp(conversationId);
                listener.onTimestampLoaded(timestamp);
            } catch (Exception e) {
                Log.e(TAG, "Error getting latest timestamp", e);
                listener.onError(e);
            }
        });
    }
    
    // Conversion helpers
    private ChatMessageEntity messageToEntity(Message message) {
        String proposalJson = null;
        if (message.getProposal() != null) {
            proposalJson = gson.toJson(message.getProposal());
        }
        
        return new ChatMessageEntity(
                message.getId(),
                message.getConversationId(),
                message.getSenderId(),
                message.getContent(),
                message.getSentAt(),
                message.getType() != null ? message.getType().name() : ContentType.TEXT.name(),
                message.getStatus(),
                message.getDeliveredAt(),
                message.getReadAt(),
                proposalJson
        );
    }
    
    private Message entityToMessage(ChatMessageEntity entity) {
        InteractiveProposal proposal = null;
        if (entity.getProposalJson() != null && !entity.getProposalJson().isEmpty()) {
            proposal = gson.fromJson(entity.getProposalJson(), InteractiveProposal.class);
        }
        
        return new Message(
                entity.getId(),
                entity.getConversationId(),
                entity.getSenderId(),
                entity.getContent(),
                entity.getSentAt(),
                ContentType.valueOf(entity.getType()),
                entity.getStatus(),
                entity.getDeliveredAt(),
                entity.getReadAt(),
                proposal
        );
    }
    
    // Listener interfaces
    public interface OnMessagesLoadedListener {
        void onMessagesLoaded(List<Message> messages);
        void onError(Exception e);
    }
    
    public interface OnTimestampLoadedListener {
        void onTimestampLoaded(String timestamp);
        void onError(Exception e);
    }
    
    /**
     * Shutdown executor service
     */
    public void shutdown() {
        executorService.shutdown();
    }
}
