package com.ds.communication_service.infrastructure.kafka;

import com.ds.communication_service.infrastructure.kafka.dto.AuditEventDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

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
                event.setSourceService("communication-service");
            }
            
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
            
            // Add callback for error handling
            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.debug("✅ Audit event published successfully: eventId={}, operation={}, resourceType={}", 
                        event.getEventId(), event.getOperationType(), event.getResourceType());
                } else {
                    log.error("❌ Failed to publish audit event: eventId={}, operation={}, resourceType={}", 
                        event.getEventId(), event.getOperationType(), event.getResourceType(), ex);
                    
                    // Send to Dead Letter Queue for manual review
                    sendToDeadLetterQueue(event, ex);
                }
            });
            
        } catch (Exception e) {
            log.error("❌ Error publishing audit event: {}", e.getMessage(), e);
            // Send to Dead Letter Queue
            sendToDeadLetterQueue(event, e);
        }
    }

    /**
     * Send failed event to Dead Letter Queue
     */
    private void sendToDeadLetterQueue(AuditEventDto event, Throwable error) {
        try {
            // Mark as failed
            event.setStatus(AuditEventDto.Status.FAILED);
            event.setErrorMessage(error.getMessage());
            
            // Truncate stack trace if too long (max 5000 chars)
            if (error.getStackTrace() != null) {
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
            
            // Send to DLQ
            kafkaTemplate.send(KafkaConfig.TOPIC_AUDIT_EVENTS_DLQ, event.getEventId(), event);
            log.debug("[communication-service] [AuditEventPublisher.sendToDLQ] Audit event sent to DLQ: eventId={}", event.getEventId());
            
        } catch (Exception e) {
            log.error("[communication-service] [AuditEventPublisher.sendToDLQ] CRITICAL: Failed to send audit event to DLQ", e);
            // At this point, we can only log to application logs
            // Consider implementing a fallback mechanism (e.g., file-based logging)
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
