package com.ds.parcel_service.application.client;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateDestinationRequest {
    private String addressText;
    private String name;
    private BigDecimal lat;
    private BigDecimal lon;
}
