package com.ds.deliveryapp.clients.req;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for accepting a transferred parcel by scanning session QR
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AcceptTransferredParcelRequest {
    private String sourceSessionId; // Session ID of the shipper transferring the parcel
    private String parcelId;
}
