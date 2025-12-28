package com.ds.communication_service.common.dto;

import com.ds.communication_service.common.enums.TicketType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for creating a new ticket
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateTicketRequest {
    
    /**
     * Parcel ID (required)
     * Reference to parcel-service.parcels.id
     */
    @NotBlank(message = "Parcel ID is required")
    private String parcelId;

    /**
     * Delivery assignment ID (optional)
     * Reference to session-service.delivery_assignments.id
     */
    private String deliveryAssignmentId;

    /**
     * User ID (required)
     * Can be either client (for NOT_RECEIVED) or shipper (for DELIVERY_FAILED)
     * Reference to user-service.users.id
     */
    @NotBlank(message = "User ID is required")
    private String userId;

    /**
     * Ticket type (required)
     */
    @NotNull(message = "Ticket type is required")
    private TicketType type;

    /**
     * Description of the issue (optional)
     */
    private String description;
}
