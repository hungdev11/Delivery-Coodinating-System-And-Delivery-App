package com.ds.parcel_service.app_context.repositories;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ds.parcel_service.app_context.models.AssignmentSnapshot;

@Repository
public interface AssignmentSnapshotRepository extends JpaRepository<AssignmentSnapshot, UUID> {

    Optional<AssignmentSnapshot> findFirstByParcelIdOrderByUpdatedAtDesc(UUID parcelId);
}

