package com.ds.session.session_service.common.entities.dto.response;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Session information for a delivery man
 * Used when enriching delivery man list with session data
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryManSessionInfo {
    
    /**
     * Whether the delivery man has an active session (CREATED or IN_PROGRESS)
     */
    private Boolean hasActiveSession;
    
    /**
     * Start time of the last session (most recent startTime)
     * Null if no sessions exist
     */
    private LocalDateTime lastSessionStartTime;
}
