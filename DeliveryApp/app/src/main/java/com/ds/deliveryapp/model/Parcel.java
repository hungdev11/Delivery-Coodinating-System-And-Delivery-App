package com.ds.deliveryapp.model;

import com.ds.deliveryapp.enums.DeliveryType;
import com.ds.deliveryapp.enums.ParcelStatus;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Parcel implements Serializable {
    private String id;
    private String code;
    private String senderId;
    private String senderName; // Added: sender name from UserSnapshot
    private String receiverId;
    private String receiverName; // Added: receiver name from UserSnapshot
    private String receiverPhoneNumber;
    private DeliveryType deliveryType;
    private String receiveFrom;
    private String targetDestination;
    private ParcelStatus status;
    private double weight;
    private BigDecimal value;
    private String createdAt;
    private String updatedAt;
    private String windowStart;
    private String windowEnd;
    private String deliveredAt;
}
