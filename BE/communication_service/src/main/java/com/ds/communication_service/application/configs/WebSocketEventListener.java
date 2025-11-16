package com.ds.communication_service.application.configs;

import com.ds.communication_service.business.v1.services.WebSocketSessionManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

import java.security.Principal;

/**
 * Event listener for WebSocket connection events
 * Tracks session lifecycle and manages session registry
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketEventListener {

    private final WebSocketSessionManager sessionManager;

    /**
     * Handle WebSocket connection established
     */
    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        Principal user = headerAccessor.getUser();
        
        if (user != null) {
            String userId = user.getName();
            log.info("🔌 WebSocket session connected: userId={}, sessionId={}", userId, sessionId);
            
            // Session is already registered in WebSocketAuthInterceptor during CONNECT
            // This is just for logging and additional setup if needed
        } else {
            log.warn("⚠️ WebSocket session connected but no user principal found: sessionId={}", sessionId);
        }
    }

    /**
     * Handle WebSocket disconnection
     */
    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        Principal user = headerAccessor.getUser();
        
        String userId = null;
        if (user != null) {
            userId = user.getName();
        }
        
        log.info("🔌 WebSocket session disconnected: userId={}, sessionId={}", userId, sessionId);
        
        // Unregister session from session manager
        // Use sessionId lookup if userId is not available
        if (userId != null) {
            sessionManager.unregisterSession(userId, sessionId);
        } else {
            // Try to unregister by sessionId only (session manager will lookup userId)
            sessionManager.unregisterSessionBySessionId(sessionId);
        }
    }

    /**
     * Handle WebSocket subscription
     */
    @EventListener
    public void handleWebSocketSubscribeListener(SessionSubscribeEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        String destination = headerAccessor.getDestination();
        Principal user = headerAccessor.getUser();
        
        if (user != null) {
            String userId = user.getName();
            log.debug("📡 WebSocket subscription: userId={}, sessionId={}, destination={}", 
                userId, sessionId, destination);
        }
    }

    /**
     * Handle WebSocket unsubscription
     */
    @EventListener
    public void handleWebSocketUnsubscribeListener(SessionUnsubscribeEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        Principal user = headerAccessor.getUser();
        
        if (user != null) {
            String userId = user.getName();
            log.debug("📡 WebSocket unsubscription: userId={}, sessionId={}", userId, sessionId);
        }
    }
}
