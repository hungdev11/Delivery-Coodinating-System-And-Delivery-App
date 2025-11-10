package com.ds.parcel_service.application.client;

import lombok.Data;

@Data
public class DestinationResponse <T>{
    private T result;
}
