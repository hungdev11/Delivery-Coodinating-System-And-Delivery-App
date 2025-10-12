package com.ds.parcel_service.app_context.repositories;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.ds.parcel_service.app_context.models.Parcel;
import com.ds.parcel_service.common.enums.ParcelStatus;

public interface ParcelRepository extends JpaRepository<Parcel, UUID>, JpaSpecificationExecutor<Parcel>{
    Optional<Parcel> findByCode(String code);

    boolean existsByCode(String code);

    List<Parcel> findByStatusAndDeliveredAtBefore(ParcelStatus delivered, LocalDateTime deadline);

    List<Parcel> findByStatusAndDeliveredAtBetween(ParcelStatus delivered, LocalDateTime twentyFourHoursAgo,
            LocalDateTime now);
}
