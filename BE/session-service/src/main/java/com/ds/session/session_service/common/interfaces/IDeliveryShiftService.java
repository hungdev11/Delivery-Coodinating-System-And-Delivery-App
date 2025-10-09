package com.ds.session.session_service.common.interfaces;

import java.util.List;
import java.util.UUID;

import com.ds.session.session_service.app_context.models.DeliveryManShift;

public interface IDeliveryShiftService {
    DeliveryManShift assignShift(String deliveryManId, long shiftId);
    List<DeliveryManShift> bulkAssignShift(List<String> shipperIds, Long shiftId);
    void cancelShift(UUID deliveryManShiftId);
    List<DeliveryManShift> getDeliveryMansByShift(Long shiftId);
    List<DeliveryManShift> getShiftsByDeliveryMan(String shipperId);
}
