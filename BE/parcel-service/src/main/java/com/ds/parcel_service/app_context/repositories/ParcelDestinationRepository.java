package com.ds.parcel_service.app_context.repositories;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ds.parcel_service.app_context.models.Parcel;
import com.ds.parcel_service.app_context.models.ParcelDestination;

public interface ParcelDestinationRepository extends JpaRepository<ParcelDestination, UUID>{
    /**
     * Find the current destination for a parcel (should be 0 or 1)
     */
    Optional<ParcelDestination> findByParcelAndIsCurrentTrue(Parcel parcel);
    
    /**
     * Find all destinations for a parcel, ordered by isCurrent (current first) then by creation
     */
    List<ParcelDestination> findByParcelOrderByIsCurrentDescIdAsc(Parcel parcel);
    
    /**
     * Find all current destinations for a parcel (for validation - should be 0 or 1)
     */
    List<ParcelDestination> findAllByParcelAndIsCurrentTrue(Parcel parcel);
}
