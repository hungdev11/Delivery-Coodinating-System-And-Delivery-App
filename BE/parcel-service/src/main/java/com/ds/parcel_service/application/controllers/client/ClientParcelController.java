package com.ds.parcel_service.application.controllers.client;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

import com.ds.parcel_service.common.entities.dto.common.BaseResponse;
import com.ds.parcel_service.common.entities.dto.common.PagedData;
import com.ds.parcel_service.common.entities.dto.request.PagingRequestV2;
import com.ds.parcel_service.common.entities.dto.request.ParcelConfirmRequest;
import com.ds.parcel_service.common.entities.dto.response.ParcelResponse;
import com.ds.parcel_service.common.interfaces.IParcelService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Client-scoped parcel endpoints. These endpoints automatically scope results
 * to the authenticated user (receiver) based on the X-User-Id header forwarded
 * from the API Gateway.
 */
@RestController
@RequestMapping("/api/v1/client/parcels")
@RequiredArgsConstructor
@Slf4j
@Validated
public class ClientParcelController {

    private final IParcelService parcelService;

    @PostMapping("/received")
    public ResponseEntity<BaseResponse<PagedData<ParcelResponse>>> getReceivedParcels(
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody PagingRequestV2 request
    ) {
        log.info("POST /api/v1/client/parcels/received - Fetching parcels for receiver {}", userId);
        PagedData<ParcelResponse> result = parcelService.getParcelsForReceiver(userId, request);
        return ResponseEntity.ok(BaseResponse.success(result));
    }

    @PostMapping("/{parcelId}/confirm")
    public ResponseEntity<BaseResponse<ParcelResponse>> confirmParcel(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable("parcelId") UUID parcelId,
            @Valid @RequestBody ParcelConfirmRequest request
    ) {
        log.info("POST /api/v1/client/parcels/{}/confirm by {}", parcelId, userId);
        ParcelResponse response = parcelService.confirmParcelByClient(parcelId, userId, request);
        return ResponseEntity.ok(BaseResponse.success(response));
    }
}
