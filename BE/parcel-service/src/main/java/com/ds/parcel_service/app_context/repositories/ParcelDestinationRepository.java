package com.ds.parcel_service.app_context.repositories;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ds.parcel_service.app_context.models.Parcel;
import com.ds.parcel_service.app_context.models.ParcelDestination;

public interface ParcelDestinationRepository extends JpaRepository<ParcelDestination, UUID>{
    Optional<ParcelDestination> findByParcelAndIsCurrentTrue(Parcel parcel);
}
