package com.ds.parcel_service.common.entities.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import com.ds.parcel_service.common.enums.DeliveryType;
import com.ds.parcel_service.common.enums.ParcelStatus;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParcelResponse {
    private String code;
    private String senderId;     
    private String receiverId;
    private String receiverPhoneNumber;
    private DeliveryType deliveryType;
    private String receiveFrom;
    private String targetDestination;
    private ParcelStatus status;
    private double weight;
    private BigDecimal value;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
