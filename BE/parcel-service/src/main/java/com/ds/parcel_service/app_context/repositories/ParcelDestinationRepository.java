package com.ds.parcel_service.app_context.repositories;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ds.parcel_service.app_context.models.ParcelDestination;

public interface ParcelDestinationRepository extends JpaRepository<ParcelDestination, UUID>{

}
