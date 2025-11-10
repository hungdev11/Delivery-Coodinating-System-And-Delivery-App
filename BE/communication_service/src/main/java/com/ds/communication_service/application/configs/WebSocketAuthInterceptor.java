package com.ds.communication_service.application.configs;

import java.security.Principal;
import java.util.Collections;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    /**
     * Được gọi mỗi khi một tin nhắn (CONNECT, SUBSCRIBE, SEND...) được gửi từ client.
     */
    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        
        // 1. Lấy "phong bì" STOMP để đọc header
        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null) {
            return message;
        }

        StompCommand command = accessor.getCommand();
        
        // Log heartbeat frames (they don't have a command, but we can detect them)
        if (command == null) {
            // This might be a heartbeat frame
            log.debug("WebSocket frame received (possibly heartbeat)");
        } else {
            log.debug("WebSocket command: {}", command);
        }

        // 2. Chúng ta CHỈ quan tâm đến lệnh "CONNECT" và "SUBSCRIBE"
        if (StompCommand.CONNECT.equals(command)) {

            // 3. Đọc header "Authorization" mà client gửi
            // (Client đang gửi: "Bearer <USER_ID>")
            String authHeader = accessor.getFirstNativeHeader("Authorization");

            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                
                // 4. Lấy User ID (là phần sau "Bearer ")
                String userId = authHeader.substring(7);

                if (userId != null && !userId.isBlank()) {
                    
                    // 5. Tạo một đối tượng "Principal" (danh tính)
                    // Chúng ta dùng UsernamePasswordAuthenticationToken vì nó
                    // implement Principal và dễ sử dụng.
                    Principal authToken = new UsernamePasswordAuthenticationToken(
                            userId, // Đây sẽ là giá trị của principal.getName()
                            null,
                            Collections.emptyList() // Không cần quyền (authorities)
                    );

                    // 6. Gán Principal vào session WebSocket này
                    accessor.setUser(authToken);
                    log.info("WebSocket CONNECT: User {} authenticated, Principal name={}", userId, authToken.getName());
                } else {
                    log.warn("WebSocket CONNECT: Empty userId in Authorization header");
                }
            } else {
                log.warn("WebSocket CONNECT: Missing or invalid Authorization header");
            }
        } else if (StompCommand.SUBSCRIBE.equals(command)) {
            // Log subscription attempts
            String destination = accessor.getDestination();
            Principal user = accessor.getUser();
            if (user != null) {
                log.info("WebSocket SUBSCRIBE: User {} subscribing to {}", user.getName(), destination);
            } else {
                log.warn("WebSocket SUBSCRIBE: No Principal found for subscription to {}", destination);
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
            }
        }
    }
}
