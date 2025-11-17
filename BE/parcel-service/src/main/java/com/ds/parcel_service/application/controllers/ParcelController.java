package com.ds.parcel_service.application.controllers;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ds.parcel_service.common.annotations.EnumValue;
import com.ds.parcel_service.common.entities.dto.request.ParcelCreateRequest;
import com.ds.parcel_service.common.entities.dto.request.ParcelFilterRequest;
import com.ds.parcel_service.common.entities.dto.request.ParcelUpdateRequest;
import com.ds.parcel_service.common.entities.dto.response.PageResponse;
import com.ds.parcel_service.common.entities.dto.response.ParcelResponse;
import com.ds.parcel_service.common.enums.ParcelEvent;
import com.ds.parcel_service.common.interfaces.IParcelService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;



@RestController
@RequestMapping("/api/v1/parcels")
@RequiredArgsConstructor
@Slf4j
@Validated
public class ParcelController {

    private final IParcelService parcelService;

    @PostMapping
    public ResponseEntity<ParcelResponse> createParcel(@Valid @RequestBody ParcelCreateRequest request) {
        log.info("Creating parcel with code={}", request.getCode());
        ParcelResponse response = parcelService.createParcel(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{parcelId}")
    public ResponseEntity<ParcelResponse> updateParcel(
            @PathVariable UUID parcelId,
            @Valid @RequestBody ParcelUpdateRequest request) {
        log.info("Updating parcel id={}", parcelId);
        ParcelResponse response = parcelService.updateParcel(parcelId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{parcelId}")
    public ResponseEntity<ParcelResponse> getParcelById(@PathVariable UUID parcelId) {
        log.info("Fetching parcel id={}", parcelId);
        ParcelResponse response = parcelService.getParcelById(parcelId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/code/{code}")
    public ResponseEntity<ParcelResponse> getParcelByCode(@PathVariable String code) {
        log.info("Fetching parcel by code={}", code);
        ParcelResponse response = parcelService.getParcelByCode(code);
        return ResponseEntity.ok(response);
    }

    /**
     * Get parcels sent by current user
     * User ID is extracted from X-User-Id header (forwarded by API Gateway from JWT token)
     */
    @GetMapping("/me")
    public ResponseEntity<PageResponse<ParcelResponse>> getMyParcels(
            @RequestHeader("X-User-Id") String customerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        log.info("GET /api/v1/parcels/me - Get parcels sent by user: {}", customerId);
        PageResponse<ParcelResponse> parcels = parcelService.getParcelsSentByCustomer(customerId, page, size);
        return ResponseEntity.ok(parcels);
    }

    /**
     * Get parcels received by current user
     * User ID is extracted from X-User-Id header (forwarded by API Gateway from JWT token)
     */
    @GetMapping("/me/receive")
    public ResponseEntity<PageResponse<ParcelResponse>> getReceiveParcels(
            @RequestHeader("X-User-Id") String customerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        log.info("GET /api/v1/parcels/me/receive - Get parcels received by user: {}", customerId);
        PageResponse<ParcelResponse> parcels = parcelService.getParcelsReceivedByCustomer(customerId, page, size);
        return ResponseEntity.ok(parcels);
    }
    
    @GetMapping()
    public ResponseEntity<PageResponse<ParcelResponse>> getParcels(
            // Spring tự động ánh xạ các Query Param thành đối tượng ParcelFilterRequest
            @Valid ParcelFilterRequest filter, 
            
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false, defaultValue = "asc") String direction) {
        
        log.info("Fetching parcels page={} size={}. Filters: {}", page, size, filter);
        
        // Truyền đối tượng filter đã được ánh xạ vào Service
        PageResponse<ParcelResponse> response = parcelService.getParcels(filter, page, size, sortBy, direction);
        
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{parcelId}")
    public ResponseEntity<Void> deleteParcel(@PathVariable UUID parcelId) {
        log.warn("Deleting parcel id={} (not implemented)", parcelId);
        parcelService.deleteParcel(parcelId);
        return ResponseEntity.noContent().build();
    }

    // --- CÁC API CHUYỂN TRẠNG THÁI ---

    /**
     * API chung (generic) để thay đổi trạng thái bưu kiện.
     * @return ParcelResponse với trạng thái mới
     */
    @PutMapping("/change-status/{parcelId}")
    public ResponseEntity<ParcelResponse> changeParcelStatus(
            @PathVariable UUID parcelId,
            @RequestParam 
            @EnumValue(name = "event", enumClass = ParcelEvent.class, message = "event must be a valid enum value") 
            String event
    ) {
        ParcelResponse response = parcelService.changeParcelStatus(parcelId, ParcelEvent.valueOf(event));
        return ResponseEntity.ok(response);
    }

    /**
     * API tắt (Shipper): Báo cáo đã giao hàng thành công (chuyển sang DELIVERED).
     * Tương đương: change-status?event=DELIVERY_SUCCESSFUL
     */
    @PutMapping("/deliver/{parcelId}")
    public ResponseEntity<ParcelResponse> deliverParcel(@PathVariable UUID parcelId) {
        log.info("Shipper marking parcel {} as DELIVERY_SUCCESSFUL", parcelId);
        ParcelResponse response = parcelService.changeParcelStatus(parcelId, ParcelEvent.DELIVERY_SUCCESSFUL);
        return ResponseEntity.ok(response);
    }

    /**
     * API tắt (Customer): Khách hàng xác nhận đã nhận hàng (chuyển sang SUCCEEDED).
     * Tương đương: change-status?event=CUSTOMER_RECEIVED
     */
    @PutMapping("/confirm/{parcelId}")
    public ResponseEntity<ParcelResponse> confirmParcelArrived(@PathVariable UUID parcelId) {
        ParcelResponse response = parcelService.changeParcelStatus(parcelId, ParcelEvent.CUSTOMER_RECEIVED);
        return ResponseEntity.ok(response);
    }

    /**
     * API tắt (Shipper): Shipper báo cáo sự cố không thể giao hàng (vd: tai nạn).
     * Tương đương: change-status?event=CAN_NOT_DELIVERY
     */
    @PutMapping("/accident/{parcelId}")
    public ResponseEntity<ParcelResponse> notifyBrokenParcel(@PathVariable UUID parcelId) {
        ParcelResponse response = parcelService.changeParcelStatus(parcelId, ParcelEvent.CAN_NOT_DELIVERY);
        return ResponseEntity.ok(response);
    }

    /**
     * API tắt (Shipper/Customer): Khách hàng từ chối nhận hàng.
     * Tương đương: change-status?event=CUSTOMER_REJECT
     */
    @PutMapping("/refuse/{parcelId}")
    public ResponseEntity<ParcelResponse> refuseParcel(@PathVariable UUID parcelId) {
        ParcelResponse response = parcelService.changeParcelStatus(parcelId, ParcelEvent.CUSTOMER_REJECT);
        return ResponseEntity.ok(response);
    }

    /**
     * API tắt (Shipper): Shipper yêu cầu hoãn/trì hoãn đơn hàng (vd: khách hẹn lại).
     * Tương đương: change-status?event=POSTPONE
     */
    @PutMapping("/postpone/{parcelId}") // Sửa: Đổi tên endpoint cho rõ nghĩa
    public ResponseEntity<ParcelResponse> postponeParcel(@PathVariable UUID parcelId) {
        ParcelResponse response = parcelService.changeParcelStatus(parcelId, ParcelEvent.POSTPONE);
        return ResponseEntity.ok(response);
    }

    // --- CÁC API XỬ LÝ KHIẾU NẠI (DISPUTE) ---

    /**
     * API tắt (Customer): Khách hàng báo cáo KHÔNG nhận được hàng (mở khiếu nại).
     * Tương đương: change-status?event=CUSTOMER_CONFIRM_NOT_RECEIVED
     */
    @PutMapping("/dispute/{parcelId}")
    public ResponseEntity<ParcelResponse> disputeParcel(@PathVariable UUID parcelId) {
        log.info("Customer creating DISPUTE for parcel {} (CUSTOMER_CONFIRM_NOT_RECEIVED)", parcelId);
        ParcelResponse response = parcelService.changeParcelStatus(parcelId, ParcelEvent.CUSTOMER_CONFIRM_NOT_RECEIVED);
        return ResponseEntity.ok(response);
    }

    /**
     * API tắt (Admin/Support): Giải quyết khiếu nại (lỗi do hiểu nhầm/khách sai).
     * Tương đương: change-status?event=MISSUNDERSTANDING_DISPUTE
     */
    @PutMapping("/resolve-dispute/misunderstanding/{parcelId}")
    public ResponseEntity<ParcelResponse> resolveDisputeAsMisunderstanding(@PathVariable UUID parcelId) {
        log.info("Admin resolving dispute for parcel {} as MISSUNDERSTANDING_DISPUTE", parcelId);
        ParcelResponse response = parcelService.changeParcelStatus(parcelId, ParcelEvent.MISSUNDERSTANDING_DISPUTE);
        return ResponseEntity.ok(response);
    }

    /**
     * API tắt (Admin/Support): Giải quyết khiếu nại (lỗi do shipper/làm mất hàng).
     * Tương đương: change-status?event=FAULT_DISPUTE
     */
    @PutMapping("/resolve-dispute/fault/{parcelId}")
    public ResponseEntity<ParcelResponse> resolveDisputeAsFault(@PathVariable UUID parcelId) {
        log.info("Admin resolving dispute for parcel {} as FAULT_DISPUTE (marking as LOST)", parcelId);
        ParcelResponse response = parcelService.changeParcelStatus(parcelId, ParcelEvent.FAULT_DISPUTE);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/bulk")
    ResponseEntity<Map<String, ParcelResponse>> fetchParcelsBulk(@RequestBody List<UUID> parcelIds) {
        return ResponseEntity.ok(parcelService.fetchParcelsBulk(parcelIds));
    }

    /**
     * API to update parcel priority.
     * Priority affects routing order (higher priority = delivered first).
     * @param parcelId UUID of the parcel
     * @param priority New priority value (e.g., 1-10)
     * @return Updated parcel response
     */
    @PutMapping("/{parcelId}/priority")
    public ResponseEntity<ParcelResponse> updateParcelPriority(
            @PathVariable UUID parcelId,
            @RequestParam Integer priority) {
        log.info("Updating priority for parcel {} to {}", parcelId, priority);
        ParcelResponse response = parcelService.updateParcelPriority(parcelId, priority);
        return ResponseEntity.ok(response);
    }

    /**
     * API to delay/postpone a parcel.
     * When delayed, parcel is temporarily hidden from routing until specified time.
     * @param parcelId UUID of the parcel
     * @param delayedUntil Time when parcel should be available again (optional)
     * @return Updated parcel response
     */
    @PutMapping("/{parcelId}/delay")
    public ResponseEntity<ParcelResponse> delayParcel(
            @PathVariable UUID parcelId,
            @RequestParam(required = false) LocalDateTime delayedUntil) {
        log.info("Delaying parcel {} until {}", parcelId, delayedUntil);
        ParcelResponse response = parcelService.delayParcel(parcelId, delayedUntil);
        return ResponseEntity.ok(response);
    }

    /**
     * API to undelay/resume a parcel.
     * Makes a delayed parcel available for routing again.
     * @param parcelId UUID of the parcel
     * @return Updated parcel response
     */
    @PutMapping("/{parcelId}/undelay")
    public ResponseEntity<ParcelResponse> undelayParcel(@PathVariable UUID parcelId) {
        log.info("Undelaying parcel {}", parcelId);
        ParcelResponse response = parcelService.undelayParcel(parcelId);
        return ResponseEntity.ok(response);
    }
}
