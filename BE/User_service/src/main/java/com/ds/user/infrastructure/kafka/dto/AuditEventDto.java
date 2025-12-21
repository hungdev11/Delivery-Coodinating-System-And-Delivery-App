package com.ds.user.infrastructure.kafka.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * DTO for audit events published to Kafka
 * Logs all create/update/delete operations for audit trail
 * Same structure as other services for consistency
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditEventDto {
    
    public enum OperationType {
        CREATE,
        UPDATE,
        DELETE,
        READ,
        CONNECT,
        DISCONNECT,
        MESSAGE
    }
    
    public enum Status {
        SUCCESS,
        FAILED,
        PENDING
    }
    
    private String eventId;
    private LocalDateTime timestamp;
    private OperationType operationType;
    private String sourceService;
    private String userId;
    private String userRoles;
    private String httpMethod;
    private String endpoint;
    private String resourceType;
    private String resourceId;
    private Map<String, Object> requestPayload;
    private Integer responseStatus;
    private Status status;
    private String errorMessage;
    private String errorStackTrace;
    private String requestId;
    private Map<String, Object> metadata;
    private Long durationMs;
    private String clientIp;
    private String userAgent;
}
