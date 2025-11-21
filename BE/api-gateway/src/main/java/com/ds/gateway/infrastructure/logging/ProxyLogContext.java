package com.ds.gateway.infrastructure.logging;

import com.ds.gateway.infrastructure.kafka.dto.AuditEventDto;
import lombok.Builder;
import lombok.Value;

import java.util.Map;

/**
 * Context holder for outbound proxy logs.
 * Stores correlation information so controllers can finish the log after the downstream call.
 */
@Value
@Builder
public class ProxyLogContext {
    String logId;
    long startTime;
    String targetService;
    String targetUrl;
    String httpMethod;
    AuditEventDto.OperationType operationType;
    String userId;
    String userRoles;
    String sourceEndpoint;
    String clientIp;
    String userAgent;
    Map<String, Object> requestPayload;
}
