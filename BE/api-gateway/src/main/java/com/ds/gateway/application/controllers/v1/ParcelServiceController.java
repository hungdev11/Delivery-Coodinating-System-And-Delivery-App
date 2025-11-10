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
}
