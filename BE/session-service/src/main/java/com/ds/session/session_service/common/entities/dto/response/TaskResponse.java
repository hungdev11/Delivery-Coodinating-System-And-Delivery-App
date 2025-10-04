package com.ds.session.session_service.common.entities.dto.response;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskResponse {
    private String taskId;
    private String parcelId;
    private String status;
    private LocalDateTime completedAt;
    private int attemptCount;
    private LocalDateTime createdAt;
}
