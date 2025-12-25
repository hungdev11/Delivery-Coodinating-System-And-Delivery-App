package com.ds.session.session_service.app_context.repositories;

import com.ds.session.session_service.app_context.models.DeliveryAssignmentParcel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DeliveryAssignmentParcelRepository extends JpaRepository<DeliveryAssignmentParcel, UUID> {
    
    /**
     * Find all parcels for an assignment
     */
    List<DeliveryAssignmentParcel> findByAssignmentId(UUID assignmentId);
    
    /**
     * Find all assignments containing a specific parcel
     */
    List<DeliveryAssignmentParcel> findByParcelId(String parcelId);
    
    /**
     * Check if parcel exists in any assignment
     */
    boolean existsByParcelId(String parcelId);
    
    /**
     * Delete all parcels for an assignment
     */
    void deleteByAssignmentId(UUID assignmentId);
}
