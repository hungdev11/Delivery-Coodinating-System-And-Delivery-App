package com.ds.session.session_service.app_context.repositories;

import com.ds.session.session_service.app_context.models.DeliveryManSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DeliveryManSnapshotRepository extends JpaRepository<DeliveryManSnapshot, String> {
    Optional<DeliveryManSnapshot> findByUserId(String userId);
}
