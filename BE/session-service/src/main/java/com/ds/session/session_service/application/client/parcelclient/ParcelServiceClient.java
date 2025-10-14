package com.ds.session.session_service.application.client.parcelclient;

import java.util.UUID;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.ds.session.session_service.application.client.parcelclient.response.ParcelResponse;
import com.ds.session.session_service.common.enums.ParcelEvent;

@FeignClient(
    name = "parcel-service", 
    url = "http://localhost:21506"
)
public interface ParcelServiceClient {
    @GetMapping("/api/v1/parcels/{parcelId}")
    ParcelResponse fetchParcelResponse(@PathVariable UUID parcelId);

    @PutMapping("/api/v1/parcels/change-status/{parcelId}")
    ParcelResponse changeParcelStatus(@PathVariable UUID parcelId, @RequestParam ParcelEvent event);
}
