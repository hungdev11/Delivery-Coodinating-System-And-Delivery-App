package com.ds.parcel_service.common.entities.dto.request;

import java.math.BigDecimal;
import java.time.LocalTime;

import com.ds.parcel_service.common.annotations.EnumValue;
import com.ds.parcel_service.common.enums.DeliveryType;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParcelCreateRequest {

    @NotBlank(message = "code must not be blank")
    private String code;

    @NotBlank(message = "senderId must not be blank")
    private String senderId;

    @NotBlank(message = "receiverId must not be blank")
    private String receiverId;

    @NotNull(message = "deliveryType must not be null")
    @EnumValue(name = "deliveryType", enumClass = DeliveryType.class, message = "deliveryType must be a valid enum value")
    private String deliveryType;

    /**
     * UserAddress ID from user-service for sender address
     */
    @NotBlank(message = "senderAddressId must not be blank")
    private String senderAddressId;

    /**
     * UserAddress ID from user-service for receiver address
     */
    @NotBlank(message = "receiverAddressId must not be blank")
    private String receiverAddressId;

    @DecimalMin(value = "0.0", inclusive = true, message = "weight must be greater than or equal to 0")
    private double weight;

    @NotNull(message = "value must not be null")
    @DecimalMin(value = "0.0", inclusive = true, message = "value must be greater than or equal to 0")
    private BigDecimal value;

    private LocalTime windowStart;
    private LocalTime windowEnd;
}
