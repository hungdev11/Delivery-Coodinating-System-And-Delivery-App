package com.ds.parcel_service.application.client;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class DesDetail {
    private String id;
    private BigDecimal lat;
    private BigDecimal lon;
    private String zoneId;
}
