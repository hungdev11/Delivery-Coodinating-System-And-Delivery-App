package com.ds.session.session_service.app_context.repositories;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.ds.session.session_service.app_context.models.DeliveryAssignment;

@Repository
public interface DeliveryAssignmentRepository extends JpaRepository<DeliveryAssignment, UUID>, JpaSpecificationExecutor<DeliveryAssignment>{
    boolean existsByDeliveryManIdAndParcelIdAndScanedAtBetween(String deliveryManId, String parcelId, LocalDateTime start, LocalDateTime end);
    Optional<DeliveryAssignment> findByDeliveryManIdAndParcelIdAndScanedAtBetween(String parcelId, String deliveryManId, LocalDateTime start, LocalDateTime end);
    List<DeliveryAssignment> findAllByDeliveryManIdAndScanedAtBetween(String deliveryManId, LocalDateTime start, LocalDateTime end);
    List<DeliveryAssignment> findAllByDeliveryManId(String string);
}
