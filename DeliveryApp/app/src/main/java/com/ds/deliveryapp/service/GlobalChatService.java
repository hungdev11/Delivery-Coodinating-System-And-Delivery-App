package com.ds.deliveryapp.service;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import com.ds.deliveryapp.auth.AuthManager;
import com.ds.deliveryapp.clients.req.ProposalUpdateDTO;
import com.ds.deliveryapp.clients.res.Message;
import com.ds.deliveryapp.clients.res.UpdateNotification;
import com.ds.deliveryapp.configs.ServerConfigManager;
import com.ds.deliveryapp.utils.ChatWebSocketListener;
import com.ds.deliveryapp.utils.ChatWebSocketManager;
import com.ds.deliveryapp.enums.ContentType;
import com.ds.deliveryapp.repository.ChatHistoryRepository;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Global singleton service to manage WebSocket connection app-wide.
 * Listens to all incoming messages and tracks unread count.
 */
public class GlobalChatService implements ChatWebSocketListener {
    private static final String TAG = "GlobalChatService";

    private static GlobalChatService instance;
    private ChatWebSocketManager webSocketManager;
    private AuthManager authManager;
    private Context context;
    private Gson gson;
    private ChatHistoryRepository chatHistoryRepository;

    // Unread message tracking per conversation
    private java.util.Map<String, Integer> unreadCountPerConversation = new java.util.concurrent.ConcurrentHashMap<>();
    private List<Message> pendingProposals = new ArrayList<>();
    
    // In-memory message storage for all conversations (similar to web's chatStore)
    private java.util.Map<String, java.util.List<Message>> messagesByConversation = new java.util.concurrent.ConcurrentHashMap<>();
    
    // Conversations metadata (partner info, online status, etc.)
    private java.util.Map<String, com.ds.deliveryapp.clients.res.Conversation> conversationsMetadata = new java.util.concurrent.ConcurrentHashMap<>();
    
    // Track processed message IDs to avoid double counting
    private java.util.Set<String> processedMessageIds = java.util.concurrent.ConcurrentHashMap.newKeySet();

    // Listeners
    private List<GlobalChatListener> listeners = new ArrayList<>();
    private List<ProposalListener> proposalListeners = new ArrayList<>();

    private GlobalChatService(Context context) {
        this.context = context.getApplicationContext();
        this.authManager = new AuthManager(context);
        this.chatHistoryRepository = new ChatHistoryRepository(this.context);
        // Initialize Gson with LocalDateTime support
        this.gson = new GsonBuilder()
                .setDateFormat("yyyy-MM-dd'T'HH:mm:ss")
                .create();
    }

    public static synchronized GlobalChatService getInstance(Context context) {
        if (instance == null) {
            instance = new GlobalChatService(context);
        }
        return instance;
    }

    /**
     * Initialize and connect WebSocket
     */
    public void initialize() {
        String jwtToken = authManager.getAccessToken();
        String userId = authManager.getUserId();
        List<String> userRoles = authManager.getRoles();

        if (jwtToken == null || userId == null || userId.isEmpty()) {
            Log.w(TAG, "Cannot initialize: Missing token or userId");
            return;
        }

        if (webSocketManager != null && webSocketManager.isConnected()) {
            Log.d(TAG, "WebSocket already connected");
            return;
        }

        // Get WebSocket URL from ServerConfigManager
        String webSocketUrl = ServerConfigManager.getInstance(context).getWebSocketUrl();
        Log.d(TAG, "Initializing GlobalChatService with WebSocket URL: " + webSocketUrl);
        webSocketManager = new ChatWebSocketManager(webSocketUrl, jwtToken, userId, userRoles);
        webSocketManager.setListener(this);
        webSocketManager.connect();
        
        // Subscribe to session messages if user is a shipper
        // This will be done after connection is established (handled in ChatWebSocketManager)
        // We'll subscribe after a delay to ensure connection is ready
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            if (webSocketManager != null && webSocketManager.isConnected()) {
                // Check if user is shipper and subscribe to session messages
                // For now, subscribe for all users (can be filtered later)
                webSocketManager.subscribeToSessionMessages();
                Log.d(TAG, "Subscribed to session messages");
            }
        }, 2000); // Wait 2 seconds for connection to stabilize
    }

    /**
     * Disconnect WebSocket
     */
    public void disconnect() {
        if (webSocketManager != null) {
            webSocketManager.disconnect();
            webSocketManager = null;
        }
    }

    /**
     * Check if WebSocket is connected
     */
    public boolean isConnected() {
        return webSocketManager != null && webSocketManager.isConnected();
    }

    /**
     * Get total unread message count across all conversations
     */
    public int getUnreadMessageCount() {
        return unreadCountPerConversation.values().stream()
                .mapToInt(Integer::intValue)
                .sum();
    }

    /**
     * Get unread count for a specific conversation
     */
    public int getUnreadCountForConversation(String conversationId) {
        return unreadCountPerConversation.getOrDefault(conversationId, 0);
    }

    /**
     * Clear unread count for a specific conversation (when user opens chat)
     */
    public void clearUnreadCountForConversation(String conversationId) {
        if (conversationId != null && unreadCountPerConversation.containsKey(conversationId)) {
            unreadCountPerConversation.put(conversationId, 0);
            notifyUnreadCountChanged();
        }
    }

    /**
     * Clear all unread counts (when user opens conversations list)
     */
    public void clearAllUnreadCounts() {
        unreadCountPerConversation.clear();
        notifyUnreadCountChanged();
    }

    /**
     * Update unread count for a conversation (sync with backend)
     */
    public void updateUnreadCountForConversation(String conversationId, int count) {
        if (conversationId != null) {
            if (count > 0) {
                unreadCountPerConversation.put(conversationId, count);
            } else {
                unreadCountPerConversation.remove(conversationId);
            }
            notifyUnreadCountChanged();
        }
    }

    /**
     * Sync unread counts from backend conversation list
     */
    public void syncUnreadCounts(java.util.Map<String, Integer> counts) {
        if (counts != null) {
            unreadCountPerConversation.clear();
            unreadCountPerConversation.putAll(counts);
            notifyUnreadCountChanged();
        }
    }

    /**
     * Register listener for global chat events
     */
    public void addListener(GlobalChatListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    /**
     * Unregister listener
     */
    public void removeListener(GlobalChatListener listener) {
        listeners.remove(listener);
    }

    /**
     * Register listener for proposal messages
     */
    public void addProposalListener(ProposalListener listener) {
        if (listener != null && !proposalListeners.contains(listener)) {
            proposalListeners.add(listener);
        }
    }

    /**
     * Unregister proposal listener
     */
    public void removeProposalListener(ProposalListener listener) {
        proposalListeners.remove(listener);
    }

    // ChatWebSocketListener implementation
    @Override
    public void onWebSocketOpened() {
        Log.d(TAG, "WebSocket opened");
        for (GlobalChatListener listener : listeners) {
            listener.onConnectionStatusChanged(true);
        }
    }

    @Override
    public void onWebSocketClosed() {
        Log.d(TAG, "WebSocket closed");
        for (GlobalChatListener listener : listeners) {
            listener.onConnectionStatusChanged(false);
        }
    }

    @Override
    public void onWebSocketError(String error) {
        Log.e(TAG, "WebSocket error: " + error);
        for (GlobalChatListener listener : listeners) {
            listener.onError(error);
        }
    }

    @Override
    public void onMessageReceived(Message message) {
        if (message == null || message.getId() == null) return;

        // Atomically check and mark message as processed (avoid double counting from multiple queues)
        // Set.add() returns false if the element was already present
        boolean wasNew = processedMessageIds.add(message.getId());
        if (!wasNew) {
            Log.d(TAG, "Message " + message.getId() + " already processed, skipping duplicate");
            return;
        }
        
        // Limit processed message IDs set size to prevent memory leak (keep last 1000)
        if (processedMessageIds.size() > 1000) {
            // Remove oldest entries (simple approach: clear and start over)
            // In production, use a LRU cache or timestamp-based cleanup
            processedMessageIds.clear();
            Log.d(TAG, "Cleared processed message IDs cache to prevent memory leak");
        }

        Log.d(TAG, "Global message received: " + message.getId() + ", type: " + message.getType());

        // Save message to local database
        if (chatHistoryRepository != null) {
            chatHistoryRepository.saveMessage(message);
            Log.d(TAG, "Message saved to local database: " + message.getId());
        }
        
        // Add message to in-memory store
        String conversationId = message.getConversationId();
        if (conversationId != null) {
            java.util.List<Message> conversationMessages = messagesByConversation.computeIfAbsent(conversationId, k -> new java.util.ArrayList<>());
            
            // Check if message already exists (avoid duplicates)
            boolean exists = conversationMessages.stream().anyMatch(m -> m.getId() != null && m.getId().equals(message.getId()));
            if (!exists) {
                conversationMessages.add(message);
                // Sort by sentAt (oldest first)
                conversationMessages.sort((m1, m2) -> {
                    if (m1.getSentAt() == null || m2.getSentAt() == null) return 0;
                    return m1.getSentAt().compareTo(m2.getSentAt());
                });
                Log.d(TAG, "Message added to in-memory store for conversation " + conversationId + ". Total: " + conversationMessages.size());
            }
        }

        // Handle proposal messages specially
        if (message.getType() == ContentType.INTERACTIVE_PROPOSAL) {
            handleProposalMessage(message);
        } else {
            // Normal message - increment unread count for this conversation
            // Use conversationId already defined above
            if (conversationId != null) {
                // Only increment if message is not from current user (we assume messages from current user are not counted)
                String currentUserId = authManager.getUserId();
                if (currentUserId != null && !currentUserId.equals(message.getSenderId())) {
                    // Increment unread count for this conversation
                    int currentCount = unreadCountPerConversation.getOrDefault(conversationId, 0);
                    unreadCountPerConversation.put(conversationId, currentCount + 1);
                    notifyUnreadCountChanged();
                    Log.d(TAG, "Incremented unread count for conversation " + conversationId + ". New count: " + (currentCount + 1));
                }
            }
        }

        // Notify all listeners
        for (GlobalChatListener listener : listeners) {
            listener.onMessageReceived(message);
        }
    }

    @Override
    public void onProposalUpdateReceived(ProposalUpdateDTO update) {
        Log.d(TAG, "Proposal update received: " + update.getProposalId());
        for (ProposalListener listener : proposalListeners) {
            listener.onProposalUpdate(update);
        }
    }

    @Override
    public void onStatusUpdateReceived(String statusJson) {
        Log.d(TAG, "📡 Status update received: " + statusJson);
        
        try {
            // Parse status update JSON
            // Expected format: {"userId": "xxx", "isOnline": true/false}
            java.util.Map<String, Object> statusUpdate = gson.fromJson(statusJson, java.util.Map.class);
            
            if (statusUpdate != null && statusUpdate.containsKey("userId") && statusUpdate.containsKey("isOnline")) {
                String userId = (String) statusUpdate.get("userId");
                Boolean isOnline = null;
                
                Object isOnlineObj = statusUpdate.get("isOnline");
                if (isOnlineObj instanceof Boolean) {
                    isOnline = (Boolean) isOnlineObj;
                } else if (isOnlineObj instanceof String) {
                    isOnline = Boolean.parseBoolean((String) isOnlineObj);
                } else if (isOnlineObj != null) {
                    isOnline = Boolean.parseBoolean(isOnlineObj.toString());
                }
                
                if (userId != null && isOnline != null) {
                    Log.d(TAG, "📡 User " + userId + " is now " + (isOnline ? "online" : "offline"));
                    
                    // Update conversation metadata if this user is a partner in any conversation
                    for (java.util.Map.Entry<String, com.ds.deliveryapp.clients.res.Conversation> entry : conversationsMetadata.entrySet()) {
                        com.ds.deliveryapp.clients.res.Conversation conv = entry.getValue();
                        if (conv != null && userId.equals(conv.getPartnerId())) {
                            conv.setPartnerOnline(isOnline);
                            Log.d(TAG, "✅ Updated online status for conversation " + entry.getKey() + " (partner: " + userId + ")");
                        }
                    }
                    
                    // Notify all listeners
                    for (GlobalChatListener listener : listeners) {
                        try {
                            listener.onUserStatusUpdate(userId, isOnline);
                        } catch (Exception e) {
                            Log.e(TAG, "Error notifying listener of user status update", e);
                        }
                    }
                } else {
                    Log.w(TAG, "⚠️ Invalid status update format: missing userId or isOnline");
                }
            } else {
                Log.w(TAG, "⚠️ Status update missing required fields: " + statusJson);
            }
        } catch (Exception e) {
            Log.e(TAG, "❌ Error parsing status update JSON: " + statusJson, e);
        }
    }

    @Override
    public void onTypingIndicatorReceived(String typingJson) {
        Log.d(TAG, "⌨️ Typing indicator received: " + typingJson);
        
        try {
            // Parse typing indicator JSON
            // Expected format: {"userId": "xxx", "conversationId": "yyy", "isTyping": true/false}
            java.util.Map<String, Object> typingIndicator = gson.fromJson(typingJson, java.util.Map.class);
            
            if (typingIndicator != null && typingIndicator.containsKey("userId") && typingIndicator.containsKey("isTyping")) {
                String userId = (String) typingIndicator.get("userId");
                String conversationId = (String) typingIndicator.get("conversationId");
                Boolean isTyping = null;
                
                Object isTypingObj = typingIndicator.get("isTyping");
                if (isTypingObj instanceof Boolean) {
                    isTyping = (Boolean) isTypingObj;
                } else if (isTypingObj instanceof String) {
                    isTyping = Boolean.parseBoolean((String) isTypingObj);
                } else if (isTypingObj != null) {
                    isTyping = Boolean.parseBoolean(isTypingObj.toString());
                }
                
                if (userId != null && isTyping != null) {
                    Log.d(TAG, "⌨️ User " + userId + " is " + (isTyping ? "typing" : "stopped typing") + " in conversation " + conversationId);
                    
                    // Notify all listeners
                    for (GlobalChatListener listener : listeners) {
                        try {
                            listener.onTypingIndicatorUpdate(userId, conversationId, isTyping);
                        } catch (Exception e) {
                            Log.e(TAG, "Error notifying listener of typing indicator update", e);
                        }
                    }
                } else {
                    Log.w(TAG, "⚠️ Invalid typing indicator format: missing userId or isTyping");
                }
            } else {
                Log.w(TAG, "⚠️ Typing indicator missing required fields: " + typingJson);
            }
        } catch (Exception e) {
            Log.e(TAG, "❌ Error parsing typing indicator JSON: " + typingJson, e);
        }
    }

    @Override
    public void onNotificationReceived(String notificationJson) {
        Log.d(TAG, "Notification received: " + notificationJson);
        for (GlobalChatListener listener : listeners) {
            listener.onNotificationReceived(notificationJson);
        }
    }

    @Override
    public void onSessionMessageReceived(Message message) {
        // Handle session messages - treat them like regular messages for global listening
        Log.d(TAG, "Session message received: " + message.getId());
        if (message != null) {
            // Forward to message handlers
            onMessageReceived(message);
        }
    }
    
    @Override
    public void onUpdateNotificationReceived(String updateNotificationJson) {
        Log.d(TAG, "📥 Update notification received: " + updateNotificationJson);
        
        try {
            // Parse update notification JSON
            UpdateNotification updateNotification = gson.fromJson(updateNotificationJson, UpdateNotification.class);
            
            if (updateNotification == null) {
                Log.w(TAG, "⚠️ Failed to parse update notification JSON");
                return;
            }
            
            Log.i(TAG, String.format("📋 Update notification: type=%s, entityType=%s, entityId=%s, action=%s, userId=%s", 
                updateNotification.getUpdateType(), 
                updateNotification.getEntityType(), 
                updateNotification.getEntityId(), 
                updateNotification.getAction(), 
                updateNotification.getUserId()));
            
            // Notify all listeners about update notification
            for (GlobalChatListener listener : listeners) {
                try {
                    if (listener instanceof UpdateNotificationListener) {
                        ((UpdateNotificationListener) listener).onUpdateNotificationReceived(updateNotification);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error notifying listener of update notification", e);
                }
            }
            
        } catch (Exception e) {
            Log.e(TAG, "❌ Error parsing update notification JSON: " + e.getMessage(), e);
        }
    }

    private void handleProposalMessage(Message message) {
        Log.d(TAG, "Handling proposal message: " + message.getId());
        pendingProposals.add(message);

        // Notify proposal listeners
        for (ProposalListener listener : proposalListeners) {
            listener.onProposalReceived(message);
        }

        // Auto-show popup for specific proposal types
        if (message.getProposal() != null) {
            String proposalType = message.getProposal().getType();
            
            // Show DisputeAppealDialog for DISPUTE_APPEAL type
            if ("DISPUTE_APPEAL".equals(proposalType)) {
                showDisputeAppealDialog(message);
            } else {
                // Show generic ProposalPopupDialog for other types
                showProposalPopup(message);
            }
        }
    }

    private void notifyUnreadCountChanged() {
        int totalUnread = getUnreadMessageCount();
        Log.d(TAG, "Notifying unread count changed: total=" + totalUnread + ", listeners=" + listeners.size() + 
              ", conversationCounts=" + unreadCountPerConversation);
        for (GlobalChatListener listener : listeners) {
            try {
                listener.onUnreadCountChanged(totalUnread);
            } catch (Exception e) {
                Log.e(TAG, "Error notifying listener of unread count change", e);
            }
        }
    }

    /**
     * Get ChatWebSocketManager instance (for sending messages)
     */
    public ChatWebSocketManager getWebSocketManager() {
        return webSocketManager;
    }
    
    /**
     * Set messages for a conversation (initial load from API)
     * This replaces existing messages for the conversation
     */
    public void setMessagesForConversation(String conversationId, java.util.List<Message> messages) {
        if (conversationId == null || messages == null) return;
        
        // Sort messages by sentAt (oldest first)
        java.util.List<Message> sortedMessages = new java.util.ArrayList<>(messages);
        sortedMessages.sort((m1, m2) -> {
            if (m1.getSentAt() == null || m2.getSentAt() == null) return 0;
            return m1.getSentAt().compareTo(m2.getSentAt());
        });
        
        messagesByConversation.put(conversationId, sortedMessages);
        Log.d(TAG, "Set " + sortedMessages.size() + " messages for conversation " + conversationId);
    }
    
    /**
     * Get messages for a conversation from in-memory store
     */
    public java.util.List<Message> getMessagesForConversation(String conversationId) {
        if (conversationId == null) return new java.util.ArrayList<>();
        return new java.util.ArrayList<>(messagesByConversation.getOrDefault(conversationId, new java.util.ArrayList<>()));
    }
    
    /**
     * Set conversation metadata (partner info, online status, etc.)
     */
    public void setConversationMetadata(com.ds.deliveryapp.clients.res.Conversation conversation) {
        if (conversation != null && conversation.getConversationId() != null) {
            conversationsMetadata.put(conversation.getConversationId(), conversation);
            Log.d(TAG, "Set metadata for conversation " + conversation.getConversationId());
        }
    }
    
    /**
     * Set multiple conversations metadata
     */
    public void setConversationsMetadata(java.util.List<com.ds.deliveryapp.clients.res.Conversation> conversations) {
        if (conversations == null) return;
        for (com.ds.deliveryapp.clients.res.Conversation conv : conversations) {
            setConversationMetadata(conv);
        }
        Log.d(TAG, "Set metadata for " + conversations.size() + " conversations");
    }
    
    /**
     * Get conversation metadata
     */
    public com.ds.deliveryapp.clients.res.Conversation getConversationMetadata(String conversationId) {
        if (conversationId == null) return null;
        return conversationsMetadata.get(conversationId);
    }
    
    /**
     * Clear messages for a conversation (when manually refreshed)
     */
    public void clearMessagesForConversation(String conversationId) {
        if (conversationId != null) {
            messagesByConversation.remove(conversationId);
            Log.d(TAG, "Cleared messages for conversation " + conversationId);
        }
    }

    /**
     * Interface for global chat events
     */
    public interface GlobalChatListener {
        void onMessageReceived(Message message);
        void onUnreadCountChanged(int count);
        void onConnectionStatusChanged(boolean connected);
        void onError(String error);
        void onNotificationReceived(String notificationJson);
        void onUserStatusUpdate(String userId, boolean isOnline);
        void onTypingIndicatorUpdate(String userId, String conversationId, boolean isTyping);
    }

    /**
     * Interface for update notification events
     * Extends GlobalChatListener to handle update notifications from other services
     */
    public interface UpdateNotificationListener extends GlobalChatListener {
        /**
         * Called when an update notification is received
         * Clients should refresh data based on the update type and entity
         */
        void onUpdateNotificationReceived(UpdateNotification updateNotification);
    }

    /**
     * Interface for proposal-specific events
     */
    public interface ProposalListener {
        void onProposalReceived(Message proposalMessage);
        void onProposalUpdate(ProposalUpdateDTO update);
    }
}
