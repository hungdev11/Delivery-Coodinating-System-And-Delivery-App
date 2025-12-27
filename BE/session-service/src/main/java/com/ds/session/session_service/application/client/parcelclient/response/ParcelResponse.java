package com.ds.session.session_service.application.client.parcelclient.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true) // Ignore unknown fields from parcel-service V2 API
public class ParcelResponse {
    private String id;
    private String code;
    private String senderId;
    private String senderName; // Added: sender name from User Service (via bulk query)
    private String receiverId;
    private String receiverName;
    private String receiverPhoneNumber;
    private String deliveryType;
    private String receiveFrom;
    private String targetDestination;
    private String senderAddressId; // Reference to UserAddress ID
    private String receiverAddressId; // Reference to UserAddress ID
    private String status;
    private double weight;
    private BigDecimal value;
    private LocalDateTime createdAt; // Added to match parcel-service response
    private LocalDateTime updatedAt; // Added to match parcel-service response
    private LocalDateTime deliveredAt; // Added to match parcel-service response
    private LocalDateTime confirmedAt; // Added to match parcel-service response
    private LocalTime windowStart;
    private LocalTime windowEnd;

    private BigDecimal lat;
    private BigDecimal lon;
}
