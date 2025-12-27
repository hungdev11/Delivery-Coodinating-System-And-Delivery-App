package com.ds.parcel_service.application.client;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class DesDetail {
    private String id;
    private BigDecimal lat;
    private BigDecimal lon;
    
    // Zone ID (required for processing even when zone service is unavailable)
    @JsonProperty("zoneId")
    private String zoneId;
    
    // Nested zone object (from AddressDto.zone)
    @JsonProperty("zone")
    private ZoneInfoFromAddress zone;
}
