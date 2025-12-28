package com.ds.communication_service.common.dto;

import com.ds.communication_service.common.enums.TicketStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for updating ticket status
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateTicketRequest {
    
    /**
     * New status (optional)
     */
    private TicketStatus status;

    /**
     * Resolution notes (optional)
     * Used when resolving the ticket
     */
    private String resolutionNotes;
}
