package com.ds.gateway.common.interfaces;

import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.UUID;

public interface ISessionServiceClient {
    ResponseEntity<?> acceptParcelToSession(String deliveryManId, Object scanParcelRequest);
    ResponseEntity<?> getSessionById(UUID sessionId);
    ResponseEntity<?> completeSession(UUID sessionId);
    ResponseEntity<?> failSession(UUID sessionId, Object sessionFailRequest);
    ResponseEntity<?> createSessionBatch(Object createSessionRequest);

    ResponseEntity<?> getDailyTasks(UUID deliveryManId, List<String> status, int page, int size);
    ResponseEntity<?> getTasksHistory(UUID deliveryManId, List<String> status,
                                      String createdAtStart, String createdAtEnd,
                                      String completedAtStart, String completedAtEnd,
                                      int page, int size);
    ResponseEntity<?> completeTask(UUID deliveryManId, UUID parcelId, Object routeInfo);
    ResponseEntity<?> failTask(UUID deliveryManId, UUID parcelId, Object taskFailRequest);
    ResponseEntity<?> refuseTask(UUID deliveryManId, UUID parcelId);
    ResponseEntity<?> postponeTask(UUID deliveryManId, UUID parcelId, String addInfo);
    ResponseEntity<?> lastestShipperForParcel(UUID parcelId);
    
    /**
     * Update assignment status by sessionId and assignmentId
     * This is used by API gateway for nested queries
     */
    ResponseEntity<?> updateAssignmentStatus(UUID sessionId, UUID assignmentId, Object statusUpdateRequest);
    
    /**
     * Generate QR code
     */
    ResponseEntity<?> generateQR(String data);
    
    /**
     * Create session in CREATED status (prepare to receive parcels)
     */
    ResponseEntity<?> createSessionPrepared(String deliveryManId);
    
    /**
     * Start session (transition from CREATED to IN_PROGRESS)
     */
    ResponseEntity<?> startSession(UUID sessionId);
    
    /**
     * Get active session (CREATED or IN_PROGRESS) for a delivery man
     */
    ResponseEntity<?> getActiveSession(String deliveryManId);
}
