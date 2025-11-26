package com.ds.gateway.application.controllers.v1;

import com.ds.gateway.annotations.AuthRequired;
import com.ds.gateway.common.interfaces.IParcelServiceClient;
import com.ds.gateway.common.utils.ProxyHeaderUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Client-scoped parcel endpoints proxied through the API Gateway.
 * These endpoints automatically scope parcels to the authenticated user.
 */
@RestController
@RequestMapping("/api/v1/client/parcels")
@RequiredArgsConstructor
@Slf4j
public class ClientParcelController {

    private final IParcelServiceClient parcelServiceClient;

    /**
     * Fetch parcels where the current user is the receiver.
     * The gateway injects X-User-Id/X-User-Roles headers based on the JWT.
     */
    @PostMapping("/received")
    @AuthRequired
    public ResponseEntity<?> getReceivedParcels(@RequestBody Object request) {
        log.debug("[api-gateway] [ClientParcelController.getReceivedParcels] POST /api/v1/client/parcels/received - proxying to Parcel Service");
        return ProxyHeaderUtils.cleanResponseHeaders(parcelServiceClient.getClientReceivedParcels(request));
    }

    @PostMapping("/{parcelId}/confirm")
    @AuthRequired
    public ResponseEntity<?> confirmParcel(
            @PathVariable UUID parcelId,
            @RequestBody Object request) {
        log.debug("[api-gateway] [ClientParcelController.confirmParcel] POST /api/v1/client/parcels/{}/confirm - proxying to Parcel Service", parcelId);
        return ProxyHeaderUtils.cleanResponseHeaders(parcelServiceClient.confirmParcel(parcelId, request));
    }
}
