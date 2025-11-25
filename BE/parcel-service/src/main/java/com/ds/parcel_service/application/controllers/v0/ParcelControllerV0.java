package com.ds.parcel_service.application.controllers.v0;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.ds.parcel_service.common.entities.dto.request.PagingRequestV0;
import com.ds.parcel_service.common.entities.dto.response.PageResponse;
import com.ds.parcel_service.common.entities.dto.response.ParcelResponse;
import com.ds.parcel_service.common.interfaces.IParcelService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * V0 API Controller for Parcel Management
 * V0: Simple paging and sorting without dynamic filters
 */
@RestController
@RequestMapping("/api/v0/parcels")
@RequiredArgsConstructor
@Slf4j
@Validated
public class ParcelControllerV0 {

    private final IParcelService parcelService;

    @PostMapping
    public ResponseEntity<PageResponse<ParcelResponse>> getParcels(@Valid @RequestBody PagingRequestV0 request) {
        log.debug("[parcel-service] [ParcelControllerV0.getParcels] POST /api/v0/parcels - Get parcels with simple paging (V0)");
        PageResponse<ParcelResponse> response = parcelService.getParcelsV0(request);
        return ResponseEntity.ok(response);
    }
}
