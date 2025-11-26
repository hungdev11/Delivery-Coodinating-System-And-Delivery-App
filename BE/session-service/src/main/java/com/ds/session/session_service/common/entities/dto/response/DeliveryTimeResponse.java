package com.ds.session.session_service.common.entities.dto.response;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response for delivery time calculation
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryTimeResponse {
    /**
     * Total duration in seconds (route calculation + buffer)
     */
    private Long totalDurationSeconds;
    
    /**
     * Total duration in minutes
     */
    private Long totalDurationMinutes;
    
    /**
     * Estimated completion time (current time + total duration)
     */
    private LocalDateTime estimatedCompletionTime;
    
    /**
     * Route duration from zone service (without buffer)
     */
    private Long routeDurationSeconds;
    
    /**
     * Buffer time in seconds (5 minutes * number of remaining parcels)
     */
    private Long bufferSeconds;
    
    /**
     * Number of parcels in calculation
     */
    private Integer parcelCount;
}
