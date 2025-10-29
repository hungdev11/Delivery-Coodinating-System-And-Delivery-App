package com.ds.session.session_service.application.controllers;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ds.session.session_service.common.entities.dto.request.CreateSessionRequest;
import com.ds.session.session_service.common.entities.dto.request.ScanParcelRequest;
import com.ds.session.session_service.common.entities.dto.request.SessionFailRequest;
import com.ds.session.session_service.common.entities.dto.response.AssignmentResponse;
import com.ds.session.session_service.common.entities.dto.response.SessionResponse;
import com.ds.session.session_service.common.interfaces.ISessionService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Controller này quản lý VÒNG ĐỜI CỦA PHIÊN (SESSION).
 * Bao gồm tạo phiên, kết thúc phiên, hủy phiên, và thêm task vào phiên.
 */
@RestController
@RequestMapping("/api/v1/sessions")
@RequiredArgsConstructor
@Slf4j
@Validated
public class SessionController {

    private final ISessionService sessionService;

    /**
     * API này dùng cho logic "Quét và chấp nhận đơn hàng"
     * Service sẽ tự "tìm hoặc tạo" (find-or-create) phiên làm việc.
     */
    @PostMapping("/drivers/{deliveryManId}/accept-parcel")
    public ResponseEntity<AssignmentResponse> acceptParcelToSession(
            @PathVariable("deliveryManId") String deliveryManId,
            @Valid @RequestBody ScanParcelRequest request
    ) {
        log.info("Shipper {} is accepting parcel {}", deliveryManId, request.getParcelId());
        
        AssignmentResponse response = sessionService.acceptParcelToSession(deliveryManId, request.getParcelId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Lấy thông tin chi tiết của một phiên, bao gồm tất cả các task bên trong.
     */
    @GetMapping("/{sessionId}")
    public ResponseEntity<SessionResponse> getSessionById(@PathVariable UUID sessionId) {
        log.info("Fetching session details for id={}", sessionId);
        SessionResponse response = sessionService.getSessionById(sessionId);
        return ResponseEntity.ok(response);
    }

    /**
     * API này được shipper gọi khi chủ động bấm "Kết thúc phiên" trên ứng dụng.
     */
    @PostMapping("/{sessionId}/complete")
    public ResponseEntity<SessionResponse> completeSession(@PathVariable UUID sessionId) {
        log.info("Shipper completing session {}", sessionId);
        SessionResponse response = sessionService.completeSession(sessionId);
        return ResponseEntity.ok(response);
    }

    /**
     * API này được shipper gọi khi báo cáo sự cố (hỏng xe, tai nạn...)
     */
    @PostMapping("/{sessionId}/fail")
    public ResponseEntity<SessionResponse> failSession(
            @PathVariable UUID sessionId,
            @Valid @RequestBody SessionFailRequest request
    ) {
        log.warn("Shipper failing session {} with reason: {}", sessionId, request.getReason());
        SessionResponse response = sessionService.failSession(sessionId, request.getReason());
        return ResponseEntity.ok(response);
    }

    /**
     * (Tùy chọn) API này dùng để tạo phiên với HÀNG LOẠT đơn hàng
     * (thay vì quét từng đơn).
     */
    @PostMapping
    public ResponseEntity<SessionResponse> createSessionBatch(
            @Valid @RequestBody CreateSessionRequest request
    ) {
        log.info("Creating new batch session for delivery man {}", request.getDeliveryManId());
        SessionResponse response = sessionService.createSession(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}

