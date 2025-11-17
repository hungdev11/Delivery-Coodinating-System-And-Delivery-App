package com.ds.communication_service.application.configs;

import com.ds.communication_service.business.v1.services.WebSocketSessionManager;
import com.ds.communication_service.infrastructure.logging.WebSocketEventLogger;
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

@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketEventListener {

    private final WebSocketEventLogger eventLogger;
    private final WebSocketSessionManager sessionManager;

    @EventListener
    public void handleConnect(SessionConnectedEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();
        Principal user = accessor.getUser();
        String userId = user != null ? user.getName() : null;
        String roles = accessor.getFirstNativeHeader("X-User-Roles");
        String clientType = accessor.getFirstNativeHeader("Client-Type");
        log.info("🔌 WebSocket session connected: userId={}, sessionId={}", userId, sessionId);
        eventLogger.logConnect(userId, sessionId, roles, clientType != null ? clientType : "ALL");
    }

    @EventListener
    public void handleDisconnect(SessionDisconnectEvent event) {
        String sessionId = event.getSessionId();
        String userId = event.getUser() != null ? event.getUser().getName() : sessionManager.findUserIdBySession(sessionId);
        log.info("🔌 WebSocket session disconnected: userId={}, sessionId={}", userId, sessionId);
        if (userId != null) {
            sessionManager.unregisterSession(userId, sessionId);
        } else {
            sessionManager.unregisterSessionBySessionId(sessionId);
        }
        eventLogger.logDisconnect(userId, sessionId);
    }

    @EventListener
    public void handleSubscribe(SessionSubscribeEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        Principal user = accessor.getUser();
        String destination = accessor.getDestination();
        if (user != null) {
            log.debug("📡 WebSocket subscription: userId={}, destination={}", user.getName(), destination);
            eventLogger.logSubscribe(user.getName(), destination);
        } else {
            eventLogger.logSubscribe(null, destination);
        }
    }

    @EventListener
    public void handleUnsubscribe(SessionUnsubscribeEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        Principal user = accessor.getUser();
        if (user != null) {
            log.debug("📡 WebSocket unsubscription: userId={}", user.getName());
        }
    }
}
