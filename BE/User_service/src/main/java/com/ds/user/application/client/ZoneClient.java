package com.ds.user.application.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
    name = "zone-service", 
    url = "${services.zone.base-url:http://localhost:21503}"
)
public interface ZoneClient {
    @GetMapping("/api/v1/addresses/{id}")
    AddressResponse<AddressDetail> getAddress(@PathVariable String id);
}
