package com.ds.user.common.entities.dto.deliveryman;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Session information DTO (matches session-service response)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryManSessionInfo {
    private Boolean hasActiveSession;
    private LocalDateTime lastSessionStartTime;
}

