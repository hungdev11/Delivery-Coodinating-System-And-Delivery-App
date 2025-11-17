package com.ds.communication_service.infrastructure.logging;

import com.ds.communication_service.infrastructure.kafka.AuditEventPublisher;
import com.ds.communication_service.infrastructure.kafka.dto.AuditEventDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketEventLogger {

    private final AuditEventPublisher auditEventPublisher;
    @Qualifier("auditLogExecutor")
    private final TaskExecutor auditLogExecutor;

    public void logConnect(String userId, String sessionId, String roles, String clientType) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("sessionId", sessionId);
        metadata.put("clientType", clientType);
        publish(AuditEventDto.OperationType.CONNECT, "WS_CONNECT", "/ws", userId, roles, metadata, null);
    }

    public void logDisconnect(String userId, String sessionId) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("sessionId", sessionId);
        publish(AuditEventDto.OperationType.DISCONNECT, "WS_DISCONNECT", "/ws", userId, null, metadata, null);
    }

    public void logSubscribe(String userId, String destination) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("destination", destination);
        publish(AuditEventDto.OperationType.MESSAGE, "WS_SUBSCRIBE", destination, userId, null, metadata, null);
    }

    public void logSend(String userId, String destination, boolean sent) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("destination", destination);
        metadata.put("sent", sent);
        publish(AuditEventDto.OperationType.MESSAGE, "WS_SEND", destination, userId, null, metadata, sent ? null : "Not delivered");
    }

    private void publish(AuditEventDto.OperationType type,
                         String method,
                         String endpoint,
                         String userId,
                         String roles,
                         Map<String, Object> metadata,
                         String errorMessage) {
        CompletableFuture.runAsync(() -> {
            try {
                auditEventPublisher.logOperation(
                        type,
                        method,
                        endpoint,
                        "communication-service",
                        null,
                        userId,
                        roles,
                        200,
                        errorMessage == null ? AuditEventDto.Status.SUCCESS : AuditEventDto.Status.FAILED,
                        0L,
                        null,
                        null,
                        null,
                        metadata,
                        errorMessage
                );
            } catch (Exception e) {
                log.warn("Failed to log WebSocket event {} {}: {}", method, endpoint, e.getMessage());
            }
        }, auditLogExecutor);
    }
}
