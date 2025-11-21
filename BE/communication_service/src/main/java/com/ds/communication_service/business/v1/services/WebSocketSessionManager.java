package com.ds.communication_service.business.v1.services;

import com.ds.communication_service.common.dto.UpdateNotificationDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Service to manage WebSocket sessions and track active connections
 * Supports tracking client type (ANDROID, WEB) for filtering messages
 */
@Service
@Slf4j
public class WebSocketSessionManager {

    private final SimpMessagingTemplate messagingTemplate;
    
    /**
     * Map to track client type for each user session
     * Key: userId, Value: Set of client types (ANDROID, WEB)
     */
    private final ConcurrentMap<String, Set<UpdateNotificationDTO.ClientType>> userClientTypes = new ConcurrentHashMap<>();
    
    /**
     * Map to track session IDs for each user
     * Key: userId, Value: Set of session IDs
     */
    private final ConcurrentMap<String, Set<String>> userSessions = new ConcurrentHashMap<>();
    
    /**
     * Map to track userId for each session ID
     * Key: sessionId, Value: userId
     * Used for cleanup when disconnect event doesn't have Principal
     */
    private final ConcurrentMap<String, String> sessionToUser = new ConcurrentHashMap<>();
    
    /**
     * Map to track client type for each session ID
     * Key: sessionId, Value: clientType
     * Used for cleanup when disconnect event doesn't have Principal
     */
    private final ConcurrentMap<String, UpdateNotificationDTO.ClientType> sessionToClientType = new ConcurrentHashMap<>();
    
    public WebSocketSessionManager(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
        log.info("✅ WebSocketSessionManager initialized");
    }
    
    /**
     * Register a WebSocket session for a user
     * Called when a user connects via WebSocket
     */
    public void registerSession(String userId, String sessionId, UpdateNotificationDTO.ClientType clientType) {
        log.info("🔌 Registering WebSocket session: userId={}, sessionId={}, clientType={}", 
            userId, sessionId, clientType);
        
        userClientTypes.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet()).add(clientType);
        userSessions.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet()).add(sessionId);
        sessionToUser.put(sessionId, userId);
        sessionToClientType.put(sessionId, clientType);
        
        log.info("📊 Active sessions for user {}: {}", userId, userSessions.get(userId).size());
        log.info("📱 Client types for user {}: {}", userId, userClientTypes.get(userId));
    }
    
    /**
     * Unregister a WebSocket session for a user
     * Called when a user disconnects from WebSocket
     * Can be called with either userId+sessionId or just sessionId
     */
    public void unregisterSession(String userId, String sessionId) {
        log.info("🔌 Unregistering WebSocket session: userId={}, sessionId={}", userId, sessionId);
        
        // If userId is null, try to get it from sessionToUser map
        if (userId == null || userId.isBlank()) {
            userId = sessionToUser.get(sessionId);
            if (userId == null) {
                log.warn("⚠️ Cannot unregister session {}: userId not found in session registry", sessionId);
                return;
            }
        }
        
        Set<String> sessions = userSessions.get(userId);
        if (sessions != null) {
            sessions.remove(sessionId);
            sessionToClientType.remove(sessionId);
            sessionToUser.remove(sessionId);
            
            if (sessions.isEmpty()) {
                // All sessions closed for this user
                userSessions.remove(userId);
                userClientTypes.remove(userId);
                log.info("📊 All sessions closed for user {}", userId);
            } else {
                // User still has other sessions
                // Check if we need to remove client type (if no other sessions have this client type)
                Set<UpdateNotificationDTO.ClientType> remainingClientTypes = new HashSet<>();
                for (String remainingSessionId : sessions) {
                    UpdateNotificationDTO.ClientType remainingClientType = sessionToClientType.get(remainingSessionId);
                    if (remainingClientType != null) {
                        remainingClientTypes.add(remainingClientType);
                    }
                }
                
                // Update client types set for this user
                userClientTypes.put(userId, remainingClientTypes);
                log.debug("📊 Remaining sessions for user {}: {}, clientTypes={}", userId, sessions.size(), remainingClientTypes);
            }
        }
    }
    
    /**
     * Unregister a WebSocket session by sessionId only
     * Used when disconnect event doesn't have Principal
     */
    public void unregisterSessionBySessionId(String sessionId) {
        String userId = sessionToUser.get(sessionId);
        if (userId != null) {
            unregisterSession(userId, sessionId);
        } else {
            log.warn("⚠️ Cannot unregister session {}: userId not found", sessionId);
        }
    }

    public String findUserIdBySession(String sessionId) {
        return sessionToUser.get(sessionId);
    }
    
    /**
     * Check if a user has active WebSocket connections
     */
    public boolean isUserConnected(String userId) {
        return userSessions.containsKey(userId) && !userSessions.get(userId).isEmpty();
    }
    
    /**
     * Check if a user is online (has active WebSocket connections)
     * Alias for isUserConnected for better readability
     */
    public Boolean isUserOnline(String userId) {
        if (userId == null || userId.isBlank()) {
            return null;
        }
        return isUserConnected(userId);
    }
    
    /**
     * Get client types for a user
     */
    public Set<UpdateNotificationDTO.ClientType> getClientTypes(String userId) {
        return userClientTypes.getOrDefault(userId, new HashSet<>());
    }
    
    /**
     * Check if a user has a specific client type connected
     */
    public boolean hasClientType(String userId, UpdateNotificationDTO.ClientType clientType) {
        if (clientType == UpdateNotificationDTO.ClientType.ALL) {
            return isUserConnected(userId);
        }
        return userClientTypes.containsKey(userId) && userClientTypes.get(userId).contains(clientType);
    }
    
    /**
     * Get number of active sessions for a user
     */
    public int getActiveSessionCount(String userId) {
        return userSessions.getOrDefault(userId, new HashSet<>()).size();
    }
    
    /**
     * Get all connected user IDs
     */
    public Set<String> getConnectedUsers() {
        return new HashSet<>(userSessions.keySet());
    }
    
    /**
     * Send message to a user if they have the specified client type (or ALL)
     */
    public void sendToUserIfClientType(String userId, String destination, Object payload, UpdateNotificationDTO.ClientType requiredClientType) {
        if (requiredClientType == UpdateNotificationDTO.ClientType.ALL) {
            // Send to all client types
            messagingTemplate.convertAndSendToUser(userId, destination, payload);
            return;
        }
        
        // Check if user has the required client type
        if (hasClientType(userId, requiredClientType)) {
            messagingTemplate.convertAndSendToUser(userId, destination, payload);
        } else {
            log.debug("⏭️ Skipping send to user {}: required clientType={}, user has={}", 
                userId, requiredClientType, getClientTypes(userId));
        }
    }
}
