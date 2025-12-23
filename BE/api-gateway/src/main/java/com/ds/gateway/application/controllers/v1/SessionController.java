package com.ds.gateway.application.controllers.v1;

import com.ds.gateway.common.entities.dto.common.BaseResponse;
import com.ds.gateway.common.entities.dto.session.EnrichedSessionResponse;
import com.ds.gateway.common.interfaces.ISessionServiceClient;
import com.ds.gateway.business.v1.services.SessionEnrichmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/v1/sessions")
@RequiredArgsConstructor
@Slf4j
@Validated
public class SessionController {

    private final ISessionServiceClient sessionServiceClient;
    private final SessionEnrichmentService sessionEnrichmentService;

    @PostMapping("/drivers/{deliveryManId}/accept-parcel")
    public ResponseEntity<?> acceptParcelToSession(
            @PathVariable String deliveryManId,
            @Valid @RequestBody Object scanParcelRequest
    ) {
        return sessionServiceClient.acceptParcelToSession(deliveryManId, scanParcelRequest);
    }

    @GetMapping("/{sessionId}")
    public ResponseEntity<?> getSessionById(@PathVariable UUID sessionId) {
        return sessionServiceClient.getSessionById(sessionId);
    }

    /**
     * Get enriched session with full assignment details including parcel info and proofs
     * This endpoint aggregates data from session-service, parcel-service, and delivery-proofs
     */
    @GetMapping("/{sessionId}/enriched")
    public ResponseEntity<BaseResponse<EnrichedSessionResponse>> getEnrichedSessionById(@PathVariable UUID sessionId) {
        log.debug("[api-gateway] [SessionController.getEnrichedSessionById] GET /api/v1/sessions/{}/enriched", sessionId);
        try {
            CompletableFuture<EnrichedSessionResponse> future = sessionEnrichmentService.getEnrichedSession(sessionId);
            EnrichedSessionResponse enrichedSession = future.join();
            
            if (enrichedSession == null) {
                return ResponseEntity.ok(BaseResponse.error("Session not found or failed to enrich"));
            }
            
            return ResponseEntity.ok(BaseResponse.success(enrichedSession));
        } catch (Exception e) {
            log.error("[api-gateway] [SessionController.getEnrichedSessionById] Error enriching session {}", sessionId, e);
            return ResponseEntity.ok(BaseResponse.error("Failed to enrich session: " + e.getMessage()));
        }
    }

    @PostMapping("/{sessionId}/complete")
    public ResponseEntity<?> completeSession(@PathVariable UUID sessionId) {
        return sessionServiceClient.completeSession(sessionId);
    }

    @PostMapping("/{sessionId}/fail")
    public ResponseEntity<?> failSession(
            @PathVariable UUID sessionId,
            @Valid @RequestBody Object sessionFailRequest
    ) {
        return sessionServiceClient.failSession(sessionId, sessionFailRequest);
    }

    @PostMapping
    public ResponseEntity<?> createSessionBatch(@Valid @RequestBody Object createSessionRequest) {
        return sessionServiceClient.createSessionBatch(createSessionRequest);
    }

    /**
     * Tạo phiên ở trạng thái CREATED (chuẩn bị nhận đơn).
     * Shipper nhấn "Bắt đầu phiên" để tạo phiên này.
     */
    @PostMapping("/drivers/{deliveryManId}/prepare")
    public ResponseEntity<?> createSessionPrepared(@PathVariable String deliveryManId) {
        log.debug("[api-gateway] [SessionController.createSessionPrepared] Creating prepared session (CREATED) for delivery man {}", deliveryManId);
        return sessionServiceClient.createSessionPrepared(deliveryManId);
    }

    /**
     * Chuyển phiên từ CREATED sang IN_PROGRESS (bắt đầu giao hàng).
     * Shipper nhấn "Bắt đầu giao" để chuyển trạng thái.
     */
    @PostMapping("/{sessionId}/start")
    public ResponseEntity<?> startSession(
            @PathVariable UUID sessionId,
            @RequestBody(required = false) Object startSessionRequest) {
        log.debug("[api-gateway] [SessionController.startSession] Starting session {} (CREATED -> IN_PROGRESS)", sessionId);
        return sessionServiceClient.startSession(sessionId, startSessionRequest);
    }

    /**
     * Lấy phiên đang hoạt động (CREATED hoặc IN_PROGRESS) của shipper.
     * Dùng để kiểm tra xem shipper có đang có phiên hoạt động không.
     */
    @GetMapping("/drivers/{deliveryManId}/active")
    public ResponseEntity<?> getActiveSession(@PathVariable String deliveryManId) {
        log.debug("[api-gateway] [SessionController.getActiveSession] Getting active session for delivery man {}", deliveryManId);
        return sessionServiceClient.getActiveSession(deliveryManId);
    }

    /**
     * Lấy tất cả sessions của một shipper.
     * Có thể exclude một parcelId cụ thể (dùng khi cần lấy sessions khác ngoài
     * session hiện tại chứa parcel này).
     */
    @GetMapping("/drivers/{deliveryManId}/sessions")
    public ResponseEntity<?> getAllSessionsForDeliveryMan(
            @PathVariable String deliveryManId,
            @RequestParam(required = false) String excludeParcelId) {
        log.debug("[api-gateway] [SessionController.getAllSessionsForDeliveryMan] Getting all sessions for delivery man {} (excludeParcelId: {})", deliveryManId, excludeParcelId);
        return sessionServiceClient.getAllSessionsForDeliveryMan(deliveryManId, excludeParcelId);
    }
}
