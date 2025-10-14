package com.ds.parcel_service.common.entities.dto.request;

import java.time.LocalDateTime;

import org.springframework.format.annotation.DateTimeFormat;

import com.ds.parcel_service.common.annotations.EnumValue;
import com.ds.parcel_service.common.enums.DeliveryType;
import com.ds.parcel_service.common.enums.ParcelStatus;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParcelFilterRequest {
    @EnumValue(name = "status", enumClass = ParcelStatus.class, message = "parcel status must be a valid enum value")
    private String status;

    @EnumValue(name = "deliveryType", enumClass = DeliveryType.class, message = "delivery type must be a valid enum value")
    private String deliveryType;
    
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime createdFrom;
    
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime createdTo;
}
