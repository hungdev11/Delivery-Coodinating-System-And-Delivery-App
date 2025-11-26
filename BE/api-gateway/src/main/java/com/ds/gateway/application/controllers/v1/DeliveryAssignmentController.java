package com.ds.gateway.application.controllers.v1;

import com.ds.gateway.common.interfaces.ISessionServiceClient;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/assignments")
@RequiredArgsConstructor
@Slf4j
@Validated
public class DeliveryAssignmentController {

    private final ISessionServiceClient assignmentClient;

    @GetMapping("/session/delivery-man/{deliveryManId}/tasks/today")
    public ResponseEntity<?> getDailyTasks(
            @PathVariable UUID deliveryManId,
            @RequestParam(required = false) List<String> status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return assignmentClient.getDailyTasks(deliveryManId, status, page, size);
    }

    @GetMapping("/session/{sessionId}/tasks")
    public ResponseEntity<?> getTasksBySessionId(
            @PathVariable UUID sessionId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.debug("[api-gateway] [DeliveryAssignmentController.getTasksBySessionId] GET /api/v1/assignments/session/{}/tasks - Get tasks by session ID", sessionId);
        return assignmentClient.getTasksBySessionId(sessionId, page, size);
    }

    @GetMapping("/session/delivery-man/{deliveryManId}/tasks")
    public ResponseEntity<?> getTasksHistory(
            @PathVariable UUID deliveryManId,
            @RequestParam(required = false) List<String> status,
            @RequestParam(required = false) String createdAtStart,
            @RequestParam(required = false) String createdAtEnd,
            @RequestParam(required = false) String completedAtStart,
            @RequestParam(required = false) String completedAtEnd,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return assignmentClient.getTasksHistory(
                deliveryManId, status, createdAtStart, createdAtEnd,
                completedAtStart, completedAtEnd, page, size);
    }

    @PostMapping("/drivers/{deliveryManId}/parcels/{parcelId}/complete")
    public ResponseEntity<?> completeTask(@PathVariable UUID deliveryManId,
                                          @PathVariable UUID parcelId,
                                          @Valid @RequestBody Object routeInfo) {
        return assignmentClient.completeTask(deliveryManId, parcelId, routeInfo);
    }

    @PostMapping("/drivers/{deliveryManId}/parcels/{parcelId}/fail")
    public ResponseEntity<?> failTask(@PathVariable UUID deliveryManId,
                                      @PathVariable UUID parcelId,
                                      @Valid @RequestBody Object taskFailRequest) {
        return assignmentClient.failTask(deliveryManId, parcelId, taskFailRequest);
    }

    @PostMapping("/drivers/{deliveryManId}/parcels/{parcelId}/refuse")
    public ResponseEntity<?> refuseTask(@PathVariable UUID deliveryManId,
                                        @PathVariable UUID parcelId) {
        return assignmentClient.refuseTask(deliveryManId, parcelId);
    }

    @PostMapping("/drivers/{deliveryManId}/parcels/{parcelId}/postpone")
    public ResponseEntity<?> postponeTask(@PathVariable UUID deliveryManId,
                                          @PathVariable UUID parcelId,
                                          @RequestBody String addInfo) {
        return assignmentClient.postponeTask(deliveryManId, parcelId, addInfo);
    }

    @GetMapping("/current-shipper/parcels/{parcelId}")
    public ResponseEntity<?> lastestShipperForParcel(@PathVariable UUID parcelId) {
        return assignmentClient.lastestShipperForParcel(parcelId);
    }
}
