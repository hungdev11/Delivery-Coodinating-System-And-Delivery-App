package com.ds.communication_service.application.configs;

import com.ds.communication_service.business.v1.services.WebSocketSessionManager;
import com.ds.communication_service.common.dto.UpdateNotificationDTO;

import java.security.Principal;
import java.util.Collections;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;
import com.ds.communication_service.infrastructure.logging.WebSocketEventLogger;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    @Autowired(required = false)
    @Lazy
    private WebSocketSessionManager sessionManager;

    private final WebSocketEventLogger eventLogger;

    /**
     * Được gọi mỗi khi một tin nhắn (CONNECT, SUBSCRIBE, SEND...) được gửi từ client.
     */
    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        
        // 1. Lấy "phong bì" STOMP để đọc header
        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null) {
            log.debug("WebSocket frame: No StompHeaderAccessor");
            return message;
        }

        StompCommand command = accessor.getCommand();
        
        // Log heartbeat frames (they don't have a command, but we can detect them)
        if (command == null) {
            // This might be a heartbeat frame
            log.debug("WebSocket frame received (possibly heartbeat)");
        } else {
            log.info("🔔 WebSocket command: {} from session: {}", command, accessor.getSessionId());
        }

        // 2. Chúng ta CHỈ quan tâm đến lệnh "CONNECT" và "SUBSCRIBE"
        if (StompCommand.CONNECT.equals(command)) {

            // DEBUG: Log ALL headers from CONNECT frame
            log.info("🔍 STOMP CONNECT received. All native headers: {}", accessor.toNativeHeaderMap());

            // 3. Đọc header "Authorization" mà client gửi
            // (Client đang gửi: "Bearer <USER_ID>")
            String authHeader = accessor.getFirstNativeHeader("Authorization");
            
            // 3a. Đọc X-User-Id và X-User-Roles headers (optional, for consistency)
            String userIdHeader = accessor.getFirstNativeHeader("X-User-Id");
            String userRolesHeader = accessor.getFirstNativeHeader("X-User-Roles");
            
            // 4. Đọc header "Client-Type" để xác định client type (ANDROID, WEB)
            String clientTypeHeader = accessor.getFirstNativeHeader("Client-Type");
            UpdateNotificationDTO.ClientType clientType = UpdateNotificationDTO.ClientType.ALL; // Default to ALL if not specified
            if (clientTypeHeader != null && !clientTypeHeader.isBlank()) {
                try {
                    clientType = UpdateNotificationDTO.ClientType.valueOf(clientTypeHeader.toUpperCase());
                } catch (IllegalArgumentException e) {
                    log.warn("WebSocket CONNECT: Invalid Client-Type header: {}. Using default: ALL", clientTypeHeader);
                    clientType = UpdateNotificationDTO.ClientType.ALL;
                }
            }

            // Determine userId: prefer Authorization header, fallback to X-User-Id
            String userId = null;
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                userId = authHeader.substring(7);
            } else if (userIdHeader != null && !userIdHeader.isBlank()) {
                userId = userIdHeader;
            }

            if (userId != null && !userId.isBlank()) {
                // Log headers for debugging
                if (userRolesHeader != null && !userRolesHeader.isBlank()) {
                    log.info("WebSocket CONNECT: User {} with roles: {}, ClientType={}", 
                        userId, userRolesHeader, clientType);
                } else {
                    log.info("WebSocket CONNECT: User {} authenticated, ClientType={}", 
                        userId, clientType);
                }
                
                // 6. Tạo một đối tượng "Principal" (danh tính)
                // Chúng ta dùng UsernamePasswordAuthenticationToken vì nó
                // implement Principal và dễ sử dụng.
                Principal authToken = new UsernamePasswordAuthenticationToken(
                        userId, // Đây sẽ là giá trị của principal.getName()
                        null,
                        Collections.emptyList() // Không cần quyền (authorities)
                );

                // 7. Gán Principal vào session WebSocket này
                accessor.setUser(authToken);
                
                // 8. Register session với session manager (nếu có)
                if (sessionManager != null && accessor.getSessionId() != null) {
                    sessionManager.registerSession(userId, accessor.getSessionId(), clientType);
                }
                eventLogger.logConnect(userId, accessor.getSessionId(), userRolesHeader, clientType.name());
            } else {
                log.warn("WebSocket CONNECT: Missing or invalid Authorization/X-User-Id header");
            }
        } else if (StompCommand.SUBSCRIBE.equals(command)) {
            // Log subscription attempts
            String destination = accessor.getDestination();
            Principal user = accessor.getUser();
            if (user != null) {
                log.info("WebSocket SUBSCRIBE: User {} subscribing to {}", user.getName(), destination);
                eventLogger.logSubscribe(user.getName(), destination);
            } else {
                log.warn("WebSocket SUBSCRIBE: No Principal found for subscription to {}", destination);
                eventLogger.logSubscribe(null, destination);
            }
        }
        
        // 7. Cho phép tin nhắn tiếp tục được xử lý
        return message;
    }

    /**
     * Called after a message is sent (outgoing messages from server to client)
     */
    @Override
    public void postSend(Message<?> message, MessageChannel channel, boolean sent) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor != null) {
            String destination = accessor.getDestination();
            if (destination != null && destination.contains("/queue/messages")) {
                log.info("📤 WebSocket message sent: destination={}, sent={}", destination, sent);
                Principal user = accessor.getUser();
                String userId = user != null ? user.getName() : null;
                eventLogger.logSend(userId, destination, sent);
            }
        }
    }
}
