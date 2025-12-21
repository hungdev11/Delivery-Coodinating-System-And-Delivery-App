package com.ds.setting.infrastructure.health;

import com.ds.setting.business.v1.services.SettingsService;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Singleton component to publish health status heartbeat to Kafka
 * Publishes health status at regular intervals (configurable via Settings Service)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HealthHeartbeatPublisher {

    private static final String TOPIC_HEALTH_STATUS = "health-status";
    
    @Value("${spring.application.name:settings-service}")
    private String serviceName;
    
    @Value("${info.app.version:1.2.0}")
    private String serviceVersion;
    
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final SettingsService settingsService;
    
    private ScheduledExecutorService scheduler;
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private final AtomicBoolean hasError = new AtomicBoolean(false);
    private int pingIntervalSeconds = 10; // Default value
    
    /**
     * Start heartbeat after application is ready
     */
    @EventListener(ApplicationReadyEvent.class)
    @Async
    public void startHeartbeat() {
        if (isRunning.compareAndSet(false, true)) {
            log.info("[{}] [HealthHeartbeatPublisher] Starting health heartbeat publisher", serviceName);
            
            // Load ping interval from Settings Service (self)
            try {
                String intervalStr = settingsService.getValue("HEALTH_PING_INTERVAL_SECONDS", "10");
                pingIntervalSeconds = Integer.parseInt(intervalStr);
                log.info("[{}] [HealthHeartbeatPublisher] Health ping interval: {} seconds", serviceName, pingIntervalSeconds);
            } catch (Exception e) {
                log.warn("[{}] [HealthHeartbeatPublisher] Failed to load HEALTH_PING_INTERVAL_SECONDS, using default: 10s. Error: {}", 
                    serviceName, e.getMessage());
            }
            
            // Create scheduler (single thread, daemon)
            scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "health-heartbeat-" + serviceName);
                t.setDaemon(true);
                return t;
            });
            
            // Start periodic heartbeat
            scheduler.scheduleAtFixedRate(
                this::publishHealthStatus,
                0, // Initial delay: start immediately
                pingIntervalSeconds, // Period
                TimeUnit.SECONDS
            );
            
            log.info("[{}] [HealthHeartbeatPublisher] Health heartbeat publisher started (interval: {}s)", serviceName, pingIntervalSeconds);
        }
    }
    
    /**
     * Publish health status to Kafka
     */
    private void publishHealthStatus() {
        // Skip if we've encountered a fatal error (topic doesn't exist)
        if (hasError.get()) {
            return;
        }
        
        try {
            HealthStatusDto healthStatus = HealthStatusDto.builder()
                .serviceName(serviceName)
                .status("UP")
                .timestamp(LocalDateTime.now())
                .version(serviceVersion)
                .metadata(buildMetadata())
                .build();
            
            @SuppressWarnings("null")
            CompletableFuture<SendResult<String, Object>> future = 
                kafkaTemplate.send(TOPIC_HEALTH_STATUS, serviceName, healthStatus);
            
            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.debug("[{}] [HealthHeartbeatPublisher] Health status published successfully", serviceName);
                } else {
                    log.error("[{}] [HealthHeartbeatPublisher] Failed to publish health status: {}", serviceName, ex.getMessage());
                    
                    // Check if error is due to topic not existing
                    if (ex.getCause() instanceof org.apache.kafka.common.errors.UnknownTopicOrPartitionException) {
                        log.error("[{}] [HealthHeartbeatPublisher] Topic '{}' does not exist. Stopping heartbeat.", serviceName, TOPIC_HEALTH_STATUS);
                        hasError.set(true);
                        stopHeartbeat();
                    }
                }
            });
            
        } catch (Exception e) {
            log.error("[{}] [HealthHeartbeatPublisher] Error publishing health status", serviceName, e);
        }
    }
    
    /**
     * Build metadata map for health status
     */
    private Map<String, Object> buildMetadata() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("hostname", getHostname());
        metadata.put("pid", getProcessId());
        metadata.put("timestamp_millis", System.currentTimeMillis());
        return metadata;
    }
    
    private String getHostname() {
        try {
            return java.net.InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            return "unknown";
        }
    }
    
    private String getProcessId() {
        return String.valueOf(ProcessHandle.current().pid());
    }
    
    /**
     * Stop heartbeat (called on error or shutdown)
     */
    private void stopHeartbeat() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        isRunning.set(false);
    }
    
    /**
     * Shutdown hook
     */
    @PreDestroy
    public void destroy() {
        log.info("[{}] [HealthHeartbeatPublisher] Shutting down health heartbeat publisher", serviceName);
        stopHeartbeat();
    }
    
    /**
     * DTO for health status message
     */
    @Data
    @Builder
    public static class HealthStatusDto {
        private String serviceName;
        private String status;
        
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime timestamp;
        
        private String version;
        private Map<String, Object> metadata;
    }
}
