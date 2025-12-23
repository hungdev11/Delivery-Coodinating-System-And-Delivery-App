package com.ds.session.session_service.application.controllers;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ds.session.session_service.common.entities.dto.common.BaseResponse;
import com.ds.session.session_service.common.entities.dto.request.CalculateDeliveryTimeRequest;
import com.ds.session.session_service.common.entities.dto.request.CreateSessionRequest;
import com.ds.session.session_service.common.entities.dto.request.ScanParcelRequest;
import com.ds.session.session_service.common.entities.dto.request.SessionFailRequest;
import com.ds.session.session_service.common.entities.dto.request.UpdateAssignmentStatusRequest;
import com.ds.session.session_service.common.entities.dto.response.AssignmentResponse;
import com.ds.session.session_service.common.entities.dto.response.DeliveryAssignmentResponse;
import com.ds.session.session_service.common.entities.dto.response.DeliveryTimeResponse;
import com.ds.session.session_service.common.entities.dto.response.SessionResponse;
import com.ds.session.session_service.common.interfaces.IDeliveryAssignmentService;
import com.ds.session.session_service.common.interfaces.ISessionService;

import jakarta.validation.Valid;
import jakarta.websocket.server.PathParam;
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
    private final IDeliveryAssignmentService assignmentService;

    /**
     * API này dùng cho logic "Quét và chấp nhận đơn hàng"
     * Service sẽ tự "tìm hoặc tạo" (find-or-create) phiên làm việc.
     */
    @PostMapping("/drivers/{deliveryManId}/accept-parcel")
    public ResponseEntity<BaseResponse<AssignmentResponse>> acceptParcelToSession(
            @PathVariable("deliveryManId") String deliveryManId,
            @Valid @RequestBody ScanParcelRequest request) {
        log.debug("Shipper {} is accepting parcel {}", deliveryManId, request.getParcelId());

        AssignmentResponse response = sessionService.acceptParcelToSession(deliveryManId, request.getParcelId());
        return ResponseEntity.status(HttpStatus.CREATED).body(BaseResponse.success(response));
    }

    /**
     * Lấy thông tin chi tiết của một phiên, bao gồm tất cả các task bên trong.
     */
    @GetMapping("/{sessionId}")
    public ResponseEntity<BaseResponse<SessionResponse>> getSessionById(@PathVariable UUID sessionId) {
        log.debug("Fetching session details for id={}", sessionId);
        SessionResponse response = sessionService.getSessionById(sessionId);
        return ResponseEntity.ok(BaseResponse.success(response));
    }

    /**
     * API này được shipper gọi khi chủ động bấm "Kết thúc phiên" trên ứng dụng.
     */
    @PostMapping("/{sessionId}/complete")
    public ResponseEntity<BaseResponse<SessionResponse>> completeSession(@PathVariable UUID sessionId) {
        log.debug("Shipper completing session {}", sessionId);
        SessionResponse response = sessionService.completeSession(sessionId);
        return ResponseEntity.ok(BaseResponse.success(response));
    }

    /**
     * API này được shipper gọi khi báo cáo sự cố (hỏng xe, tai nạn...)
     */
    @PostMapping("/{sessionId}/fail")
    public ResponseEntity<BaseResponse<SessionResponse>> failSession(
            @PathVariable UUID sessionId,
            @Valid @RequestBody SessionFailRequest request) {
        log.debug("Shipper failing session {} with reason: {}", sessionId, request.getReason());
        SessionResponse response = sessionService.failSession(sessionId, request.getReason());
        return ResponseEntity.ok(BaseResponse.success(response));
    }

    /**
     * (Tùy chọn) API này dùng để tạo phiên với HÀNG LOẠT đơn hàng
     * (thay vì quét từng đơn).
     */
    @PostMapping
    public ResponseEntity<BaseResponse<SessionResponse>> createSessionBatch(
            @Valid @RequestBody CreateSessionRequest request) {
        log.debug("Creating new batch session for delivery man {}", request.getDeliveryManId());
        SessionResponse response = sessionService.createSession(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(BaseResponse.success(response));
    }

    /**
     * Tạo phiên ở trạng thái CREATED (chuẩn bị nhận đơn).
     * Shipper nhấn "Bắt đầu phiên" để tạo phiên này.
     */
    @PostMapping("/drivers/{deliveryManId}/prepare")
    public ResponseEntity<BaseResponse<SessionResponse>> createSessionPrepared(
            @PathVariable("deliveryManId") String deliveryManId) {
        log.debug("Creating prepared session (CREATED) for delivery man {}", deliveryManId);
        SessionResponse response = sessionService.createSessionPrepared(deliveryManId);
        return ResponseEntity.status(HttpStatus.CREATED).body(BaseResponse.success(response));
    }

    /**
     * Chuyển phiên từ CREATED sang IN_PROGRESS (bắt đầu giao hàng).
     * Shipper nhấn "Bắt đầu giao" để chuyển trạng thái.
     */
    @PostMapping("/{sessionId}/start")
    public ResponseEntity<BaseResponse<SessionResponse>> startSession(
            @PathVariable UUID sessionId,
            @RequestBody(required = false) com.ds.session.session_service.common.entities.dto.request.StartSessionRequest request) {
        log.debug("Starting session {} (CREATED -> IN_PROGRESS)", sessionId);
        SessionResponse response = sessionService.startSession(sessionId, request);
        return ResponseEntity.ok(BaseResponse.success(response));
    }

    /**
     * Lấy phiên đang hoạt động (CREATED hoặc IN_PROGRESS) của shipper.
     * Dùng để kiểm tra xem shipper có đang có phiên hoạt động không.
     */
    @GetMapping("/drivers/{deliveryManId}/active")
    public ResponseEntity<BaseResponse<SessionResponse>> getActiveSession(
            @PathVariable("deliveryManId") String deliveryManId) {
        log.debug("Getting active session for delivery man {}", deliveryManId);
        SessionResponse response = sessionService.getActiveSession(deliveryManId);
        if (response == null) {
            return ResponseEntity.ok(BaseResponse.error("Không tìm thấy phiên làm việc đang hoạt động"));
        }
        return ResponseEntity.ok(BaseResponse.success(response));
    }

    /**
     * Lấy tất cả sessions của một shipper.
     * Có thể exclude một parcelId cụ thể (dùng khi cần lấy sessions khác ngoài
     * session hiện tại chứa parcel này).
     * 
     * @param deliveryManId   ID của shipper
     * @param excludeParcelId (Optional) ParcelId để exclude - không trả về sessions
     *                        chứa parcel này
     * @return Danh sách sessions của shipper
     */
    @GetMapping("/drivers/{deliveryManId}/sessions")
    public ResponseEntity<BaseResponse<java.util.List<SessionResponse>>> getAllSessionsForDeliveryMan(
            @PathVariable("deliveryManId") String deliveryManId,
            @RequestParam(required = false) String excludeParcelId) {
        log.debug("Getting all sessions for delivery man {} (excludeParcelId: {})", deliveryManId, excludeParcelId);
        java.util.List<SessionResponse> sessions = sessionService.getAllSessionsForDeliveryMan(deliveryManId,
                excludeParcelId);
        return ResponseEntity.ok(BaseResponse.success(sessions));
    }

    /**
     * Update assignment status by sessionId and assignmentId
     * This endpoint is used by API gateway for nested queries
     */
    @PutMapping("/{sessionId}/assignments/{assignmentId}/status")
    public ResponseEntity<BaseResponse<DeliveryAssignmentResponse>> updateAssignmentStatus(
            @PathVariable UUID sessionId,
            @PathVariable UUID assignmentId,
            @Valid @RequestBody UpdateAssignmentStatusRequest request) {
        log.debug("Updating assignment {} status in session {}", assignmentId, sessionId);
        DeliveryAssignmentResponse response = assignmentService.updateAssignmentStatus(sessionId, assignmentId,
                request);
        return ResponseEntity.ok(BaseResponse.success(response));
    }

    /**
     * Calculate delivery time for a list of parcels
     * Used to validate if postpone time is within session time
     * Formula: route duration + (5 minutes * number of parcels)
     */
    @PostMapping("/calculate-delivery-time")
    public ResponseEntity<BaseResponse<DeliveryTimeResponse>> calculateDeliveryTime(
            @Valid @RequestBody CalculateDeliveryTimeRequest request) {
        log.debug("Calculating delivery time for {} parcels", request.getParcelIds().size());
        DeliveryTimeResponse response = sessionService.calculateDeliveryTime(request);
        return ResponseEntity.ok(BaseResponse.success(response));
    }

    /**
     * Transfer a parcel from current shipper to another shipper
     * Only allows transferring ON_ROUTE parcels
     */
    @PostMapping("/drivers/{deliveryManId}/transfer-parcel")
    public ResponseEntity<BaseResponse<AssignmentResponse>> transferParcel(
            @PathVariable("deliveryManId") String deliveryManId,
            @Valid @RequestBody com.ds.session.session_service.common.entities.dto.request.TransferParcelRequest request) {
        log.debug("Shipper {} transferring parcel {} to session {}", deliveryManId, request.getParcelId(),
                request.getTargetSessionId());
        AssignmentResponse response = sessionService.transferParcel(deliveryManId, request);
        return ResponseEntity.ok(BaseResponse.success(response));
    }

    /**
     * Accept a transferred parcel by scanning source session QR
     */
    @PostMapping("/drivers/{deliveryManId}/accept-transferred-parcel")
    public ResponseEntity<BaseResponse<AssignmentResponse>> acceptTransferredParcel(
            @PathVariable("deliveryManId") String deliveryManId,
            @Valid @RequestBody com.ds.session.session_service.common.entities.dto.request.AcceptTransferredParcelRequest request) {
        log.debug("Shipper {} accepting transferred parcel {} from session {}", deliveryManId, request.getParcelId(),
                request.getSourceSessionId());
        AssignmentResponse response = sessionService.acceptTransferredParcel(deliveryManId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(BaseResponse.success(response));
    }

    @GetMapping("/list-must-return-warehouse/{sessionId}")
    public ResponseEntity<BaseResponse<List<AssignmentResponse>>> listMustReturnToWarehouse(
            @PathVariable("sessionId") String sessionId) {
        return ResponseEntity.ok(BaseResponse.success(sessionService.listAssignmentsMustReturnWarehouse(sessionId)));
    }
}
