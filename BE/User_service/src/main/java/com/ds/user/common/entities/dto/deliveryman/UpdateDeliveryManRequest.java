package com.ds.user.common.entities.dto.deliveryman;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateDeliveryManRequest {
    @NotBlank(message = "Vehicle type is required")
    private String vehicleType;

    @Positive(message = "Capacity must be positive")
    private Double capacityKg;
}
