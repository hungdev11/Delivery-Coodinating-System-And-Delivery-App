package com.ds.communication_service.application.configs;

import com.ds.communication_service.app_context.repositories.ConversationRepository;
import com.ds.communication_service.business.v1.services.WebSocketSessionManager;
import com.ds.communication_service.common.dto.UpdateNotificationDTO;
import com.ds.communication_service.infrastructure.logging.WebSocketEventLogger;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketEventListener {

    private final WebSocketEventLogger eventLogger;
    private final WebSocketSessionManager sessionManager;
    private final ConversationRepository conversationRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @EventListener
    public void handleConnect(SessionConnectedEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();
        Principal user = accessor.getUser();
        String userId = user != null ? user.getName() : null;
        String roles = accessor.getFirstNativeHeader("X-User-Roles");
        String clientTypeStr = accessor.getFirstNativeHeader("Client-Type");
        UpdateNotificationDTO.ClientType clientType = clientTypeStr != null 
                ? UpdateNotificationDTO.ClientType.valueOf(clientTypeStr) 
                : UpdateNotificationDTO.ClientType.ALL;
        
        log.debug("[communication-service] [WebSocketEventListener.handleConnect] WebSocket session connected: userId={}, sessionId={}", userId, sessionId);
        eventLogger.logConnect(userId, sessionId, roles, clientTypeStr != null ? clientTypeStr : "ALL");
        
        // Register session
        if (userId != null) {
            sessionManager.registerSession(userId, sessionId, clientType);
            
            // Check if this is the first session for this user (user just came online)
            boolean wasOffline = !sessionManager.isUserConnected(userId) || sessionManager.getActiveSessionCount(userId) == 1;
            
            if (wasOffline) {
                // Broadcast user online status to all users who have conversations with this user
                broadcastUserStatusUpdate(userId, true);
            }
        }
    }

    @EventListener
    public void handleDisconnect(SessionDisconnectEvent event) {
        String sessionId = event.getSessionId();
        Principal user = event.getUser();
        String userId = (user != null && user.getName() != null) 
                ? user.getName() 
                : sessionManager.findUserIdBySession(sessionId);
        log.debug("[communication-service] [WebSocketEventListener.handleDisconnect] WebSocket session disconnected: userId={}, sessionId={}", userId, sessionId);
        
        if (userId != null) {
            // Check if user will be offline after this disconnect
            int activeSessionsBefore = sessionManager.getActiveSessionCount(userId);
            boolean willBeOffline = activeSessionsBefore <= 1;
            
            // Unregister session
            sessionManager.unregisterSession(userId, sessionId);
            
            // If user is now offline (no more active sessions), broadcast offline status
            if (willBeOffline && !sessionManager.isUserConnected(userId)) {
                broadcastUserStatusUpdate(userId, false);
            }
        } else {
            sessionManager.unregisterSessionBySessionId(sessionId);
        }
        
        eventLogger.logDisconnect(userId, sessionId);
    }
    
    /**
     * Broadcast user online/offline status update to all users who have conversations with this user
     */
    private void broadcastUserStatusUpdate(String userId, boolean isOnline) {
        try {
            // Find all conversations involving this user
            java.util.List<com.ds.communication_service.app_context.models.Conversation> conversations = 
                    conversationRepository.findAllByUserId(userId);
            
            if (conversations == null || conversations.isEmpty()) {
                log.debug("[communication-service] [WebSocketEventListener.broadcastUserStatusUpdate] No conversations found for user {}", userId);
                return;
            }
            
            // Create status update payload
            Map<String, Object> statusUpdate = new HashMap<>();
            statusUpdate.put("userId", userId);
            statusUpdate.put("isOnline", isOnline);
            statusUpdate.put("timestamp", java.time.LocalDateTime.now().toString());
            
            // Broadcast to all partner users
            for (com.ds.communication_service.app_context.models.Conversation conv : conversations) {
                String partnerId = conv.getUser1Id().equals(userId) 
                        ? conv.getUser2Id() 
                        : conv.getUser1Id();
                
                if (partnerId != null && !partnerId.isBlank()) {
                    // Send status update to partner via WebSocket
                    // Destination: /user/{partnerId}/queue/status-updates
                    messagingTemplate.convertAndSendToUser(
                            partnerId, 
                            "/queue/status-updates", 
                            statusUpdate
                    );
                    
                    log.debug("[communication-service] [WebSocketEventListener.broadcastUserStatusUpdate] Broadcasted {} status for user {} to partner {}", 
                            isOnline ? "online" : "offline", userId, partnerId);
                }
            }
        } catch (Exception e) {
            log.error("[communication-service] [WebSocketEventListener.broadcastUserStatusUpdate] Error broadcasting status update for user {}", userId, e);
        }
    }

    @EventListener
    public void handleSubscribe(SessionSubscribeEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        Principal user = accessor.getUser();
        String destination = accessor.getDestination();
        if (user != null) {
            log.debug("[communication-service] [WebSocketEventListener.handleSubscribe] WebSocket subscription: userId={}, destination={}", user.getName(), destination);
            eventLogger.logSubscribe(user.getName(), destination);
        } else {
            eventLogger.logSubscribe(null, destination);
        }
    }

    @EventListener
    public void handleUnsubscribe(SessionUnsubscribeEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        Principal user = accessor.getUser();
        if (user != null && user.getName() != null) {
            log.debug("[communication-service] [WebSocketEventListener.handleUnsubscribe] WebSocket unsubscription: userId={}", user.getName());
        }
    }
}
