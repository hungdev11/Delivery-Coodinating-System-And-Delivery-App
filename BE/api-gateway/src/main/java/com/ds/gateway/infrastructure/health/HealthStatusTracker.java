package com.ds.gateway.infrastructure.health;

import com.ds.gateway.common.entities.dto.health.HealthStatusDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Tracks health status of all services
 * Consumes from health-status topic and maintains latest status for each service
 */
@Slf4j
@Service
public class HealthStatusTracker {
    
    private final ObjectMapper objectMapper;
    
    // Store latest health status for each service
    private final Map<String, HealthStatusDto> serviceStatusMap = new ConcurrentHashMap<>();
    
    // Track ping interval (default 10s, will be updated from settings)
    private final AtomicInteger pingIntervalSeconds = new AtomicInteger(10);
    
    public HealthStatusTracker(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }
    
    @KafkaListener(
        topics = "health-status",
        groupId = "api-gateway-health-monitoring-v2",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeHealthStatus(
            @Payload String payload,
            @Header(value = KafkaHeaders.RECEIVED_KEY, required = false) String messageKey) {
        
        try {
            // Parse JSON string to HealthStatusDto
            HealthStatusDto healthStatus = objectMapper.readValue(payload, HealthStatusDto.class);
            
            // Use serviceName from payload, or fallback to message key
            String serviceName = healthStatus.getServiceName();
            if (serviceName == null || serviceName.isEmpty()) {
                serviceName = messageKey;
            }
            
            if (serviceName == null || serviceName.isEmpty()) {
                log.warn("[api-gateway] [HealthStatusTracker] Received health status without service name");
                return;
            }
            
            log.debug("[api-gateway] [HealthStatusTracker] Received health status: service={}, status={}, timestamp={}", 
                serviceName, healthStatus.getStatus(), healthStatus.getTimestamp());
            
            // Update latest status
            serviceStatusMap.put(serviceName, healthStatus);
            
        } catch (Exception e) {
            log.error("[api-gateway] [HealthStatusTracker] Error processing health status", e);
        }
    }
    
    /**
     * Get latest health status for a service
     */
    public HealthStatusDto getLatestStatus(String serviceName) {
        return serviceStatusMap.get(serviceName);
    }
    
    /**
     * Check if service is down
     * Service is considered down if:
     * - No status received yet, OR
     * - Last update was more than 2*pingInterval (or 5s, whichever is larger) ago
     */
    public boolean isServiceDown(String serviceName) {
        HealthStatusDto status = serviceStatusMap.get(serviceName);
        if (status == null) {
            return true; // Never received status
        }
        
        long secondsSinceLastUpdate = Duration.between(
            status.getTimestamp(), 
            LocalDateTime.now()
        ).getSeconds();
        
        int interval = pingIntervalSeconds.get();
        int timeoutThreshold = Math.max(2 * interval, 5); // Max(2*interval, 5s)
        
        boolean isDown = secondsSinceLastUpdate > timeoutThreshold;
        
        if (isDown) {
            log.debug("[api-gateway] [HealthStatusTracker] Service {} is DOWN (last update: {}s ago, threshold: {}s)", 
                serviceName, secondsSinceLastUpdate, timeoutThreshold);
        }
        
        return isDown;
    }
    
    /**
     * Get all service statuses
     */
    public Map<String, HealthStatusDto> getAllStatuses() {
        return new ConcurrentHashMap<>(serviceStatusMap);
    }
    
    /**
     * Get all service names being tracked
     */
    public Set<String> getTrackedServices() {
        return serviceStatusMap.keySet();
    }
    
    /**
     * Update ping interval (called when settings change)
     */
    public void updatePingInterval(int seconds) {
        pingIntervalSeconds.set(seconds);
        log.info("[api-gateway] [HealthStatusTracker] Updated ping interval to {} seconds", seconds);
    }
}
