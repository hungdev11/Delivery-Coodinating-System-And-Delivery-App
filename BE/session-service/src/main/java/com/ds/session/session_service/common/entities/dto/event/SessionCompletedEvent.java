package com.ds.session.session_service.common.entities.dto.event;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Event published when a session is completed
 * Communication service will notify all related clients/shippers
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionCompletedEvent {
    private String eventId;
    private String sessionId;
    private String deliveryManId;
    private LocalDateTime completedAt;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private int totalTasks;
    private int completedTasks;
    private int failedTasks;
    private int delayedTasks;
    /**
     * List of parcel IDs in this session (for client notification)
     */
    private List<String> parcelIds;
    /**
     * List of receiver IDs (for client notification)
     */
    private List<String> receiverIds;
    private Instant createdAt;
    private String sourceService;
}
