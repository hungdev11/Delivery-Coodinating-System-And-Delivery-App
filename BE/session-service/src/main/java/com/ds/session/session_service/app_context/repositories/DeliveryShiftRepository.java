package com.ds.session.session_service.app_context.repositories;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import com.ds.session.session_service.app_context.models.DeliveryManShift;
import com.ds.session.session_service.app_context.models.Shift;

public interface DeliveryShiftRepository extends JpaRepository<DeliveryManShift, UUID>{

    boolean existsByDeliveryManIdAndShift(String shipperId, Shift shift);

    List<DeliveryManShift> findByShift(Shift shift);

    List<DeliveryManShift> findByDeliveryManId(String deliveryManId);

}
