package com.ds.session.session_service.application.client.parcelclient;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import com.ds.session.session_service.application.client.parcelclient.response.BaseResponse;
import com.ds.session.session_service.application.client.parcelclient.response.ParcelResponse;
import com.ds.session.session_service.common.enums.ParcelEvent;

@FeignClient(
    name = "parcel-service", 
    url = "${PARCEL_SERVICE_URL:http://localhost:21506}"
)
public interface ParcelServiceClient {
    @GetMapping("/api/v1/parcels/{parcelId}")
    BaseResponse<ParcelResponse> fetchParcelResponseWrapped(@PathVariable String parcelId);

    @PutMapping("/api/v1/parcels/change-status/{parcelId}")
    BaseResponse<ParcelResponse> changeParcelStatusWrapped(@PathVariable String parcelId, @RequestParam ParcelEvent event);
    
    @PostMapping("/api/v1/parcels/bulk")
    BaseResponse<Map<String, ParcelResponse>> fetchParcelsBulkWrapped(@RequestBody List<UUID> parcelIds);
    
    // Helper default methods to unwrap BaseResponse
    default ParcelResponse fetchParcelResponse(String parcelId) {
        BaseResponse<ParcelResponse> response = fetchParcelResponseWrapped(parcelId);
        return response != null ? response.getResult() : null;
    }
    
    default ParcelResponse changeParcelStatus(String parcelId, ParcelEvent event) {
        BaseResponse<ParcelResponse> response = changeParcelStatusWrapped(parcelId, event);
        return response != null ? response.getResult() : null;
    }
    
    default Map<String, ParcelResponse> fetchParcelsBulk(List<UUID> parcelIds) {
        BaseResponse<Map<String, ParcelResponse>> response = fetchParcelsBulkWrapped(parcelIds);
        return response != null ? response.getResult() : null;
    }
}
