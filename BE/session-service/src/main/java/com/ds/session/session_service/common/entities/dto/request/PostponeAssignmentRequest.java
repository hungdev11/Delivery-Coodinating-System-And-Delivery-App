package com.ds.session.session_service.common.entities.dto.request;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request to postpone an assignment
 * Contains reason, route info, and optional postpone datetime
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostponeAssignmentRequest {
    /**
     * Reason for postponing (required)
     */
    private String reason;
    
    /**
     * Route info (optional)
     */
    @JsonProperty("routeInfo")
    private RouteInfo routeInfo;
    
    /**
     * Requested postpone datetime (optional)
     * If provided, will check if it's within session time
     * Format: ISO 8601 (e.g., "2024-01-15T14:30:00")
     */
    @JsonProperty("postponeDateTime")
    private LocalDateTime postponeDateTime;
    
    /**
     * Whether to move parcel to end of route instead of DELAY (optional)
     * If true and postpone is within session time, parcel will be moved to end
     * If false or postpone is outside session time, parcel will be DELAYED
     */
    @JsonProperty("moveToEnd")
    private Boolean moveToEnd;
}
