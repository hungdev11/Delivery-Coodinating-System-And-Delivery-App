package com.ds.parcel_service.app_context.repositories;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ds.parcel_service.app_context.models.Parcel;

public interface ParcelRepository extends JpaRepository<Parcel, UUID>{
    Optional<Parcel> findByCode(String code);

    boolean existsByCode(String code);
}
