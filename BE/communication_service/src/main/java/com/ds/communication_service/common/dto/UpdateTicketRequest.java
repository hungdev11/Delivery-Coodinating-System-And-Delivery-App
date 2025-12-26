package com.ds.communication_service.common.dto;

import com.ds.communication_service.common.enums.TicketStatus;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for updating ticket (admin actions)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateTicketRequest {
    
    @NotNull(message = "Status is required")
    private TicketStatus status;
    
    /**
     * Optional: Resolution notes when resolving ticket
     */
    private String resolutionNotes;
    
    /**
     * Optional: Action taken (REASSIGN, CANCEL, RESOLVE)
     */
    private String actionTaken;
    
    /**
     * Optional: New assignment ID if reassigning
     */
    private String newAssignmentId;
}
