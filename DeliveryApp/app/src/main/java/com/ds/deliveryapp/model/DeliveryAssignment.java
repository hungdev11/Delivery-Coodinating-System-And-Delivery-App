package com.ds.deliveryapp.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.util.Locale;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class DeliveryAssignment implements Serializable {
    private String sessionId;
    private String parcelId;
    private String parcelCode;
    private String deliveryType;
    private String status;
    private String deliveryManAssignedId;
    private String receiverId;
    private String deliveryManPhone;
    private String receiverName;
    private String receiverPhone;
    private String deliveryLocation;
    private BigDecimal value;
    private double weight;
    private String createdAt;
    private String completedAt;
    private String failReason;
}