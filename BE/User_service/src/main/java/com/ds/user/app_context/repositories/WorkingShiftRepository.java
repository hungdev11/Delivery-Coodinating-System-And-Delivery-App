package com.ds.user.app_context.repositories;

import com.ds.user.common.entities.base.WorkingShift;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WorkingShiftRepository extends JpaRepository<WorkingShift, UUID> {
    
    /**
     * Find all working shifts for a delivery man
     */
    List<WorkingShift> findByDeliveryManId(UUID deliveryManId);
    
    /**
     * Find active working shifts for a delivery man
     */
    List<WorkingShift> findByDeliveryManIdAndIsActiveTrue(UUID deliveryManId);
    
    /**
     * Find working shift by delivery man, day of week, and time range
     */
    @Query("SELECT ws FROM WorkingShift ws WHERE ws.deliveryMan.id = :deliveryManId " +
           "AND ws.dayOfWeek = :dayOfWeek " +
           "AND ws.startTime <= :time AND ws.endTime >= :time " +
           "AND ws.isActive = true")
    List<WorkingShift> findActiveShiftAtTime(
        @Param("deliveryManId") UUID deliveryManId,
        @Param("dayOfWeek") Integer dayOfWeek,
        @Param("time") LocalTime time
    );
    
    /**
     * Check if delivery man has any working shifts
     */
    boolean existsByDeliveryManId(UUID deliveryManId);
}
