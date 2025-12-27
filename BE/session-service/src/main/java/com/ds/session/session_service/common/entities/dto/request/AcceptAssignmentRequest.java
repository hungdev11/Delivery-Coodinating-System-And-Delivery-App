package com.ds.session.session_service.common.entities.dto.request;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for shipper to accept assignment (QR code scan)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AcceptAssignmentRequest {
    
    /**
     * Assignment ID (from QR code)
     */
    @NotNull(message = "Assignment ID is required")
    private UUID assignmentId;
}
