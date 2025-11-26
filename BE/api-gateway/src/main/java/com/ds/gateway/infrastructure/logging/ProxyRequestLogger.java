package com.ds.gateway.infrastructure.logging;

import com.ds.gateway.application.security.UserContext;
import com.ds.gateway.infrastructure.kafka.AuditEventPublisher;
import com.ds.gateway.infrastructure.kafka.dto.AuditEventDto;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpMethod;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Centralized asynchronous logger for outbound proxy calls.
 * Controllers should call start(...) before invoking downstream services
 * and finishSuccess/finishFailure after receiving the response.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProxyRequestLogger {

    private final AuditEventPublisher auditEventPublisher;
    @Qualifier("auditLogExecutor")
    private final TaskExecutor auditLogExecutor;

    public ProxyLogContext start(HttpMethod method, String targetService, String targetUrl, Object requestBody) {
        HttpServletRequest httpRequest = getCurrentRequest();
        String sourceEndpoint = httpRequest != null ? httpRequest.getRequestURI() : null;
        String clientIp = extractClientIp(httpRequest);
        String userAgent = httpRequest != null ? httpRequest.getHeader("User-Agent") : null;

        String userId = UserContext.getCurrentUser().map(UserContext::getUserId).orElse(null);
        String userRoles = UserContext.getCurrentUser()
            .map(UserContext::getRoles)
            .filter(set -> !set.isEmpty())
            .map(set -> set.stream().sorted().collect(Collectors.joining(",")))
            .orElse(null);

        AuditEventDto.OperationType operationType = mapOperationType(method);
        Map<String, Object> payload = buildPayload(requestBody);

        ProxyLogContext context = ProxyLogContext.builder()
            .logId(UUID.randomUUID().toString())
            .startTime(System.currentTimeMillis())
            .targetService(targetService)
            .targetUrl(targetUrl)
            .httpMethod(method.name())
            .operationType(operationType)
            .userId(userId)
            .userRoles(userRoles)
            .sourceEndpoint(sourceEndpoint)
            .clientIp(clientIp)
            .userAgent(userAgent)
            .requestPayload(payload)
            .build();

        publishAsync(context, null, AuditEventDto.Status.PENDING, null);
        return context;
    }

    public void success(ProxyLogContext context, int statusCode) {
        publishAsync(context, statusCode, AuditEventDto.Status.SUCCESS, null);
    }

    public void failure(ProxyLogContext context, int statusCode, String errorMessage, Throwable error) {
        String message = errorMessage != null ? errorMessage : (error != null ? error.getMessage() : null);
        publishAsync(context, statusCode, AuditEventDto.Status.FAILED, message);
    }

    private void publishAsync(ProxyLogContext context, Integer statusCode, AuditEventDto.Status status, String errorMessage) {
        CompletableFuture.runAsync(() -> {
            try {
                long duration = System.currentTimeMillis() - context.getStartTime();
                auditEventPublisher.logOperation(
                    context.getOperationType(),
                    context.getHttpMethod(),
                    context.getSourceEndpoint(),
                    context.getTargetService(),
                    null,
                    context.getUserId(),
                    context.getUserRoles(),
                    statusCode,
                    status,
                    duration,
                    context.getLogId(),
                    context.getClientIp(),
                    context.getUserAgent(),
                    enrichPayload(context),
                    errorMessage
                );
            } catch (Exception e) {
                log.debug("[api-gateway] [ProxyRequestLogger.publishAsync] Failed to publish proxy log for {} {} -> {}: {}", context.getHttpMethod(),
                    context.getSourceEndpoint(), context.getTargetUrl(), e.getMessage());
            }
        }, auditLogExecutor);
    }

    private Map<String, Object> enrichPayload(ProxyLogContext context) {
        Map<String, Object> payload = context.getRequestPayload() != null
            ? new HashMap<>(context.getRequestPayload())
            : new HashMap<>();
        payload.put("targetUrl", context.getTargetUrl());
        return payload;
    }

    private Map<String, Object> buildPayload(Object body) {
        if (body == null) {
            return null;
        }
        Map<String, Object> payload = new HashMap<>();
        String value = truncate(body.toString(), 1000);
        payload.put("body", value);
        return payload;
    }

    private String truncate(String input, int maxLength) {
        if (input == null) {
            return null;
        }
        if (input.length() <= maxLength) {
            return input;
        }
        return input.substring(0, maxLength) + "...(truncated)";
    }

    private AuditEventDto.OperationType mapOperationType(HttpMethod method) {
        if (method == HttpMethod.POST) {
            return AuditEventDto.OperationType.CREATE;
        }
        if (method == HttpMethod.PUT || method == HttpMethod.PATCH) {
            return AuditEventDto.OperationType.UPDATE;
        }
        if (method == HttpMethod.DELETE) {
            return AuditEventDto.OperationType.DELETE;
        }
        return AuditEventDto.OperationType.READ;
    }

    private HttpServletRequest getCurrentRequest() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attrs != null ? attrs.getRequest() : null;
    }

    private String extractClientIp(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank()) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isBlank()) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            return ip.split(",")[0].trim();
        }
        return ip;
    }
}
