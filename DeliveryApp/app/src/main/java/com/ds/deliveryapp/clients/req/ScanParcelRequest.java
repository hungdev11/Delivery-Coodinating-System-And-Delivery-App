package com.ds.deliveryapp.clients.req;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScanParcelRequest {
    private String parcelId;
}