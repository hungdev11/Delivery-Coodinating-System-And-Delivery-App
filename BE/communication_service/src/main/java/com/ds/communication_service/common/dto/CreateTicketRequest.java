package com.ds.communication_service.common.dto;

import com.ds.communication_service.common.enums.TicketType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for creating a ticket
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateTicketRequest {
    
    @NotNull(message = "Ticket type is required")
    private TicketType type;
    
    @NotBlank(message = "Parcel ID is required")
    private String parcelId;
    
    /**
     * Optional: Assignment ID if ticket is related to a specific assignment
     */
    private String assignmentId;
    
    @NotBlank(message = "Description is required")
    private String description;
}
