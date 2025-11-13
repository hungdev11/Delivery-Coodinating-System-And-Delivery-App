package com.ds.gateway.application.controllers.v1;

import com.ds.gateway.common.interfaces.ISessionServiceClient;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/sessions")
@RequiredArgsConstructor
@Slf4j
@Validated
public class SessionController {

    private final ISessionServiceClient sessionServiceClient;

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
        log.info("Creating prepared session (CREATED) for delivery man {}", deliveryManId);
        return sessionServiceClient.createSessionPrepared(deliveryManId);
    }

    /**
     * Chuyển phiên từ CREATED sang IN_PROGRESS (bắt đầu giao hàng).
     * Shipper nhấn "Bắt đầu giao" để chuyển trạng thái.
     */
    @PostMapping("/{sessionId}/start")
    public ResponseEntity<?> startSession(@PathVariable UUID sessionId) {
        log.info("Starting session {} (CREATED -> IN_PROGRESS)", sessionId);
        return sessionServiceClient.startSession(sessionId);
    }

    /**
     * Lấy phiên đang hoạt động (CREATED hoặc IN_PROGRESS) của shipper.
     * Dùng để kiểm tra xem shipper có đang có phiên hoạt động không.
     */
    @GetMapping("/drivers/{deliveryManId}/active")
    public ResponseEntity<?> getActiveSession(@PathVariable String deliveryManId) {
        log.info("Getting active session for delivery man {}", deliveryManId);
        return sessionServiceClient.getActiveSession(deliveryManId);
    }
}
