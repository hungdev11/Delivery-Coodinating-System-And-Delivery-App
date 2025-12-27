package com.ds.parcel_service.infrastructure.kafka.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Event DTO for seed progress updates
 * Published to Kafka, consumed by communication-service and broadcast via WebSocket
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeedProgressEvent {
    /**
     * Session key to identify the seed session
     * Clients subscribe to WebSocket topic: /topic/seed-progress/{sessionKey}
     */
    private String sessionKey;
    
    /**
     * Event type: STARTED, PROGRESS, COMPLETED, ERROR
     */
    private EventType eventType;
    
    /**
     * Current step number (1-5)
     */
    private Integer currentStep;
    
    /**
     * Total steps
     */
    private Integer totalSteps;
    
    /**
     * Step description
     */
    private String stepDescription;
    
    /**
     * Progress percentage (0-100)
     */
    private Integer progress;
    
    /**
     * Number of old parcels failed
     */
    private Integer failedOldParcelsCount;
    
    /**
     * Number of new parcels seeded
     */
    private Integer seededParcelsCount;
    
    /**
     * Number of addresses skipped
     */
    private Integer skippedAddressesCount;
    
    /**
     * Current client being processed (for progress tracking)
     */
    private Integer currentClient;
    
    /**
     * Total clients to process
     */
    private Integer totalClients;
    
    /**
     * Error message (if eventType is ERROR)
     */
    private String errorMessage;
    
    /**
     * Timestamp of the event
     */
    private LocalDateTime timestamp;
    
    public enum EventType {
        STARTED,
        PROGRESS,
        COMPLETED,
        ERROR
    }
}
