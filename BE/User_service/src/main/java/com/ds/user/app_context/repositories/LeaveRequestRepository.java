package com.ds.user.app_context.repositories;

import com.ds.user.common.entities.base.LeaveRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, UUID> {
    
    /**
     * Find all leave requests for a delivery man
     */
    List<LeaveRequest> findByDeliveryManIdOrderByStartTimeDesc(UUID deliveryManId);
    
    /**
     * Find approved leave requests that overlap with a time period
     */
    @Query("SELECT lr FROM LeaveRequest lr WHERE lr.deliveryMan.id = :deliveryManId " +
           "AND lr.status = 'APPROVED' " +
           "AND lr.startTime <= :endTime AND lr.endTime >= :startTime")
    List<LeaveRequest> findOverlappingApprovedLeaves(
        @Param("deliveryManId") UUID deliveryManId,
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime
    );
    
    /**
     * Find leave requests for a specific shift
     */
    List<LeaveRequest> findByShiftId(UUID shiftId);
    
    /**
     * Check if delivery man has any leave requests
     */
    boolean existsByDeliveryManId(UUID deliveryManId);
}
