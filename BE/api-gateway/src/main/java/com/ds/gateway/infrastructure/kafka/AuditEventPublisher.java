package com.ds.gateway.infrastructure.kafka;

import com.ds.gateway.application.security.UserContext;
import com.ds.gateway.infrastructure.kafka.dto.AuditEventDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Publisher for audit events to Kafka
 * Handles retry logic and dead letter queue
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuditEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Publish audit event to Kafka
     * Uses async send with callback for error handling
     */
    public void publishAuditEvent(AuditEventDto event) {
        try {
            // Generate event ID if not provided
            if (event.getEventId() == null || event.getEventId().isBlank()) {
                event.setEventId(UUID.randomUUID().toString());
            }
            
            // Set timestamp if not provided
            if (event.getTimestamp() == null) {
                event.setTimestamp(LocalDateTime.now());
            }
            
            // Set source service
            if (event.getSourceService() == null || event.getSourceService().isBlank()) {
                event.setSourceService("api-gateway");
            }
            
            // Enrich with user context if available
            UserContext.getCurrentUser().ifPresent(user -> {
                if (event.getUserId() == null) {
                    event.setUserId(user.getUserId());
                }
                if (event.getUserRoles() == null && user.getRoles() != null && !user.getRoles().isEmpty()) {
                    event.setUserRoles(user.getRoles().stream()
                        .sorted()
                        .collect(Collectors.joining(",")));
                }
            });
            
            // Use resourceId or userId as key for partitioning
            String key = event.getResourceId() != null 
                ? event.getResourceId() 
                : (event.getUserId() != null ? event.getUserId() : event.getEventId());
            
            // Send to Kafka asynchronously
            CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(
                KafkaConfig.TOPIC_AUDIT_EVENTS,
                key,
                event
            );
            
            // Add callback for error handling (non-blocking)
            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.debug("✅ Audit event published successfully: eventId={}, operation={}, resourceType={}", 
                        event.getEventId(), event.getOperationType(), event.getResourceType());
                } else {
                    // Log error but don't throw - audit logging should never block API
                    log.warn("⚠️ Failed to publish audit event (non-critical): eventId={}, operation={}, resourceType={}, error={}", 
                        event.getEventId(), event.getOperationType(), event.getResourceType(), ex.getMessage());
                    
                    // Try to send to Dead Letter Queue (also non-blocking)
                    try {
                        sendToDeadLetterQueue(event, ex);
                    } catch (Exception dlqEx) {
                        log.error("❌ Failed to send audit event to DLQ (non-critical): {}", dlqEx.getMessage());
                    }
                }
            });
            
        } catch (Exception e) {
            // Never throw exception - audit logging should never block API
            log.warn("⚠️ Error publishing audit event (non-critical): {}", e.getMessage());
            // Try to send to Dead Letter Queue (also non-blocking)
            try {
                sendToDeadLetterQueue(event, e);
            } catch (Exception dlqEx) {
                log.error("❌ Failed to send audit event to DLQ (non-critical): {}", dlqEx.getMessage());
            }
        }
    }

    /**
     * Send failed event to Dead Letter Queue
     * Non-blocking - never throws exceptions
     */
    private void sendToDeadLetterQueue(AuditEventDto event, Throwable error) {
        try {
            // Mark as failed
            event.setStatus(AuditEventDto.Status.FAILED);
            event.setErrorMessage(error != null ? error.getMessage() : "Unknown error");
            
            // Truncate stack trace if too long (max 5000 chars)
            if (error != null && error.getStackTrace() != null) {
                StringBuilder stackTrace = new StringBuilder();
                for (StackTraceElement element : error.getStackTrace()) {
                    stackTrace.append(element.toString()).append("\n");
                    if (stackTrace.length() > 5000) {
                        stackTrace.append("... (truncated)");
                        break;
                    }
                }
                event.setErrorStackTrace(stackTrace.toString());
            }
            
            // Send to DLQ asynchronously (non-blocking)
            CompletableFuture<SendResult<String, Object>> dlqFuture = kafkaTemplate.send(
                KafkaConfig.TOPIC_AUDIT_EVENTS_DLQ, 
                event.getEventId(), 
                event
            );
            
            dlqFuture.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.debug("📋 Audit event sent to DLQ: eventId={}", event.getEventId());
                } else {
                    log.warn("⚠️ Failed to send audit event to DLQ (non-critical): eventId={}, error={}", 
                        event.getEventId(), ex.getMessage());
                }
            });
            
        } catch (Exception e) {
            // Never throw - audit logging should never block API
            log.warn("⚠️ Error sending audit event to DLQ (non-critical): {}", e.getMessage());
        }
    }

    /**
     * Helper method to create audit event from operation details
     */
    public void logOperation(
            AuditEventDto.OperationType operationType,
            String httpMethod,
            String endpoint,
            String resourceType,
            String resourceId,
            String userId,
            String userRoles,
            Integer responseStatus,
            AuditEventDto.Status status,
            Long durationMs,
            String requestId,
            String clientIp,
            String userAgent,
            Map<String, Object> requestPayload,
            String errorMessage) {
        
        AuditEventDto event = AuditEventDto.builder()
            .operationType(operationType)
            .httpMethod(httpMethod)
            .endpoint(endpoint)
            .resourceType(resourceType)
            .resourceId(resourceId)
            .userId(userId)
            .userRoles(userRoles)
            .responseStatus(responseStatus)
            .status(status)
            .durationMs(durationMs)
            .requestId(requestId)
            .clientIp(clientIp)
            .userAgent(userAgent)
            .requestPayload(requestPayload)
            .errorMessage(errorMessage)
            .build();
        
        publishAuditEvent(event);
    }
}
