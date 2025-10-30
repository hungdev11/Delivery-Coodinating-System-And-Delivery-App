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

@Component
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    /**
     * Được gọi mỗi khi một tin nhắn (CONNECT, SUBSCRIBE, SEND...) được gửi từ client.
     */
    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        
        // 1. Lấy "phong bì" STOMP để đọc header
        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        // 2. Chúng ta CHỈ quan tâm đến lệnh "CONNECT"
        // (Không cần xác thực mỗi tin nhắn, chỉ cần lúc kết nối)
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {

            // 3. Đọc header "Authorization" mà client Android gửi
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
                }
            }
        }
        
        // 7. Cho phép tin nhắn tiếp tục được xử lý
        return message;
    }
}