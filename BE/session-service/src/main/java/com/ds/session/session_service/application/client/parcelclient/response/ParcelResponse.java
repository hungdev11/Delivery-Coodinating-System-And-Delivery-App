package com.ds.session.session_service.application.client.parcelclient.response;

import java.math.BigDecimal;
import java.time.LocalTime;

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
    private String receiverId;
    private String receiverPhoneNumber;
    private String deliveryType;
    private String receiveFrom;
    private String targetDestination;
    private String status;
    private double weight;
    private BigDecimal value;
    private LocalTime windowStart;
    private LocalTime windowEnd;

    private BigDecimal lat;
    private BigDecimal lon;
}
