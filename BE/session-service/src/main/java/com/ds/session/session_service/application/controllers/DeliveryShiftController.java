package com.ds.session.session_service.application.controllers;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ds.session.session_service.app_context.models.DeliveryManShift;
import com.ds.session.session_service.common.interfaces.IDeliveryShiftService;
import com.ds.session.session_service.common.utils.Utility;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/delivery-shifts")
@RequiredArgsConstructor
public class DeliveryShiftController {
    private final IDeliveryShiftService deliveryShiftService;

    /**
     * Gán ca cho 1 shipper.
     */
    @PostMapping("/assign")
    public ResponseEntity<DeliveryManShift> assignShift(
            @RequestParam String deliveryManId,
            @RequestParam Long shiftId) {
        return ResponseEntity.ok(deliveryShiftService.assignShift(deliveryManId, shiftId));
    }

    /**
     * Gán cùng một ca cho nhiều shipper.
     */
    @PostMapping("/assign/bulk")
    public ResponseEntity<List<DeliveryManShift>> bulkAssignShift(
            @RequestBody List<String> shipperIds,
            @RequestParam Long shiftId) {
        return ResponseEntity.ok(deliveryShiftService.bulkAssignShift(shipperIds, shiftId));
    }

    /**
     * Hủy ca.
     */
    @DeleteMapping("/{deliveryManShiftId}/cancel")
    public ResponseEntity<Void> cancelShift(@PathVariable String deliveryManShiftId) {
        deliveryShiftService.cancelShift(Utility.toUUID(deliveryManShiftId));
        return ResponseEntity.noContent().build();
    }

    /**
     * Lấy danh sách shipper thuộc 1 ca.
     */
    @GetMapping("/shift/{shiftId}")
    public ResponseEntity<List<DeliveryManShift>> getDeliveryMansByShift(@PathVariable Long shiftId) {
        return ResponseEntity.ok(deliveryShiftService.getDeliveryMansByShift(shiftId));
    }

    /**
     * Lấy danh sách ca của 1 shipper.
     */
    @GetMapping("/deliveryman/{deliveryManId}")
    public ResponseEntity<List<DeliveryManShift>> getShiftsByDeliveryMan(@PathVariable String deliveryManId) {
        return ResponseEntity.ok(deliveryShiftService.getShiftsByDeliveryMan(deliveryManId));
    }
}

