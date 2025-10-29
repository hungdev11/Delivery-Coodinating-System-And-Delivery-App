package com.ds.session.session_service.application.controllers;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ds.session.session_service.common.entities.dto.request.TaskFailRequest;
import com.ds.session.session_service.common.entities.dto.request.RouteInfo;
import com.ds.session.session_service.common.entities.dto.response.DeliveryAssignmentResponse;
import com.ds.session.session_service.common.interfaces.IDeliveryAssignmentService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Controller này quản lý các HÀNH ĐỘNG trên TỪNG TASK (ASSIGNMENT).
 * Ví dụ: Giao thành công, Giao thất bại, Lấy lịch sử.
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
     */
    @GetMapping("/drivers/{deliveryManId}/daily")
    public ResponseEntity<List<DeliveryAssignmentResponse>> getDailyTasks(
        @PathVariable UUID deliveryManId
    ) {
        log.info("Fetching daily (active session) tasks for shipper {}", deliveryManId);
        List<DeliveryAssignmentResponse> response = assignmentService.getDailyTasks(deliveryManId);
        return ResponseEntity.ok(response);
    }

    /**
     * Lấy lịch sử task (từ các phiên ĐÃ HOÀN THÀNH) của shipper.
     */
    @GetMapping("/drivers/{deliveryManId}/history")
    public ResponseEntity<List<DeliveryAssignmentResponse>> getTasksHistory(
        @PathVariable UUID deliveryManId,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end
    ) {
        log.info("Fetching tasks history for shipper {} between {} and {}", deliveryManId, start, end);
        List<DeliveryAssignmentResponse> response = assignmentService.getTasksBetween(deliveryManId, start, end);
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
        @PathVariable UUID parcelId,
        @Valid @RequestBody TaskFailRequest request
    ) {
        log.info("Shipper {} failing task for parcel {} (Reason: {})", deliveryManId, parcelId, request.getReason());
        DeliveryAssignmentResponse response = assignmentService.rejectedByCustomer(
            parcelId, 
            deliveryManId, 
            request.getReason(),
            request.getRouteInfo()
        );
        return ResponseEntity.ok(response);
    }
}
