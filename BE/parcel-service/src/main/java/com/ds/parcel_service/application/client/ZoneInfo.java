package com.ds.parcel_service.application.client;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class ZoneInfo {
    private String id;
    private String code;
    private String name;
    private String centerId;
    private String centerCode;
    private String centerName;
    private String centerAddress;
    private BigDecimal centerLat;
    private BigDecimal centerLon;
}
