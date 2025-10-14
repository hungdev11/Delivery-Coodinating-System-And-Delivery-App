package com.ds.session.session_service.common.entities.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.ds.session.session_service.app_context.models.DeliveryAssignment;
import com.ds.session.session_service.business.v1.services.ParcelInfo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryAssignmentResponse {
    private String parcelId;
    private String parcelCode;
    private String deliveryType;
    private String status;
    private String deliveryManAssignedId;

    private String deliveryManPhone;
    private String receiverName;

    private String receiverPhone;
    private String deliveryLocation;
    private BigDecimal value;
    private double weight;
    private String note;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
    private double routeDistanceM;
    private long routeDurationS;
    private String routeWaypoints;
    private String failReason;
    
    public static DeliveryAssignmentResponse from(DeliveryAssignment assignment, ParcelInfo parcel, String deliveryManPhone, String receiverName) {
        return DeliveryAssignmentResponse.builder()
                .parcelId(assignment.getParcelId())
                .parcelCode(parcel.getCode()) 
                .deliveryType(parcel.getDeliveryType())
                .status(assignment.getStatus().name())
                .deliveryManAssignedId(assignment.getDeliveryManId())
                .deliveryManPhone(deliveryManPhone) 
                .receiverName(receiverName) 
                .receiverPhone(parcel.getReceiverPhoneNumber())
                .deliveryLocation(parcel.getTargetDestination()) // Mapping targetDestination as deliveryLocation
                .value(parcel.getValue())
                .weight(parcel.getWeight())
                .createdAt(assignment.getScanedAt())
                .completedAt(assignment.getUpdatedAt()) // Assuming completedAt is the last updated time
                .routeDistanceM(assignment.getDistanceM())
                .routeDurationS(assignment.getDurationS())
                .routeWaypoints(assignment.getWaypoints())
                .failReason(assignment.getFailReason())
                .build();
    }
}
