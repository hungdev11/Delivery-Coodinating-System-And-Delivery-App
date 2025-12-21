package com.ds.gateway.common.entities.dto.health;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Health status DTO published to Kafka health-status topic
 * Each service publishes its health status at regular intervals
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HealthStatusDto {
    
    /**
     * Service name (e.g., "user-service", "session-service", "parcel-service")
     */
    private String serviceName;
    
    /**
     * Health status: "UP" or "DOWN"
     */
    private String status;
    
    /**
     * Timestamp of this health check
     * Accepts ISO-8601 format string (Jackson will auto-parse)
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private LocalDateTime timestamp;
    
    /**
     * Optional: Service version
     */
    private String version;
    
    /**
     * Optional: Additional metadata (instance ID, hostname, etc.)
     */
    private Map<String, Object> metadata;
}
