package com.ds.communication_service.application.controller;

import java.security.Principal;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Controller;

import com.ds.communication_service.business.v1.services.MessageStatusService;
import com.ds.communication_service.business.v1.services.TypingService;
import com.ds.communication_service.common.dto.ChatMessagePayload;
import com.ds.communication_service.common.dto.MessageResponse;
import com.ds.communication_service.common.dto.QuickActionRequest;
import com.ds.communication_service.common.dto.TypingIndicator;
import com.ds.communication_service.common.interfaces.IMessageService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequiredArgsConstructor
@Slf4j
public class ChatController {

    private final SimpMessageSendingOperations messagingTemplate; 
    private final IMessageService messageService;
    private final MessageStatusService messageStatusService;
    private final TypingService typingService;

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
        
        try {
            messagingTemplate.convertAndSendToUser(
                payload.getRecipientId(),        
                "/queue/messages",             
                savedMessage             
            );
            log.info("✅ RECIPIENT message sent successfully");
        } catch (Exception e) {
            log.error("❌ Failed to send to RECIPIENT: {}", e.getMessage(), e);
        }
        
        // Also send to sender for confirmation (matching ChatActivity.java behavior)
        // This ensures sender sees their own message immediately via WebSocket
        String senderDestination = "/user/" + senderId + "/queue/messages";
        log.info("📤 Sending message to SENDER: userId={}, destination={}, messageId={}", 
            senderId, senderDestination, savedMessage.getId());
        
        try {
            messagingTemplate.convertAndSendToUser(
                senderId,
                "/queue/messages",             
                savedMessage             
            );
            log.info("✅ SENDER message sent successfully");
        } catch (Exception e) {
            log.error("❌ Failed to send to SENDER: {}", e.getMessage(), e);
        }
        
        log.info("✅ Message {} sent to both users: sender={}, recipient={}", 
            savedMessage.getId(), senderId, payload.getRecipientId());
    }

    /**
     * Handle typing indicator
     * Client sends typing events while user is typing
     * Server broadcasts to conversation partner
     */
    @MessageMapping("/chat.typing")
    public void handleTyping(@Payload TypingIndicator typingIndicator, Principal principal) {
        if (principal == null) {
            log.error("Typing indicator failed. No principal (user not authenticated).");
            return;
        }

        String userId = principal.getName();
        log.debug("📝 Typing indicator received: userId={}, conversationId={}, isTyping={}", 
            userId, typingIndicator.getConversationId(), typingIndicator.isTyping());
        
        // Set userId from authenticated principal
        typingIndicator.setUserId(userId);
        
        // Handle typing event (publishes to Kafka for broadcast)
        typingService.handleTypingEvent(
            typingIndicator.getConversationId(), 
            userId, 
            typingIndicator.isTyping()
        );
    }

    /**
     * Mark messages as read
     * Client sends read receipt when viewing messages
     */
    @MessageMapping("/chat.read")
    public void markAsRead(@Payload ReadReceiptPayload payload, Principal principal) {
        if (principal == null) {
            log.error("Mark as read failed. No principal (user not authenticated).");
            return;
        }

        String userId = principal.getName();
        log.info("👁️ Read receipt received: userId={}, messageIds={}", 
            userId, payload.getMessageIds().length);
        
        // Mark messages as read
        messageStatusService.markMultipleAsRead(
            payload.getMessageIds(), 
            payload.getConversationId(), 
            userId
        );
    }

    /**
     * Handle quick action on proposals
     * Enables 2-touch interaction for shippers (Accept/Reject/Postpone)
     */
    @MessageMapping("/chat.quick-action")
    public void handleQuickAction(@Payload QuickActionRequest quickAction, Principal principal) {
        if (principal == null) {
            log.error("Quick action failed. No principal (user not authenticated).");
            return;
        }

        String userId = principal.getName();
        quickAction.setUserId(userId);
        
        log.info("⚡ Quick action received: userId={}, action={}, proposalId={}", 
            userId, quickAction.getAction(), quickAction.getProposalId());
        
        // TODO: Implement quick action handling
        // This will be implemented in Phase 4 (Notification System)
        // For now, just log the action
        log.info("Quick action processing not yet implemented. Action: {}", quickAction.getAction());
    }

    /**
     * Inner class for read receipt payload
     */
    @lombok.Data
    public static class ReadReceiptPayload {
        private String[] messageIds;
        private String conversationId;
    }
}
