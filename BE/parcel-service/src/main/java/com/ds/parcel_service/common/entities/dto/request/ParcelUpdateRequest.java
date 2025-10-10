package com.ds.parcel_service.common.entities.dto.request;

import java.math.BigDecimal;

import com.ds.parcel_service.common.annotations.EnumValue;
import com.ds.parcel_service.common.enums.ParcelStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParcelUpdateRequest {

    @NotNull(message = "weight must not be null")
    @DecimalMin(value = "0.0", inclusive = true, message = "value must be greater than or equal to 0")
    private double weight;

    @NotNull(message = "value must not be null")
    @DecimalMin(value = "0.0", inclusive = true, message = "value must be greater than or equal to 0")
    private BigDecimal value;

    @NotNull(message = "parcelStatus must not be null")
    @EnumValue(name = "parcelStatus", enumClass = ParcelStatus.class, message = "parcelStatus must be a valid enum value")
    private String parcelStatus;
}
