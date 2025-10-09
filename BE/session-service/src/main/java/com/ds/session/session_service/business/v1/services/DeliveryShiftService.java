package com.ds.session.session_service.business.v1.services;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ds.session.session_service.app_context.models.DeliveryManShift;
import com.ds.session.session_service.app_context.models.Shift;
import com.ds.session.session_service.app_context.repositories.DeliveryShiftRepository;
import com.ds.session.session_service.app_context.repositories.ShiftRepository;
import com.ds.session.session_service.common.interfaces.IDeliveryShiftService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class DeliveryShiftService implements IDeliveryShiftService{
    private final DeliveryShiftRepository deliveryShiftRepository;
    private final ShiftRepository shiftRepository;

    /**
     * Gán ca cho một shipper.
     */
    public DeliveryManShift assignShift(String deliveryManId, long shiftId) {
        Shift shift = shiftRepository.findById(shiftId)
            .orElseThrow(() -> new IllegalArgumentException("Shift not found"));

        if (deliveryShiftRepository.existsByDeliveryManIdAndShift(deliveryManId, shift))
            throw new IllegalStateException("Shipper already assigned to this shift");

        DeliveryManShift shipperShift = DeliveryManShift.builder()
            .deliveryManId(deliveryManId)
            .shift(shift)
            .isActive(true)
            .build();

        return deliveryShiftRepository.save(shipperShift);
    }

    /**
     * Gán ca hàng loạt cho nhiều shipper.
     */
    public List<DeliveryManShift> bulkAssignShift(List<String> shipperIds, Long shiftId) {
        Shift shift = shiftRepository.findById(shiftId)
            .orElseThrow(() -> new IllegalArgumentException("Shift not found"));

        List<DeliveryManShift> assigned = new ArrayList<>();

        for (String shipperId : shipperIds) {
            if (deliveryShiftRepository.existsByDeliveryManIdAndShift(shipperId, shift))
                continue; // Bỏ qua shipper đã có ca này

            DeliveryManShift s = DeliveryManShift.builder()
                .deliveryManId(shipperId)
                .shift(shift)
                .isActive(true)
                .build();

            assigned.add(s);
        }

        return deliveryShiftRepository.saveAll(assigned);
    }

    /**
     * Hủy ca của shipper.
     */
    public void cancelShift(UUID deliveryManShiftId) {
        DeliveryManShift deliveryManShift = deliveryShiftRepository.findById(deliveryManShiftId)
            .orElseThrow(() -> new IllegalArgumentException("Shipper shift not found"));

        deliveryManShift.setActive(false);
        deliveryShiftRepository.save(deliveryManShift);
    }

    /**
     * Lấy danh sách shipper theo ca.
     */
    @Transactional(readOnly = true)
    public List<DeliveryManShift> getDeliveryMansByShift(Long shiftId) {
        Shift shift = shiftRepository.findById(shiftId)
            .orElseThrow(() -> new IllegalArgumentException("Shift not found"));
        return deliveryShiftRepository.findByShift(shift);
    }

    /**
     * Lấy lịch ca của một shipper trong ngày.
     */
    @Transactional(readOnly = true)
    public List<DeliveryManShift> getShiftsByDeliveryMan(String shipperId) {
        return deliveryShiftRepository.findByDeliveryManId(shipperId);
    }
}

