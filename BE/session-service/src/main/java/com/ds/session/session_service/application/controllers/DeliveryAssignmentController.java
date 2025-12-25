package com.ds.session.session_service.application.controllers;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ds.session.session_service.app_context.models.DeliveryAssignment;
import com.ds.session.session_service.common.entities.dto.common.BaseResponse;
import com.ds.session.session_service.common.entities.dto.request.CompleteTaskRequest;
import com.ds.session.session_service.common.entities.dto.request.PostponeAssignmentRequest;
import com.ds.session.session_service.common.entities.dto.request.RouteInfo;
import com.ds.session.session_service.common.entities.dto.request.TaskFailRequest;
import com.ds.session.session_service.common.entities.dto.response.DeliveryAssignmentResponse;
import com.ds.session.session_service.common.entities.dto.response.LatestAssignmentResponse;
import com.ds.session.session_service.common.entities.dto.response.PageResponse;
import com.ds.session.session_service.common.entities.dto.response.ShipperInfo;
import com.ds.session.session_service.common.interfaces.IDeliveryAssignmentService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Controller này quản lý các HÀNH ĐỘNG trên TỪNG TASK (ASSIGNMENT).
 * (File này đã chính xác từ lần trước)
 */
@RestController
@RequestMapping("/api/v1/assignments")
@RequiredArgsConstructor
@Slf4j
@Validated
public class DeliveryAssignmentController {

    private final IDeliveryAssignmentService assignmentService;

    /**
     * Lấy tất cả các task trong "phiên" (session) ĐANG HOẠT ĐỘNG của shipper.
     * 
     * @deprecated Use getTasksBySessionId instead. This endpoint will be removed in
     *             a future version.
     */
    @Deprecated
    @GetMapping("/session/delivery-man/{deliveryManId}/tasks/today")
    public ResponseEntity<BaseResponse<PageResponse<DeliveryAssignmentResponse>>> getDailyTasks(
            @PathVariable UUID deliveryManId,
            @RequestParam(required = false) List<String> status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.debug("Fetching daily (active session) tasks for shipper {} with status filter: {}", deliveryManId, status);
        PageResponse<DeliveryAssignmentResponse> response = assignmentService.getDailyTasks(deliveryManId, status, page,
                size);
        return ResponseEntity.ok(BaseResponse.success(response));
    }

    /**
     * Lấy tất cả các task của một session cụ thể theo sessionId (phân trang).
     */
    @GetMapping("/session/{sessionId}/tasks")
    public ResponseEntity<BaseResponse<PageResponse<DeliveryAssignmentResponse>>> getTasksBySessionId(
            @PathVariable UUID sessionId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.debug("Fetching tasks for session {} (page: {}, size: {})", sessionId, page, size);
        PageResponse<DeliveryAssignmentResponse> response = assignmentService.getTasksBySessionId(sessionId, page,
                size);
        return ResponseEntity.ok(BaseResponse.success(response));
    }

    /**
     * Lấy lịch sử task (từ các phiên ĐÃ HOÀN THÀNH) của shipper.
     */
    @GetMapping("/session/delivery-man/{deliveryManId}/tasks")
    public ResponseEntity<BaseResponse<PageResponse<DeliveryAssignmentResponse>>> getTasksHistory(
            @PathVariable UUID deliveryManId,
            @RequestParam(required = false) List<String> status,
            @RequestParam(required = false) String createdAtStart,
            @RequestParam(required = false) String createdAtEnd,
            @RequestParam(required = false) String completedAtStart,
            @RequestParam(required = false) String completedAtEnd,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.debug("Fetching tasks history for shipper {} with filters.", deliveryManId);
        log.debug("Status: {}, CreatedAt: {} to {}, CompletedAt: {} to {}",
                status, createdAtStart, createdAtEnd, completedAtStart, completedAtEnd);
        PageResponse<DeliveryAssignmentResponse> response = assignmentService.getTasksBetween(
                deliveryManId, status,
                createdAtStart, createdAtEnd,
                completedAtStart, completedAtEnd,
                page, size);
        return ResponseEntity.ok(BaseResponse.success(response));
    }

    /**
     * API shipper gọi khi giao HÀNG THÀNH CÔNG một task.
     * Accepts CompleteTaskRequest with routeInfo and proofImageUrls.
     * 
     * @deprecated Use completeTaskByAssignmentId instead for better performance
     */
    @PostMapping("/drivers/{deliveryManId}/parcels/{parcelId}/complete")
    public ResponseEntity<BaseResponse<DeliveryAssignmentResponse>> completeTask(
            @PathVariable UUID deliveryManId,
            @PathVariable UUID parcelId,
            @Valid @RequestBody CompleteTaskRequest request) {
        log.debug("Shipper {} completing task for parcel {} with proof images: {}", deliveryManId, parcelId, request.getProofImageUrls());
        DeliveryAssignmentResponse response = assignmentService.completeTask(
                parcelId,
                deliveryManId,
                request);
        return ResponseEntity.ok(BaseResponse.success(response));
    }

    /**
     * API shipper gọi khi giao HÀNG THÀNH CÔNG một task bằng assignmentId.
     * Hiệu quả hơn vì không cần query tìm assignment.
     * Accepts CompleteTaskRequest with routeInfo and proofImageUrls.
     */
    @PostMapping("/{assignmentId}/complete")
    public ResponseEntity<BaseResponse<DeliveryAssignmentResponse>> completeTaskByAssignmentId(
            @PathVariable UUID assignmentId,
            @Valid @RequestBody CompleteTaskRequest request) {
        log.debug("Completing assignment {} with proof images: {}", assignmentId, request.getProofImageUrls());
        DeliveryAssignmentResponse response = assignmentService.completeTaskByAssignmentId(
                assignmentId,
                request);
        return ResponseEntity.ok(BaseResponse.success(response));
    }

    /**
     * API shipper gọi khi GIAO HÀNG THẤT BẠI một task.
     */
    @PostMapping("/{assignmentId}/fail")
    public ResponseEntity<BaseResponse<DeliveryAssignmentResponse>> failTask(
            @PathVariable UUID assignmentId,
            @Valid @RequestBody TaskFailRequest request) {
        log.debug("Shipper failing task for assignment {} (Reason: {})", assignmentId, request.getReason());
        DeliveryAssignmentResponse response = assignmentService.deliveryFailed(
                assignmentId,
                request.getReason(),
                request.getRouteInfo());
        return ResponseEntity.ok(BaseResponse.success(response));
    }

    /**
     * API shipper gọi khi khách từ chối một task.
     */
    @PostMapping("/{assignmentId}/refuse")
    public ResponseEntity<BaseResponse<DeliveryAssignmentResponse>> refuseTask(
            @PathVariable UUID assignmentId) {
        log.debug("Shipper flagging assignment {} as REFUSED and accept by both side", assignmentId);
        DeliveryAssignmentResponse response = assignmentService.rejectedByCustomer(
                assignmentId,
                "Khách từ chối nhận",
                new RouteInfo());
        return ResponseEntity.ok(BaseResponse.success(response));
    }

    /**
     * API shipper gọi khi trả hàng về kho (cho các đơn FAILED hoặc DELAYED).
     * Upload proofs với type RETURNED, không thay đổi assignment status.
     */
    @PostMapping("/{assignmentId}/return-to-warehouse")
    public ResponseEntity<BaseResponse<DeliveryAssignmentResponse>> returnToWarehouse(
            @PathVariable UUID assignmentId,
            @Valid @RequestBody CompleteTaskRequest request) {
        log.debug("Recording return to warehouse for assignment {} with {} proof(s)", 
                assignmentId, request.getProofImageUrls() != null ? request.getProofImageUrls().size() : 0);
        DeliveryAssignmentResponse response = assignmentService.returnToWarehouse(
                assignmentId,
                request);
        return ResponseEntity.ok(BaseResponse.success(response));
    }

    /**
     * API shipper gọi khi khách báo hoãn (loại thẳng khỏi danh sách đang giao nếu
     * shipper chấp nhận - k xử lý các case thời gian).
     */
//     @PostMapping("/drivers/{deliveryManId}/parcels/{parcelId}/postpone")
//     public ResponseEntity<BaseResponse<DeliveryAssignmentResponse>> postponeTask(
//             @PathVariable UUID assignmentId,
//             @RequestBody String addInfo) {
//         log.debug("Shipper flagging assignment {} as POSTPONE and accept by both side", assignmentId);
//         DeliveryAssignmentResponse response = assignmentService.postponeByCustomer(
//                 assignmentId,
//                 "Khách yêu cầu hoãn với thời gian: " + addInfo,
//                 new RouteInfo());
//         return ResponseEntity.ok(BaseResponse.success(response));
//     }

    @GetMapping("/current-shipper/parcels/{parcelId}")
    public ResponseEntity<BaseResponse<ShipperInfo>> getCurrentShipperInfoForParcel(@PathVariable String parcelId) {
        Optional<ShipperInfo> shipperOpt = assignmentService.getLatestDriverIdForParcel(parcelId);
        if (shipperOpt.isPresent()) {
            return ResponseEntity.ok(BaseResponse.success(shipperOpt.get()));
        } else {
            return ResponseEntity.ok(BaseResponse.error("Không tìm thấy thông tin shipper cho đơn hàng này"));
        }
    }

    /**
     * Get active assignment ID for a parcel and delivery man.
     * Returns the assignmentId of the active assignment (in CREATED or IN_PROGRESS
     * session).
     * This is used by Communication Service to find assignmentId before calling
     * postpone endpoint.
     */
    @GetMapping("/active")
    public ResponseEntity<BaseResponse<UUID>> getActiveAssignmentId(
            @RequestParam String parcelId,
            @RequestParam String deliveryManId) {
        log.debug("Getting active assignmentId for parcelId: {} and deliveryManId: {}", parcelId, deliveryManId);
        Optional<UUID> assignmentIdOpt = assignmentService.getActiveAssignmentId(parcelId, deliveryManId);
        if (assignmentIdOpt.isPresent()) {
            return ResponseEntity.ok(BaseResponse.success(assignmentIdOpt.get()));
        } else {
            return ResponseEntity.ok(BaseResponse.error("Không tìm thấy assignment đang hoạt động"));
        }
    }

    /**
     * Get the latest assignment (any session status) for a parcel.
     * Used by Parcel Service to resolve assignment/session when client confirms
     * delivery.
     */
    @GetMapping("/parcel/{parcelId}/latest-assignment")
    public ResponseEntity<BaseResponse<LatestAssignmentResponse>> getLatestAssignmentForParcel(
            @PathVariable String parcelId) {
        Optional<LatestAssignmentResponse> infoOpt = assignmentService.getLatestAssignmentForParcel(parcelId);
        return infoOpt
                .map(info -> ResponseEntity.ok(BaseResponse.success(info)))
                .orElseGet(() -> ResponseEntity.ok(BaseResponse.error("Không tìm thấy assignment cho đơn hàng này")));
    }

    /**
     * Postpone assignment directly by assignmentId.
     * This endpoint is used when we already have the assignmentId (e.g., from
     * proposal response).
     * Supports:
     * - postponeDateTime: Check if postpone is outside session time
     * - moveToEnd: If postpone is within session time, move parcel to end of route
     * instead of DELAY
     */
    @PutMapping("/{assignmentId}/postpone")
    public ResponseEntity<BaseResponse<DeliveryAssignmentResponse>> postponeByAssignmentId(
            @PathVariable UUID assignmentId,
            @Valid @RequestBody PostponeAssignmentRequest request) {
        log.debug("Postponing assignment {} with reason: {}, postponeDateTime: {}, moveToEnd: {}",
                assignmentId, request.getReason(), request.getPostponeDateTime(), request.getMoveToEnd());
        DeliveryAssignmentResponse response = assignmentService.postponeByAssignmentId(assignmentId, request);
        return ResponseEntity.ok(BaseResponse.success(response));
    }

    
    @PostMapping("/drivers/{deliveryManId}/parcels/{parcelId}/complete-with-urls")
    public ResponseEntity<BaseResponse<DeliveryAssignmentResponse>> completeTaskWithUrls(
            @PathVariable UUID deliveryManId,
            @PathVariable UUID parcelId,
            @RequestBody CompleteTaskRequest request) {
        log.debug("Completing task for parcel {} with proof images: {}", parcelId, request.getProofImageUrls());
        // Reuse the incoming DTO directly so we don't lose location fields (currentLat/Lon/Timestamp)
        DeliveryAssignmentResponse response = assignmentService.completeTask(
                parcelId,
                deliveryManId,
                request
        );
        return ResponseEntity.ok(BaseResponse.success(response));
    }

    @PatchMapping("/{assignmentId}/drivers/{deliveryManId}/accept")
    public DeliveryAssignmentResponse acceptTask(@PathVariable UUID assignmentId, @PathVariable UUID deliveryManId) {
        log.debug("Accepting task for assignment {} by delivery man {}", assignmentId, deliveryManId);
        return assignmentService.acceptTask(deliveryManId, assignmentId);
    }

    @GetMapping("/drivers/{deliveryManId}/assigned-task")
    public ResponseEntity<BaseResponse<PageResponse<DeliveryAssignmentResponse>>> getAssignedTask(
        @PathVariable UUID deliveryManId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.debug("Getting assigned task for delivery man {}", deliveryManId);
        return ResponseEntity.ok(BaseResponse.success(
            assignmentService.getAssignedTaskForShipper(deliveryManId, page, size)
        ));
    }
    
}
