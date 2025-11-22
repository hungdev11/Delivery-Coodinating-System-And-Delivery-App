package com.ds.session.session_service.application.controllers;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PutMapping;

import com.ds.session.session_service.common.entities.dto.common.BaseResponse;
import com.ds.session.session_service.common.entities.dto.request.RouteInfo;
import com.ds.session.session_service.common.entities.dto.request.TaskFailRequest;
import com.ds.session.session_service.common.entities.dto.response.DeliveryAssignmentResponse;
import com.ds.session.session_service.common.entities.dto.response.LatestAssignmentResponse;
import com.ds.session.session_service.common.entities.dto.response.PageResponse;
import com.ds.session.session_service.common.entities.dto.response.ShipperInfo;
import com.ds.session.session_service.common.interfaces.IDeliveryAssignmentService;

import jakarta.validation.Valid;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
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
     * @deprecated Use getTasksBySessionId instead. This endpoint will be removed in a future version.
     */
    @Deprecated
    @GetMapping("/session/delivery-man/{deliveryManId}/tasks/today")
    public ResponseEntity<PageResponse<DeliveryAssignmentResponse>> getDailyTasks(
        @PathVariable UUID deliveryManId,
        @RequestParam(required = false) List<String> status,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size
    ) {
        log.info("Fetching daily (active session) tasks for shipper {} with status filter: {}", deliveryManId, status);
        PageResponse<DeliveryAssignmentResponse> response = assignmentService.getDailyTasks(deliveryManId, status, page, size);
        return ResponseEntity.ok(response);
    }

    /**
     * Lấy tất cả các task của một session cụ thể theo sessionId (phân trang).
     */
    @GetMapping("/session/{sessionId}/tasks")
    public ResponseEntity<PageResponse<DeliveryAssignmentResponse>> getTasksBySessionId(
        @PathVariable UUID sessionId,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size
    ) {
        log.info("Fetching tasks for session {} (page: {}, size: {})", sessionId, page, size);
        PageResponse<DeliveryAssignmentResponse> response = assignmentService.getTasksBySessionId(sessionId, page, size);
        return ResponseEntity.ok(response);
    }

    /**
     * Lấy lịch sử task (từ các phiên ĐÃ HOÀN THÀNH) của shipper.
     */
    @GetMapping("/session/delivery-man/{deliveryManId}/tasks")
    public ResponseEntity<PageResponse<DeliveryAssignmentResponse>> getTasksHistory(
        @PathVariable UUID deliveryManId,
        @RequestParam(required = false) List<String> status,
        @RequestParam(required = false) String createdAtStart,
        @RequestParam(required = false) String createdAtEnd,
        @RequestParam(required = false) String completedAtStart,
        @RequestParam(required = false) String completedAtEnd,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size
    ) {
        log.info("Fetching tasks history for shipper {} with filters.", deliveryManId);
        log.info("Status: {}, CreatedAt: {} to {}, CompletedAt: {} to {}", 
            status, createdAtStart, createdAtEnd, completedAtStart, completedAtEnd);
        PageResponse<DeliveryAssignmentResponse> response = assignmentService.getTasksBetween(
            deliveryManId, status, 
            createdAtStart, createdAtEnd, 
            completedAtStart, completedAtEnd, 
            page, size
        );
        return ResponseEntity.ok(response);
    }

    /**
     * API shipper gọi khi giao HÀNG THÀNH CÔNG một task.
     */
    @PostMapping("/drivers/{deliveryManId}/parcels/{parcelId}/complete")
    public ResponseEntity<DeliveryAssignmentResponse> completeTask(
        @PathVariable UUID deliveryManId,
        @PathVariable UUID parcelId,
        @Valid @RequestBody RouteInfo routeInfo
    ) {
        log.info("Shipper {} completing task for parcel {}", deliveryManId, parcelId);
        DeliveryAssignmentResponse response = assignmentService.completeTask(parcelId, deliveryManId, routeInfo);
        return ResponseEntity.ok(response);
    }

    /**
     * API shipper gọi khi GIAO HÀNG THẤT BẠI một task.
     */
    @PostMapping("/drivers/{deliveryManId}/parcels/{parcelId}/fail")
    public ResponseEntity<DeliveryAssignmentResponse> failTask(
        @PathVariable UUID deliveryManId,
        @PathVariable UUID parcelId,
        @Valid @RequestBody TaskFailRequest request
    ) {
        log.info("Shipper {} failing task for parcel {} (Reason: {})", deliveryManId, parcelId, request.getReason());
        DeliveryAssignmentResponse response = assignmentService.deliveryFailed(
            parcelId, 
            deliveryManId, 
            request.getReason(),
            request.getRouteInfo()
        );
        return ResponseEntity.ok(response);
    }

    /**
     * API shipper gọi khi khách từ chối một task.
     */
    @PostMapping("/drivers/{deliveryManId}/parcels/{parcelId}/refuse")
    public ResponseEntity<DeliveryAssignmentResponse> refuseTask(
        @PathVariable UUID deliveryManId,
        @PathVariable UUID parcelId
    ) {
        log.info("Shipper {} flagging parcel {} as REFUSED and accept by both side", deliveryManId, parcelId);
        DeliveryAssignmentResponse response = assignmentService.rejectedByCustomer(
            parcelId, 
            deliveryManId, 
            "Khách từ chối nhận",
            new RouteInfo()
        );
        return ResponseEntity.ok(response);
    }

    /**
     * API shipper gọi khi khách báo hoãn (loại thẳng khỏi danh sách đang giao nếu shipper chấp nhận - k xử lý các case thời gian).
     */
    @PostMapping("/drivers/{deliveryManId}/parcels/{parcelId}/postpone")
    public ResponseEntity<DeliveryAssignmentResponse> postponeTask(
        @PathVariable UUID deliveryManId,
        @PathVariable UUID parcelId,
        @RequestBody String addInfo
    ) {
        log.info("Shipper {} flagging parcel {} as POSTPONE and accept by both side", deliveryManId, parcelId);
        DeliveryAssignmentResponse response = assignmentService.postponeByCustomer(
            parcelId, 
            deliveryManId, 
            "Khách yêu cầu hoãn với thời gian: " + addInfo,
            new RouteInfo()
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/current-shipper/parcels/{parcelId}")
    public ResponseEntity<ShipperInfo> getCurrentShipperInfoForParcel(@PathVariable String parcelId) {
        Optional<ShipperInfo> shipperOpt = assignmentService.getLatestDriverIdForParcel(parcelId);
        if (shipperOpt.isPresent()) {
            return ResponseEntity.ok(shipperOpt.get());
        } else {
            return ResponseEntity.ok(null);
        }
    }

    /**
     * Get active assignment ID for a parcel and delivery man.
     * Returns the assignmentId of the active assignment (in CREATED or IN_PROGRESS session).
     * This is used by Communication Service to find assignmentId before calling postpone endpoint.
     */
    @GetMapping("/active")
    public ResponseEntity<BaseResponse<UUID>> getActiveAssignmentId(
        @RequestParam String parcelId,
        @RequestParam String deliveryManId
    ) {
        log.info("Getting active assignmentId for parcelId: {} and deliveryManId: {}", parcelId, deliveryManId);
        Optional<UUID> assignmentIdOpt = assignmentService.getActiveAssignmentId(parcelId, deliveryManId);
        if (assignmentIdOpt.isPresent()) {
            return ResponseEntity.ok(BaseResponse.success(assignmentIdOpt.get()));
        } else {
            return ResponseEntity.ok(BaseResponse.error("No active assignment found for parcel " + parcelId + " and delivery man " + deliveryManId));
        }
    }

    /**
     * Get the latest assignment (any session status) for a parcel.
     * Used by Parcel Service to resolve assignment/session when client confirms delivery.
     */
    @GetMapping("/parcel/{parcelId}/latest-assignment")
    public ResponseEntity<BaseResponse<LatestAssignmentResponse>> getLatestAssignmentForParcel(
        @PathVariable String parcelId
    ) {
        Optional<LatestAssignmentResponse> infoOpt = assignmentService.getLatestAssignmentForParcel(parcelId);
        return infoOpt
            .map(info -> ResponseEntity.ok(BaseResponse.success(info)))
            .orElseGet(() -> ResponseEntity.ok(BaseResponse.error("No assignments found for parcel " + parcelId)));
    }

    /**
     * Postpone assignment directly by assignmentId.
     * This endpoint is used when we already have the assignmentId (e.g., from proposal response).
     * This fixes the bug where proposal postpone responses lack assignmentId.
     */
    @PutMapping("/{assignmentId}/postpone")
    public ResponseEntity<DeliveryAssignmentResponse> postponeByAssignmentId(
        @PathVariable UUID assignmentId,
        @RequestBody PostponeRequest request
    ) {
        log.info("Postponing assignment {} with reason: {}", assignmentId, request.getReason());
        DeliveryAssignmentResponse response = assignmentService.postponeByAssignmentId(
            assignmentId,
            request.getReason(),
            request.getRouteInfo()
        );
        return ResponseEntity.ok(response);
    }

    /**
     * Request DTO for postpone by assignmentId
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    static class PostponeRequest {
        private String reason;
        private RouteInfo routeInfo;
    }
    
}
