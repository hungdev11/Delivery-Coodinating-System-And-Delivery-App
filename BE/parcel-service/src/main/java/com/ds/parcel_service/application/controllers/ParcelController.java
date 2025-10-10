package com.ds.parcel_service.application.controllers;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.ds.parcel_service.common.entities.dto.request.ParcelCreateRequest;
import com.ds.parcel_service.common.entities.dto.request.ParcelUpdateRequest;
import com.ds.parcel_service.common.entities.dto.response.PageResponse;
import com.ds.parcel_service.common.entities.dto.response.ParcelResponse;
import com.ds.parcel_service.common.interfaces.IParcelService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v1/parcels")
@RequiredArgsConstructor
@Slf4j
@Validated
public class ParcelController {

    private final IParcelService parcelService;

    @PostMapping
    public ResponseEntity<ParcelResponse> createParcel(@Valid @RequestBody ParcelCreateRequest request) {
        log.info("Creating parcel with code={}", request.getCode());
        ParcelResponse response = parcelService.createParcel(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{parcelId}")
    public ResponseEntity<ParcelResponse> updateParcel(
            @PathVariable UUID parcelId,
            @Valid @RequestBody ParcelUpdateRequest request) {
        log.info("Updating parcel id={}", parcelId);
        ParcelResponse response = parcelService.updateParcel(parcelId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{parcelId}")
    public ResponseEntity<ParcelResponse> getParcelById(@PathVariable UUID parcelId) {
        log.info("Fetching parcel id={}", parcelId);
        ParcelResponse response = parcelService.getParcelById(parcelId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/code/{code}")
    public ResponseEntity<ParcelResponse> getParcelByCode(@PathVariable String code) {
        log.info("Fetching parcel by code={}", code);
        ParcelResponse response = parcelService.getParcelByCode(code);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<PageResponse<ParcelResponse>> getParcels(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false, defaultValue = "asc") String direction) {
        log.info("Fetching parcels page={} size={}", page, size);
        PageResponse<ParcelResponse> response = parcelService.getParcels(page, size, sortBy, direction);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{parcelId}")
    public ResponseEntity<Void> deleteParcel(@PathVariable UUID parcelId) {
        log.warn("Deleting parcel id={} (not implemented)", parcelId);
        parcelService.deleteParcel(parcelId);
        return ResponseEntity.noContent().build();
    }
}
