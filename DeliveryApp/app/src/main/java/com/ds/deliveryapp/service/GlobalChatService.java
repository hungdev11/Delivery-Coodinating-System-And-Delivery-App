package com.ds.deliveryapp.service;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import com.ds.deliveryapp.auth.AuthManager;
import com.ds.deliveryapp.clients.req.ProposalUpdateDTO;
import com.ds.deliveryapp.clients.res.Message;
import com.ds.deliveryapp.clients.res.UpdateNotification;
import com.ds.deliveryapp.utils.ChatWebSocketListener;
import com.ds.deliveryapp.utils.ChatWebSocketManager;
import com.ds.deliveryapp.enums.ContentType;
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
    private static final String SERVER_WEBSOCKET_URL = "wss://localweb.phuongy.works/ws/websocket";

    private static GlobalChatService instance;
    private ChatWebSocketManager webSocketManager;
    private AuthManager authManager;
    private Context context;
    private Gson gson;

    // Unread message tracking per conversation
    private java.util.Map<String, Integer> unreadCountPerConversation = new java.util.concurrent.ConcurrentHashMap<>();
    private List<Message> pendingProposals = new ArrayList<>();
    
    // Track processed message IDs to avoid double counting
    private java.util.Set<String> processedMessageIds = java.util.concurrent.ConcurrentHashMap.newKeySet();

    // Listeners
    private List<GlobalChatListener> listeners = new ArrayList<>();
    private List<ProposalListener> proposalListeners = new ArrayList<>();

    private GlobalChatService(Context context) {
        this.context = context.getApplicationContext();
        this.authManager = new AuthManager(context);
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

        if (jwtToken == null || userId == null || userId.isEmpty()) {
            Log.w(TAG, "Cannot initialize: Missing token or userId");
            return;
        }

        if (webSocketManager != null && webSocketManager.isConnected()) {
            Log.d(TAG, "WebSocket already connected");
            return;
        }

        Log.d(TAG, "Initializing GlobalChatService...");
        webSocketManager = new ChatWebSocketManager(SERVER_WEBSOCKET_URL, jwtToken, userId);
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

        // Handle proposal messages specially
        if (message.getType() == ContentType.INTERACTIVE_PROPOSAL) {
            handleProposalMessage(message);
        } else {
            // Normal message - increment unread count for this conversation
            String conversationId = message.getConversationId();
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
        // Handle status updates if needed
    }

    @Override
    public void onTypingIndicatorReceived(String typingJson) {
        // Handle typing indicators if needed
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
     * Interface for global chat events
     */
    public interface GlobalChatListener {
        void onMessageReceived(Message message);
        void onUnreadCountChanged(int count);
        void onConnectionStatusChanged(boolean connected);
        void onError(String error);
        void onNotificationReceived(String notificationJson);
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
