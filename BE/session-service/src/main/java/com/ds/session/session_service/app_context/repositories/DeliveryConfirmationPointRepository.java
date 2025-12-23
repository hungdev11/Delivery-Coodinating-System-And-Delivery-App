package com.ds.session.session_service.app_context.repositories;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ds.session.session_service.app_context.models.DeliveryConfirmationPoint;

@Repository
public interface DeliveryConfirmationPointRepository extends JpaRepository<DeliveryConfirmationPoint, UUID> {
    
    List<DeliveryConfirmationPoint> findByAssignmentId(UUID assignmentId);
    
    List<DeliveryConfirmationPoint> findBySessionId(String sessionId);
    
    @Query("SELECT dcp FROM DeliveryConfirmationPoint dcp WHERE dcp.sessionId = :sessionId ORDER BY dcp.confirmedAt DESC")
    List<DeliveryConfirmationPoint> findBySessionIdOrderByConfirmedAtDesc(@Param("sessionId") String sessionId);
}
