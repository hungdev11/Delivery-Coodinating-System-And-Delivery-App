package com.ds.parcel_service.application.client;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

/**
 * Zone information nested in AddressDto from zone-service
 */
@Data
public class ZoneInfoFromAddress {
    @JsonProperty("id")
    private String id;
    
    @JsonProperty("code")
    private String code;
    
    @JsonProperty("name")
    private String name;
    
    @JsonProperty("center")
    private CenterInfoFromAddress center;
    
    @Data
    public static class CenterInfoFromAddress {
        @JsonProperty("id")
        private String id;
        
        @JsonProperty("code")
        private String code;
        
        @JsonProperty("name")
        private String name;
        
        @JsonProperty("address")
        private String address;
        
        @JsonProperty("lat")
        private BigDecimal lat;
        
        @JsonProperty("lon")
        private BigDecimal lon;
    }
}
