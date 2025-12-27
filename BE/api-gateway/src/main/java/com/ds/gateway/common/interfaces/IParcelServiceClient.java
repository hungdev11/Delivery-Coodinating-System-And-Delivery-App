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
    ResponseEntity<?> getParcelsV2(Object request);
    ResponseEntity<?> getClientReceivedParcels(Object request);
    ResponseEntity<?> changeParcelStatus(UUID parcelId, String event);
    ResponseEntity<?> deleteParcel(UUID parcelId);
    ResponseEntity<?> confirmParcel(UUID parcelId, Object request);
    
    // Dispute handling methods
    ResponseEntity<?> disputeParcel(UUID parcelId);
    ResponseEntity<?> retractDispute(UUID parcelId);
    ResponseEntity<?> resolveDisputeAsMisunderstanding(UUID parcelId);
    ResponseEntity<?> resolveDisputeAsFault(UUID parcelId);
    
    // Auto seed parcels
    ResponseEntity<?> autoSeedParcels();
}
