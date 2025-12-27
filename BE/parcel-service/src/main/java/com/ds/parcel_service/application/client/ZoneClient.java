package com.ds.parcel_service.application.client;

import java.math.BigDecimal;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(
    name = "zone-service", 
    url = "${services.zone.base-url:http://zone-service:21503}"
)
public interface ZoneClient {
    @GetMapping("/api/v1/addresses/{id}")
    DestinationResponse<DesDetail> getDestination(@PathVariable String id);
    
    @PostMapping("/api/v1/addresses")
    DestinationResponse<DesDetail> createDestination(@RequestBody CreateDestinationRequest request);
    
    @PostMapping("/api/v1/addresses/get-or-create")
    DestinationResponse<DesDetail> getOrCreateDestination(@RequestBody CreateDestinationRequest request);

    @GetMapping("/api/v1/addresses/nearest")
    ListAddressResponse getNearestDestination(
        @RequestParam BigDecimal lat, 
        @RequestParam BigDecimal lon);
    
    @GetMapping("/api/v1/zones/{id}")
    com.ds.parcel_service.common.entities.dto.common.BaseResponse<ZoneInfo> getZone(@PathVariable String id);
    
    @PostMapping("/api/v2/zones")
    com.ds.parcel_service.common.entities.dto.common.BaseResponse<com.ds.parcel_service.common.entities.dto.common.PagedData<java.util.Map<String, Object>>> getZonesV2(
        @RequestBody java.util.Map<String, Object> requestBody);
}
