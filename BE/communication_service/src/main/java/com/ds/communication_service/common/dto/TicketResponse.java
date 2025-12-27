package com.ds.communication_service.common.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import com.ds.communication_service.common.enums.TicketStatus;
import com.ds.communication_service.common.enums.TicketType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for ticket
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketResponse {
    private UUID id;
    private TicketType type;
    private TicketStatus status;
    private String parcelId;
    private String assignmentId;
    private String reporterId;
    private String assignedAdminId;
    private String description;
    private String resolutionNotes;
    private String actionTaken;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime resolvedAt;
}
