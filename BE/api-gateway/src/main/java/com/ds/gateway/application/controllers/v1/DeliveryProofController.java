package com.ds.gateway.application.controllers.v1;

import com.ds.gateway.common.interfaces.ISessionServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * API Gateway proxy controller for Delivery Proof endpoints
 * Proxies requests to Session Service
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/delivery-proofs")
@RequiredArgsConstructor
public class DeliveryProofController {

    private final ISessionServiceClient sessionServiceClient;

    /**
     * Get all proofs for a specific assignment
     * GET /api/v1/delivery-proofs/assignments/{assignmentId}
     */
    @GetMapping("/assignments/{assignmentId}")
    public ResponseEntity<?> getProofsByAssignment(@PathVariable UUID assignmentId) {
        log.debug("[api-gateway] [DeliveryProofController.getProofsByAssignment] GET /api/v1/delivery-proofs/assignments/{}", assignmentId);
        return sessionServiceClient.getProofsByAssignment(assignmentId);
    }

    /**
     * Get all proofs for a specific parcel
     * Returns proofs from all assignments of that parcel
     * GET /api/v1/delivery-proofs/parcels/{parcelId}
     */
    @GetMapping("/parcels/{parcelId}")
    public ResponseEntity<?> getProofsByParcel(@PathVariable String parcelId) {
        log.debug("[api-gateway] [DeliveryProofController.getProofsByParcel] GET /api/v1/delivery-proofs/parcels/{}", parcelId);
        return sessionServiceClient.getProofsByParcel(parcelId);
    }
}
