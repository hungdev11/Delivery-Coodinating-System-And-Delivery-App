package com.ds.gateway.application.controllers.v1;

import com.ds.gateway.common.interfaces.IParcelServiceClient;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/parcels")
@RequiredArgsConstructor
public class ParcelServiceController {

    private final IParcelServiceClient parcelServiceClient;

    @PostMapping
    public ResponseEntity<?> createParcel(@RequestBody Object request) {
        return parcelServiceClient.createParcel(request);
    }

    @PutMapping("/{parcelId}")
    public ResponseEntity<?> updateParcel(
            @PathVariable UUID parcelId,
            @RequestBody Object request) {
        return parcelServiceClient.updateParcel(parcelId, request);
    }

    @GetMapping("/{parcelId}")
    public ResponseEntity<?> getParcelById(@PathVariable UUID parcelId) {
        return parcelServiceClient.getParcelById(parcelId);
    }

    @GetMapping("/code/{code}")
    public ResponseEntity<?> getParcelByCode(@PathVariable String code) {
        return parcelServiceClient.getParcelByCode(code);
    }

    @GetMapping
    public ResponseEntity<?> getParcels(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String deliveryType,
            @RequestParam(required = false) String createdFrom,
            @RequestParam(required = false) String createdTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false, defaultValue = "asc") String direction) {
        // Build filter object from query parameters
        java.util.Map<String, Object> filter = new java.util.HashMap<>();
        if (status != null) filter.put("status", status);
        if (deliveryType != null) filter.put("deliveryType", deliveryType);
        if (createdFrom != null) filter.put("createdFrom", createdFrom);
        if (createdTo != null) filter.put("createdTo", createdTo);
        
        return parcelServiceClient.getParcels(filter, page, size, sortBy, direction);
    }

    @GetMapping("/me")
    public ResponseEntity<?> getParcelsSent(
            @RequestParam String customerId,
            @RequestParam int page,
            @RequestParam int size) {
        return parcelServiceClient.getParcelsSent(customerId, page, size);
    }

    @GetMapping("/me/receive")
    public ResponseEntity<?> getParcelsReceive(
            @RequestParam String customerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return parcelServiceClient.getParcelsReceived(customerId, page, size);
    }

    @PutMapping("/change-status/{parcelId}")
    public ResponseEntity<?> changeParcelStatus(
            @PathVariable UUID parcelId,
            @RequestParam String event) {
        return parcelServiceClient.changeParcelStatus(parcelId, event);
    }

    @DeleteMapping("/{parcelId}")
    public ResponseEntity<?> deleteParcel(@PathVariable UUID parcelId) {
        return parcelServiceClient.deleteParcel(parcelId);
    }

    // --- DISPUTE HANDLING ENDPOINTS ---

    @PutMapping("/dispute/{parcelId}")
    public ResponseEntity<?> disputeParcel(@PathVariable UUID parcelId) {
        return parcelServiceClient.disputeParcel(parcelId);
    }

    @PutMapping("/dispute/{parcelId}/retract")
    public ResponseEntity<?> retractDispute(@PathVariable UUID parcelId) {
        return parcelServiceClient.retractDispute(parcelId);
    }

    @PutMapping("/resolve-dispute/misunderstanding/{parcelId}")
    public ResponseEntity<?> resolveDisputeAsMisunderstanding(@PathVariable UUID parcelId) {
        return parcelServiceClient.resolveDisputeAsMisunderstanding(parcelId);
    }

    @PutMapping("/resolve-dispute/fault/{parcelId}")
    public ResponseEntity<?> resolveDisputeAsFault(@PathVariable UUID parcelId) {
        return parcelServiceClient.resolveDisputeAsFault(parcelId);
    }
}
