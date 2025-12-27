package com.ds.user.app_context.repositories;

import com.ds.user.common.entities.base.WorkingZone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WorkingZoneRepository extends JpaRepository<WorkingZone, UUID> {
    
    /**
     * Find all working zones for a delivery man, ordered by priority order
     */
    List<WorkingZone> findByDeliveryManIdOrderByOrderAsc(UUID deliveryManId);
    
    /**
     * Find working zone by delivery man and zone ID
     */
    Optional<WorkingZone> findByDeliveryManIdAndZoneId(UUID deliveryManId, String zoneId);
    
    /**
     * Count working zones for a delivery man
     */
    long countByDeliveryManId(UUID deliveryManId);
    
    /**
     * Check if delivery man has working zone
     */
    boolean existsByDeliveryManId(UUID deliveryManId);
    
    /**
     * Delete all working zones for a delivery man
     */
    void deleteByDeliveryManId(UUID deliveryManId);
}
