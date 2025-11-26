package com.ds.communication_service.business.v1.services;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;

import com.ds.communication_service.common.dto.UpdateNotificationDTO;
import com.ds.communication_service.infrastructure.kafka.dto.SessionCompletedEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service to handle session completed events and notify all related clients/shippers
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SessionCompletionNotificationService {
    
    private final SimpMessageSendingOperations messagingTemplate;
    private final WebSocketSessionManager sessionManager;
    
    /**
     * Handle session completed event
     * Notifies all related clients and shipper to update their parcel lists
     */
    public void handleSessionCompleted(SessionCompletedEvent event) {
        log.debug("[communication-service] [SessionCompletionNotificationService.handleSessionCompleted] Handling session completed event: sessionId={}, deliveryManId={}, totalTasks={}, completedTasks={}, delayedTasks={}", 
                event.getSessionId(), event.getDeliveryManId(), event.getTotalTasks(), 
                event.getCompletedTasks(), event.getDelayedTasks());
        
        try {
            // 1. Notify shipper (Android app) - refresh task list
            if (event.getDeliveryManId() != null) {
                UpdateNotificationDTO shipperNotification = UpdateNotificationDTO.builder()
                        .id(UUID.randomUUID().toString())
                        .userId(event.getDeliveryManId())
                        .updateType(UpdateNotificationDTO.UpdateType.SESSION_UPDATE)
                        .entityType(UpdateNotificationDTO.EntityType.SESSION)
                        .entityId(event.getSessionId())
                        .action(UpdateNotificationDTO.ActionType.COMPLETED)
                        .message(String.format("Phiên giao hàng đã kết thúc. Hoàn thành: %d/%d đơn", 
                                event.getCompletedTasks(), event.getTotalTasks()))
                        .clientType(UpdateNotificationDTO.ClientType.ANDROID)
                        .timestamp(event.getCompletedAt())
                        .build();
                
                sessionManager.sendToUserIfClientType(
                    event.getDeliveryManId(),
                    "/queue/updates",
                    shipperNotification,
                    UpdateNotificationDTO.ClientType.ANDROID
                );
                log.debug("[communication-service] [SessionCompletionNotificationService.handleSessionCompleted] Sent session completion notification to shipper: {}", event.getDeliveryManId());
            }
            
            // 2. Notify all clients (Web) - refresh parcel list and clear in-progress parcels
            if (event.getReceiverIds() != null && !event.getReceiverIds().isEmpty()) {
                Set<String> uniqueReceiverIds = new HashSet<>(event.getReceiverIds()); // Remove duplicates
                
                for (String receiverId : uniqueReceiverIds) {
                    if (receiverId == null || receiverId.isBlank()) {
                        continue;
                    }
                    
                    UpdateNotificationDTO clientNotification = UpdateNotificationDTO.builder()
                            .id(UUID.randomUUID().toString())
                            .userId(receiverId)
                            .updateType(UpdateNotificationDTO.UpdateType.SESSION_UPDATE)
                            .entityType(UpdateNotificationDTO.EntityType.SESSION)
                            .entityId(event.getSessionId())
                            .action(UpdateNotificationDTO.ActionType.COMPLETED)
                            .message("Phiên giao hàng đã kết thúc. Vui lòng kiểm tra lại danh sách đơn hàng.")
                            .clientType(UpdateNotificationDTO.ClientType.WEB)
                            .timestamp(event.getCompletedAt())
                            .build();
                    
                    sessionManager.sendToUserIfClientType(
                        receiverId,
                        "/queue/updates",
                        clientNotification,
                        UpdateNotificationDTO.ClientType.WEB
                    );
                    log.debug("[communication-service] [SessionCompletionNotificationService.handleSessionCompleted] Sent session completion notification to client: {}", receiverId);
                }
            }
            
            log.debug("[communication-service] [SessionCompletionNotificationService.handleSessionCompleted] Successfully processed session completed event: sessionId={}", event.getSessionId());
            
        } catch (Exception e) {
            log.error("[communication-service] [SessionCompletionNotificationService.handleSessionCompleted] Error handling session completed event: sessionId={}", 
                    event.getSessionId(), e);
            throw e;
        }
    }
}
