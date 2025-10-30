package com.ds.session.session_service.common.entities.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

import com.ds.session.session_service.common.enums.AssignmentStatus;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AssignmentResponse {
    private UUID id;
    private String parcelId;
    private AssignmentStatus status;
    private String failReason;
    private LocalDateTime scanedAt;
    private LocalDateTime updatedAt;
}