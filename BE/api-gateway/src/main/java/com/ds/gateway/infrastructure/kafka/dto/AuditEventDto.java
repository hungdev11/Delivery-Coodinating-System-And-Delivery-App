package com.ds.gateway.infrastructure.kafka.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * DTO for audit events published to Kafka
 * Logs all create/update/delete operations for audit trail
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
    
    /**
     * Unique event ID
     */
    private String eventId;
    
    /**
     * Timestamp when the operation occurred
     */
    private LocalDateTime timestamp;
    
    /**
     * Type of operation (CREATE, UPDATE, DELETE)
     */
    private OperationType operationType;
    
    /**
     * Service that initiated the operation (api-gateway, communication-service, etc.)
     */
    private String sourceService;
    
    /**
     * User ID who performed the operation (from JWT token)
     */
    private String userId;
    
    /**
     * User roles (comma-separated)
     */
    private String userRoles;
    
    /**
     * HTTP method (GET, POST, PUT, DELETE, etc.)
     */
    private String httpMethod;
    
    /**
     * API endpoint path
     */
    private String endpoint;
    
    /**
     * Resource type (e.g., "parcel", "address", "conversation", "proposal")
     */
    private String resourceType;
    
    /**
     * Resource ID (if applicable)
     */
    private String resourceId;
    
    /**
     * Request payload (for CREATE/UPDATE operations)
     * Can be truncated if too large
     */
    private Map<String, Object> requestPayload;
    
    /**
     * Response status code
     */
    private Integer responseStatus;
    
    /**
     * Operation status (SUCCESS, FAILED, PENDING)
     */
    private Status status;
    
    /**
     * Error message (if status is FAILED)
     */
    private String errorMessage;
    
    /**
     * Stack trace (if status is FAILED, truncated)
     */
    private String errorStackTrace;
    
    /**
     * Request ID for tracing across services
     */
    private String requestId;
    
    /**
     * Additional metadata
     */
    private Map<String, Object> metadata;
    
    /**
     * Duration in milliseconds
     */
    private Long durationMs;
    
    /**
     * IP address of the client
     */
    private String clientIp;
    
    /**
     * User agent
     */
    private String userAgent;
}
