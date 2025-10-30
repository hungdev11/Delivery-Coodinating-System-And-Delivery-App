package com.ds.session.session_service.common.entities.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.ds.session.session_service.common.enums.SessionStatus;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SessionResponse {
    private UUID id;
    private String deliveryManId;
    private SessionStatus status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private int totalTasks;
    private int completedTasks;
    private int failedTasks;
    private List<AssignmentResponse> assignments;
}
