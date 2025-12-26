package com.ds.parcel_service.app_context.repositories;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    Page<Parcel> findBySenderId(String senderId, Pageable pageable);

    Page<Parcel> findByReceiverId(String receiverId, Pageable pageable);
    
    /**
     * Bulk query: Find parcels by list of IDs
     */
    List<Parcel> findByIdIn(List<UUID> ids);
    
    /**
     * Bulk query: Find parcels by list of sender IDs
     */
    List<Parcel> findBySenderIdIn(List<String> senderIds);
    
    /**
     * Bulk query: Find parcels by list of receiver IDs
     */
    List<Parcel> findByReceiverIdIn(List<String> receiverIds);
}
