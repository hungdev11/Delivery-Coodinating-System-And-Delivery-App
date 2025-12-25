package com.ds.parcel_service.common.entities.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;

import com.ds.parcel_service.common.enums.DeliveryType;
import com.ds.parcel_service.common.enums.ParcelStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParcelResponse {
    private String id;
    private String code;
    private String senderId;
    private String senderName; // Full name from User Service
    private String receiverId;
    private String receiverName; // Full name from User Service
    private String receiverPhoneNumber;
    private DeliveryType deliveryType;
    
    /**
     * UserAddress ID from user-service for sender address
     */
    private String senderAddressId;
    
    /**
     * UserAddress ID from user-service for receiver address
     */
    private String receiverAddressId;
    
    private ParcelStatus status;
    private double weight;
    private BigDecimal value;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalTime windowStart;
    private LocalTime windowEnd;
    private LocalDateTime deliveredAt;
    private LocalDateTime confirmedAt;
    private String confirmedBy;
    private String confirmationNote;

    private BigDecimal lat;
    private BigDecimal lon;
    
    // Priority and delay fields
    private Integer priority;
    private Boolean isDelayed;
    private LocalDateTime delayedUntil;
}
