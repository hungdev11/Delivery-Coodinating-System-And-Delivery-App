package com.ds.communication_service.common.dto;

import com.ds.communication_service.common.enums.TicketStatus;
import com.ds.communication_service.common.enums.TicketType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for ticket response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketResponse {
    
    private String id;
    private String parcelId;
    private String deliveryAssignmentId;
    private String userId;
    private TicketType type;
    private TicketStatus status;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime resolvedAt;
    private String resolvedBy;
    private String resolutionNotes;
}
