package com.ds.parcel_service.application.client;

import java.math.BigDecimal;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(
    name = "api-gateway", 
    url = "http://api-gateway:21500"
)
public interface ZoneClient {
    @GetMapping("/api/v1/addresses/{id}")
    DestinationResponse<DesDetail> getDestination(@PathVariable String id);
    
    @PostMapping("/api/v1/addresses")
    DestinationResponse<DesDetail> createDestination(@RequestBody CreateDestinationRequest request);

    @GetMapping("/api/v1/addresses/nearest")
    ListAddressResponse getNearestDestination(
        @RequestParam BigDecimal lat, 
        @RequestParam BigDecimal lon);
}