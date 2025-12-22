package com.ds.session.session_service.app_context.repositories;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ds.session.session_service.app_context.models.DeliveryProof;
import com.ds.session.session_service.common.enums.ProofType;

public interface DeliveryProofRepository extends JpaRepository<DeliveryProof, UUID>{
    List<DeliveryProof> findByAssignmentId(UUID assignmentId);

    List<DeliveryProof> findByAssignmentIdAndType(
            UUID assignmentId,
            ProofType type
    );
}
