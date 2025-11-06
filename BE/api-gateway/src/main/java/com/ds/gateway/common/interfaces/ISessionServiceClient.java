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
}

