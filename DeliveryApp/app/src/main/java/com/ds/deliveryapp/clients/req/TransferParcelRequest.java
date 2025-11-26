package com.ds.deliveryapp.clients.req;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for transferring a parcel from one shipper to another
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransferParcelRequest {
    private String parcelId;
    private String targetSessionId; // Session ID of the receiving shipper
}
