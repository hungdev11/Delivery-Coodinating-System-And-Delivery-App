package com.ds.gateway.application.controllers.v1;

import com.ds.gateway.annotations.AuthRequired;
import com.ds.gateway.common.interfaces.ISessionServiceClient;
import com.ds.gateway.infrastructure.http.ProxyHttpClient;
import com.ds.gateway.infrastructure.logging.ProxyLogContext;
import com.ds.gateway.infrastructure.logging.ProxyRequestLogger;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/assignments")
@RequiredArgsConstructor
@Slf4j
@Validated
public class DeliveryAssignmentController {

    private final ISessionServiceClient assignmentClient;
    
    private final ProxyHttpClient proxyHttpClient;
    private final ProxyRequestLogger proxyRequestLogger;

    @Value("${services.session.base-url}")
    private String sessionServiceUrl;

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

    @PostMapping("/drivers/{deliveryManId}/parcels/{parcelId}/complete-with-urls")
    public ResponseEntity<?> completeTaskWithUrls(@PathVariable UUID deliveryManId,
                                                   @PathVariable UUID parcelId,
                                                   @Valid @RequestBody Object request) {
        log.debug("[api-gateway] [DeliveryAssignmentController.completeTaskWithUrls] POST /api/v1/assignments/drivers/{}/parcels/{}/complete-with-urls", deliveryManId, parcelId);
        // Both endpoints do the same thing - accept CompleteTaskRequest with routeInfo and proofImageUrls
        return assignmentClient.completeTask(deliveryManId, parcelId, request);
    }

    /**
     * Complete task by assignmentId - more efficient endpoint
     */
    @PostMapping("/{assignmentId}/complete")
    public ResponseEntity<?> completeTaskByAssignmentId(@PathVariable UUID assignmentId,
                                                        @Valid @RequestBody Object request) {
        log.debug("[api-gateway] [DeliveryAssignmentController.completeTaskByAssignmentId] POST /api/v1/assignments/{}/complete", assignmentId);
        return assignmentClient.completeTaskByAssignmentId(assignmentId, request);
    }

    /**
     * Record return to warehouse for FAILED/DELAYED assignments
     */
    @PostMapping("/{assignmentId}/return-to-warehouse")
    public ResponseEntity<?> returnToWarehouse(@PathVariable UUID assignmentId,
                                               @Valid @RequestBody Object request) {
        log.debug("[api-gateway] [DeliveryAssignmentController.returnToWarehouse] POST /api/v1/assignments/{}/return-to-warehouse", assignmentId);
        return assignmentClient.returnToWarehouse(assignmentId, request);
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

    /**
     * Get latest assignment info for a parcel (sessionId, assignmentId, status, deliveryManId)
     * Used by client parcel tracking to find active session for a parcel.
     */
    @GetMapping("/parcels/{parcelId}/latest-assignment")
    public ResponseEntity<?> getLatestAssignmentForParcel(@PathVariable String parcelId) {
        log.debug("[api-gateway] [DeliveryAssignmentController.getLatestAssignmentForParcel] GET /api/v1/assignments/parcels/{}/latest-assignment", parcelId);
        return assignmentClient.getLatestAssignmentForParcel(parcelId);
    }

    /**
     * Accept task by assignment ID (QR code scan)
     * POST /api/v1/assignments/{assignmentId}/accept?deliveryManId={deliveryManId}
     * Status transition: PENDING -> ACCEPTED
     */
    @PostMapping("/{assignmentId}/accept")
    @AuthRequired
    public ResponseEntity<?> acceptTaskByQR(
            @PathVariable UUID assignmentId,
            @RequestParam String deliveryManId) {
        log.debug("[api-gateway] [DeliveryAssignmentController.acceptTaskByQR] POST /api/v1/assignments/{}/accept?deliveryManId={} - Proxying to Session Service", assignmentId, deliveryManId);
        return proxySession(HttpMethod.POST, "/api/v1/assignments/" + assignmentId + "/accept?deliveryManId=" + deliveryManId, null);
    }

    private ResponseEntity<Object> proxySession(HttpMethod method, String path, Object body) {
        String url = sessionServiceUrl + path;
        ProxyLogContext context = proxyRequestLogger.start(method, "session-service", url, body);
        try {
            ResponseEntity<Object> response = proxyHttpClient.exchange(method, url, body, Object.class);
            proxyRequestLogger.success(context, response.getStatusCode().value());
            return response;
        } catch (ResourceAccessException e) {
            proxyRequestLogger.failure(context, 502, e.getMessage(), e);
            return ResponseEntity.status(502).body("{\"error\":\"Bad Gateway: Session Service unavailable\"}");
        } catch (HttpStatusCodeException e) {
            proxyRequestLogger.failure(context, e.getStatusCode().value(), e.getResponseBodyAsString(), e);
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        } catch (Exception e) {
            proxyRequestLogger.failure(context, 500, e.getMessage(), e);
            return ResponseEntity.status(500).body("{\"error\":\"Internal Server Error: " + e.getMessage() + "\"}");
        }
    }
}
