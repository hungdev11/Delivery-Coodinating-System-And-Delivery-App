package com.ds.parcel_service.application.controllers.v2;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.ds.parcel_service.common.entities.dto.request.PagingRequestV2;
import com.ds.parcel_service.common.entities.dto.response.PageResponse;
import com.ds.parcel_service.common.entities.dto.response.ParcelResponse;
import com.ds.parcel_service.common.interfaces.IParcelService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * V2 API Controller for Parcel Management
 * V2: Enhanced dynamic filtering with operations between each pair of conditions
 */
@RestController
@RequestMapping("/api/v2/parcels")
@RequiredArgsConstructor
@Slf4j
@Validated
public class ParcelControllerV2 {

    private final IParcelService parcelService;

    @PostMapping
    public ResponseEntity<PageResponse<ParcelResponse>> getParcels(@Valid @RequestBody PagingRequestV2 request) {
        log.info("POST /api/v2/parcels - Get parcels with enhanced filtering (V2)");
        PageResponse<ParcelResponse> response = parcelService.getParcelsV2(request);
        return ResponseEntity.ok(response);
    }
}
