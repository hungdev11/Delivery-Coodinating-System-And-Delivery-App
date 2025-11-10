package com.ds.communication_service.application.controller;

import java.security.Principal;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Controller;

import com.ds.communication_service.common.dto.ChatMessagePayload;
import com.ds.communication_service.common.dto.MessageResponse;
import com.ds.communication_service.common.interfaces.IMessageService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequiredArgsConstructor
@Slf4j
public class ChatController {

    private final SimpMessageSendingOperations messagingTemplate; 

    private final IMessageService messageService;

    /**
     * Nhận tin nhắn từ client (Android)
     * Client sẽ gửi đến "/app/chat.send"
     */
    @MessageMapping("/chat.send")
    public void sendMessage(@Payload ChatMessagePayload payload, Principal principal) {
        
        if (principal == null) {
            log.error("Gửi tin nhắn thất bại. Không tìm thấy principal (user chưa xác thực).");
            return;
        }

        // (Interceptor của bạn đã set "name" của Principal chính là Keycloak User ID (sub))
        String senderId = principal.getName();
        
        log.info("Tin nhắn nhận được: Từ {} -> Tới {}", senderId, payload.getRecipientId());

        MessageResponse savedMessage = messageService.processAndSaveMessage(payload, senderId);

        // CRITICAL: Send message to BOTH recipient and sender
        // This ensures both users receive the message via WebSocket immediately
        // Recipient receives the message, sender receives confirmation
        
        // Gửi đến recipient (người nhận)
        String recipientDestination = "/user/" + payload.getRecipientId() + "/queue/messages";
        log.info("📤 Sending message to RECIPIENT: userId={}, destination={}, messageId={}", 
            payload.getRecipientId(), recipientDestination, savedMessage.getId());
        messagingTemplate.convertAndSendToUser(
            payload.getRecipientId(),        
            "/queue/messages",             
            savedMessage             
        );
        
        // Also send to sender for confirmation (matching ChatActivity.java behavior)
        // This ensures sender sees their own message immediately via WebSocket
        String senderDestination = "/user/" + senderId + "/queue/messages";
        log.info("📤 Sending message to SENDER: userId={}, destination={}, messageId={}", 
            senderId, senderDestination, savedMessage.getId());
        messagingTemplate.convertAndSendToUser(
            senderId,
            "/queue/messages",             
            savedMessage             
        );
        
        log.info("✅ Message {} sent to both users: sender={}, recipient={}", 
            savedMessage.getId(), senderId, payload.getRecipientId());
    }
}
