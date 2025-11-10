package com.ds.parcel_service.application.client;

import java.util.List;

import lombok.Data;

@Data
public class ListAddressResponse {
    private List<DesDetail> result;
}
