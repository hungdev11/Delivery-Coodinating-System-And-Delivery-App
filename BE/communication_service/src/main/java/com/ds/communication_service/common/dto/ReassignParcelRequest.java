package com.ds.communication_service.common.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for reassigning a parcel (admin action)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReassignParcelRequest {
    
    /**
     * New delivery assignment ID
     * Reference to session-service.delivery_assignments.id
     */
    @NotBlank(message = "Delivery assignment ID is required")
    private String deliveryAssignmentId;

    /**
     * Notes about the reassignment (optional)
     */
    private String notes;
}
