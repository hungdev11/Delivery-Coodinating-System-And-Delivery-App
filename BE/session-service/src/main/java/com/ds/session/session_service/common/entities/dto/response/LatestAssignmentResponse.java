package com.ds.session.session_service.common.entities.dto.response;

import java.util.UUID;

import com.ds.session.session_service.common.enums.AssignmentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LatestAssignmentResponse {
    private UUID assignmentId;
    private UUID sessionId;
    private AssignmentStatus status;
    private String deliveryManId;
}

