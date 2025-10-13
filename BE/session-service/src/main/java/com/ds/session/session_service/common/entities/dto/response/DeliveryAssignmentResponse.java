package com.ds.session.session_service.common.entities.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import com.ds.session.session_service.app_context.models.DeliveryAssignment;
import com.ds.session.session_service.business.v1.services.ParcelMock.Parcel;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryAssignmentResponse {
    // Task info
    private String taskId;
    private String parcelId;
    private String status;
    private LocalDateTime completedAt;
    private int attemptCount;
    private LocalDateTime createdAt;

    // Parcel info
    private double weight;
    private String receiverName;
    private String receiverPhone;
    private String deliveryLocation;
    private String note;

    // List of sessions
    private List<SessionInfo> sessionsInfo;

    // ========== STATIC MAPPERS ==========
    // public static TaskSessionResponse from(Task task, Parcel parcel) {
    //     return TaskSessionResponse.builder()
    //             .taskId(task.getId().toString())
    //             .parcelId(parcel.parcelId)
    //             .status(task.getStatus().name())
    //             .completedAt(task.getCompletedAt())
    //             .attemptCount(task.getAttemptCount())
    //             .createdAt(task.getCreatedAt())
    //             .weight(parcel.weight)
    //             .receiverName(parcel.receiverName)
    //             .receiverPhone(parcel.receiverPhone)
    //             .deliveryLocation(parcel.deliveryLocation.toString())
    //             .note(parcel.note)
    //             .build();
    // }

    // ========== INNER CLASS ==========
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SessionInfo {
        private LocalDate date;
        private LocalTime beginTime;
        private LocalTime endTime;
        private String deliveryManAssigned;
        private String deliveryManPhone;

        private LocalTime routeStartTime;
        private LocalTime routeFinishTime;
        private double routeDistanceM;
        private long routeDurationS;
        private String routeWaypoints;
        private String failReason;

        // public static SessionInfo from(DeliveryAssignment s) {
        //     return SessionInfo.builder()
        //             .date(s.getAssignedAt().toLocalDate())
        //             .beginTime(s.getWindowStart())
        //             .endTime(s.getWindowEnd())
        //             .deliveryManAssigned(s.getDeliveryManId().toString())
        //             .routeDistanceM(s.getDistanceM())
        //             .routeDurationS(s.getDurationS())
        //             .routeWaypoints(s.getWaypoints())
        //             .failReason(s.getFailReason())
        //             .build();
        // }
    }
}
