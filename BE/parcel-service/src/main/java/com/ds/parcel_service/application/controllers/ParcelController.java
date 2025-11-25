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
import com.ds.parcel_service.common.entities.dto.common.BaseResponse;
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
    public ResponseEntity<BaseResponse<ParcelResponse>> createParcel(@Valid @RequestBody ParcelCreateRequest request) {
        log.debug("Creating parcel with code={}", request.getCode());
        ParcelResponse response = parcelService.createParcel(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(BaseResponse.success(response));
    }

    @PutMapping("/{parcelId}")
    public ResponseEntity<BaseResponse<ParcelResponse>> updateParcel(
            @PathVariable UUID parcelId,
            @Valid @RequestBody ParcelUpdateRequest request) {
        log.debug("Updating parcel id={}", parcelId);
        ParcelResponse response = parcelService.updateParcel(parcelId, request);
        return ResponseEntity.ok(BaseResponse.success(response));
    }

    @GetMapping("/{parcelId}")
    public ResponseEntity<BaseResponse<ParcelResponse>> getParcelById(@PathVariable UUID parcelId) {
        log.debug("Fetching parcel id={}", parcelId);
        ParcelResponse response = parcelService.getParcelById(parcelId);
        return ResponseEntity.ok(BaseResponse.success(response));
    }

    @GetMapping("/code/{code}")
    public ResponseEntity<BaseResponse<ParcelResponse>> getParcelByCode(@PathVariable String code) {
        log.debug("Fetching parcel by code={}", code);
        ParcelResponse response = parcelService.getParcelByCode(code);
        return ResponseEntity.ok(BaseResponse.success(response));
    }

    /**
     * Get parcels sent by current user
     * User ID is extracted from X-User-Id header (forwarded by API Gateway from JWT
     * token)
     */
    @GetMapping("/me")
    public ResponseEntity<PageResponse<ParcelResponse>> getMyParcels(
            @RequestHeader("X-User-Id") String customerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.debug("Get parcels sent by user: {}", customerId);
        PageResponse<ParcelResponse> parcels = parcelService.getParcelsSentByCustomer(customerId, page, size);
        return ResponseEntity.ok(parcels);
    }

    /**
     * Get parcels received by current user
     * User ID is extracted from X-User-Id header (forwarded by API Gateway from JWT
     * token)
     */
    @GetMapping("/me/receive")
    public ResponseEntity<PageResponse<ParcelResponse>> getReceiveParcels(
            @RequestHeader("X-User-Id") String customerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.debug("Get parcels received by user: {}", customerId);
        PageResponse<ParcelResponse> parcels = parcelService.getParcelsReceivedByCustomer(customerId, page, size);
        return ResponseEntity.ok(parcels);
    }

    @GetMapping()
    public ResponseEntity<PageResponse<ParcelResponse>> getParcels(
            @Valid ParcelFilterRequest filter,

            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false, defaultValue = "asc") String direction) {

        log.debug("Fetching parcels page={} size={}. Filters: {}", page, size, filter);

        PageResponse<ParcelResponse> response = parcelService.getParcels(filter, page, size, sortBy, direction);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{parcelId}")
    public ResponseEntity<Void> deleteParcel(@PathVariable UUID parcelId) {
        log.debug("Deleting parcel id={}", parcelId);
        parcelService.deleteParcel(parcelId);
        return ResponseEntity.noContent().build();
    }

    // --- STATUS CHANGE APIS ---

    /**
     * Generic API to change parcel status
     */
    @PutMapping("/change-status/{parcelId}")
    public ResponseEntity<BaseResponse<ParcelResponse>> changeParcelStatus(
            @PathVariable UUID parcelId,
            @RequestParam @EnumValue(name = "event", enumClass = ParcelEvent.class, message = "event must be a valid enum value") String event) {
        ParcelResponse response = parcelService.changeParcelStatus(parcelId, ParcelEvent.valueOf(event));
        return ResponseEntity.ok(BaseResponse.success(response));
    }

    @PutMapping("/deliver/{parcelId}")
    public ResponseEntity<BaseResponse<ParcelResponse>> deliverParcel(@PathVariable UUID parcelId) {
        log.debug("Shipper marking parcel {} as DELIVERY_SUCCESSFUL", parcelId);
        ParcelResponse response = parcelService.changeParcelStatus(parcelId, ParcelEvent.DELIVERY_SUCCESSFUL);
        return ResponseEntity.ok(BaseResponse.success(response));
    }

    @PutMapping("/confirm/{parcelId}")
    public ResponseEntity<BaseResponse<ParcelResponse>> confirmParcelArrived(@PathVariable UUID parcelId) {
        ParcelResponse response = parcelService.changeParcelStatus(parcelId, ParcelEvent.CUSTOMER_RECEIVED);
        return ResponseEntity.ok(BaseResponse.success(response));
    }

    @PutMapping("/accident/{parcelId}")
    public ResponseEntity<BaseResponse<ParcelResponse>> notifyBrokenParcel(@PathVariable UUID parcelId) {
        ParcelResponse response = parcelService.changeParcelStatus(parcelId, ParcelEvent.CAN_NOT_DELIVERY);
        return ResponseEntity.ok(BaseResponse.success(response));
    }

    @PutMapping("/refuse/{parcelId}")
    public ResponseEntity<BaseResponse<ParcelResponse>> refuseParcel(@PathVariable UUID parcelId) {
        ParcelResponse response = parcelService.changeParcelStatus(parcelId, ParcelEvent.CUSTOMER_REJECT);
        return ResponseEntity.ok(BaseResponse.success(response));
    }

    @PutMapping("/postpone/{parcelId}")
    public ResponseEntity<BaseResponse<ParcelResponse>> postponeParcel(@PathVariable UUID parcelId) {
        ParcelResponse response = parcelService.changeParcelStatus(parcelId, ParcelEvent.POSTPONE);
        return ResponseEntity.ok(BaseResponse.success(response));
    }

    // --- DISPUTE HANDLING APIS ---

    @PutMapping("/dispute/{parcelId}")
    public ResponseEntity<BaseResponse<ParcelResponse>> disputeParcel(@PathVariable UUID parcelId) {
        log.debug("Customer creating DISPUTE for parcel {}", parcelId);
        ParcelResponse response = parcelService.changeParcelStatus(parcelId, ParcelEvent.CUSTOMER_CONFIRM_NOT_RECEIVED);
        return ResponseEntity.ok(BaseResponse.success(response));
    }

    @PutMapping("/resolve-dispute/misunderstanding/{parcelId}")
    public ResponseEntity<BaseResponse<ParcelResponse>> resolveDisputeAsMisunderstanding(@PathVariable UUID parcelId) {
        log.debug("Admin resolving dispute for parcel {} as MISSUNDERSTANDING_DISPUTE", parcelId);
        ParcelResponse response = parcelService.changeParcelStatus(parcelId, ParcelEvent.MISSUNDERSTANDING_DISPUTE);
        return ResponseEntity.ok(BaseResponse.success(response));
    }

    @PutMapping("/resolve-dispute/fault/{parcelId}")
    public ResponseEntity<BaseResponse<ParcelResponse>> resolveDisputeAsFault(@PathVariable UUID parcelId) {
        log.debug("Admin resolving dispute for parcel {} as FAULT_DISPUTE", parcelId);
        ParcelResponse response = parcelService.changeParcelStatus(parcelId, ParcelEvent.FAULT_DISPUTE);
        return ResponseEntity.ok(BaseResponse.success(response));
    }

    @PostMapping("/bulk")
    ResponseEntity<BaseResponse<Map<String, ParcelResponse>>> fetchParcelsBulk(@RequestBody List<UUID> parcelIds) {
        return ResponseEntity.ok(BaseResponse.success(parcelService.fetchParcelsBulk(parcelIds)));
    }

    @PutMapping("/{parcelId}/priority")
    public ResponseEntity<BaseResponse<ParcelResponse>> updateParcelPriority(
            @PathVariable UUID parcelId,
            @RequestParam Integer priority) {
        log.debug("Updating priority for parcel {} to {}", parcelId, priority);
        ParcelResponse response = parcelService.updateParcelPriority(parcelId, priority);
        return ResponseEntity.ok(BaseResponse.success(response));
    }

    @PutMapping("/{parcelId}/delay")
    public ResponseEntity<BaseResponse<ParcelResponse>> delayParcel(
            @PathVariable UUID parcelId,
            @RequestParam(required = false) LocalDateTime delayedUntil) {
        log.debug("Delaying parcel {} until {}", parcelId, delayedUntil);
        ParcelResponse response = parcelService.delayParcel(parcelId, delayedUntil);
        return ResponseEntity.ok(BaseResponse.success(response));
    }

    @PutMapping("/{parcelId}/undelay")
    public ResponseEntity<BaseResponse<ParcelResponse>> undelayParcel(@PathVariable UUID parcelId) {
        log.debug("Undelaying parcel {}", parcelId);
        ParcelResponse response = parcelService.undelayParcel(parcelId);
        return ResponseEntity.ok(BaseResponse.success(response));
    }
}
