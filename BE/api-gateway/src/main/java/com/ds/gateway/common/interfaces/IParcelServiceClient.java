package com.ds.gateway.common.interfaces;

import org.springframework.http.ResponseEntity;

import java.util.UUID;

public interface IParcelServiceClient {
    ResponseEntity<?> createParcel(Object request);
    ResponseEntity<?> updateParcel(UUID parcelId, Object request);
    ResponseEntity<?> getParcelById(UUID parcelId);
    ResponseEntity<?> getParcelByCode(String code);
    ResponseEntity<?> getParcelsSent(String customerId, int page, int size);
    ResponseEntity<?> getParcelsReceived(String customerId, int page, int size);
}
