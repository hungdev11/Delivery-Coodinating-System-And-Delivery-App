package com.ds.session.session_service.app_context.repositories;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ds.session.session_service.app_context.models.ShipperLocationTracking;

@Repository
public interface ShipperLocationTrackingRepository extends JpaRepository<ShipperLocationTracking, UUID> {
    
    @Query("SELECT t FROM ShipperLocationTracking t WHERE t.sessionId = :sessionId ORDER BY t.timestamp DESC")
    List<ShipperLocationTracking> findBySessionIdOrderByTimestampDesc(@Param("sessionId") String sessionId);

    @Query("SELECT t FROM ShipperLocationTracking t WHERE t.sessionId = :sessionId AND t.nearestNodeId IS NOT NULL ORDER BY t.timestamp DESC")
    List<ShipperLocationTracking> findNodePassedBySessionId(@Param("sessionId") String sessionId);
}
